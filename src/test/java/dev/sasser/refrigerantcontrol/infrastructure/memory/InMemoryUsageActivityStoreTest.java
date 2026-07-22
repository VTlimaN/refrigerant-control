package dev.sasser.refrigerantcontrol.infrastructure.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import dev.sasser.refrigerantcontrol.domain.ActivityStatus;
import dev.sasser.refrigerantcontrol.domain.Cylinder;
import dev.sasser.refrigerantcontrol.domain.RefrigerantGas;
import dev.sasser.refrigerantcontrol.domain.SealNumber;
import dev.sasser.refrigerantcontrol.domain.UsageActivity;
import dev.sasser.refrigerantcontrol.domain.UsageActivityStarter;
import dev.sasser.refrigerantcontrol.domain.Weight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryUsageActivityStoreTest {

	private static final SealNumber FIRST_SEAL = SealNumber.of("LACRE-001");
	private static final SealNumber SECOND_SEAL = SealNumber.of("LACRE-002");
	private static final Instant STARTED_AT = Instant.parse("2026-07-15T12:00:00Z");
	private static final Instant COMPLETED_AT = Instant.parse("2026-07-15T13:00:00Z");
	private static final Weight DEPARTURE_WEIGHT = Weight.of(new BigDecimal("15.14"));
	private static final String ACTIVITY_LOCATION = "  Technical room — Área A  ";
	private static final UsageActivityStarter STARTER = new UsageActivityStarter();

	@Test
	void shouldReturnEmptyUnmodifiablePendingActivityCollection() {
		Collection<UsageActivity> activities = new InMemoryUsageActivityStore()
				.findPendingUsageActivities();

		assertTrue(activities.isEmpty());
		assertThrows(UnsupportedOperationException.class, activities::clear);
	}

	@Test
	void shouldReturnAllPendingActivitiesWithExactValuesWithoutDependingOnOrder() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
		Weight firstWeight = Weight.of(new BigDecimal("15.140"));
		Weight secondWeight = Weight.of(new BigDecimal("14.00"));
		String firstLocation = "  Oficina técnica — Área 7!  ";
		String secondLocation = "Casa de máquinas B";
		Instant secondStartedAt = STARTED_AT.plusSeconds(60);
		start(store, FIRST_SEAL, firstWeight, firstLocation, STARTED_AT);
		start(store, SECOND_SEAL, secondWeight, secondLocation, secondStartedAt);

		Collection<UsageActivity> activities = store.findPendingUsageActivities();

		assertEquals(2, activities.size());
		assertEquals(Set.of(FIRST_SEAL, SECOND_SEAL), sealNumbers(activities));
		assertPendingActivity(
				activityForSeal(activities, FIRST_SEAL),
				FIRST_SEAL,
				firstWeight,
				firstLocation,
				STARTED_AT);
		assertPendingActivity(
				activityForSeal(activities, SECOND_SEAL),
				SECOND_SEAL,
				secondWeight,
				secondLocation,
				secondStartedAt);
	}

	@Test
	void shouldExcludeCompletedActivitiesAndReturnFreshDetachedPendingActivities() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
		start(store, FIRST_SEAL, STARTED_AT);
		start(store, SECOND_SEAL, STARTED_AT.plusSeconds(60));
		store.completePendingAtomically(
				FIRST_SEAL,
				activity -> activity.complete(Weight.of(new BigDecimal("12.10")), COMPLETED_AT, false));

		Collection<UsageActivity> firstQuery = store.findPendingUsageActivities();
		Collection<UsageActivity> secondQuery = store.findPendingUsageActivities();
		UsageActivity detachedActivity = activityForSeal(firstQuery, SECOND_SEAL);
		UsageActivity separatelyReconstructedActivity = activityForSeal(secondQuery, SECOND_SEAL);

		assertEquals(Set.of(SECOND_SEAL), sealNumbers(firstQuery));
		assertEquals(Set.of(SECOND_SEAL), sealNumbers(secondQuery));
		assertNotSame(detachedActivity, separatelyReconstructedActivity);
		detachedActivity.complete(
				Weight.of(new BigDecimal("12.10")),
				COMPLETED_AT.plusSeconds(60),
				false);

		Collection<UsageActivity> thirdQuery = store.findPendingUsageActivities();
		UsageActivity storedActivity = activityForSeal(thirdQuery, SECOND_SEAL);
		assertNotSame(detachedActivity, storedActivity);
		assertNotSame(separatelyReconstructedActivity, storedActivity);
		assertPendingActivity(
				storedActivity,
				SECOND_SEAL,
				DEPARTURE_WEIGHT,
				ACTIVITY_LOCATION,
				STARTED_AT.plusSeconds(60));
	}

	@Test
	void shouldObserveCoherentSnapshotDuringConcurrentCompletion() {
		assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
			InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
			start(store, FIRST_SEAL, STARTED_AT);
			ExecutorService executor = Executors.newFixedThreadPool(2);
			CountDownLatch completionEntered = new CountDownLatch(1);
			CountDownLatch releaseCompletion = new CountDownLatch(1);
			CountDownLatch queryStarted = new CountDownLatch(1);
			AtomicReference<Thread> completionThread = new AtomicReference<>();
			AtomicReference<Thread> queryThread = new AtomicReference<>();
			try {
				Future<Optional<UsageActivity>> completion = executor.submit(() -> store.completePendingAtomically(
						FIRST_SEAL,
						activity -> {
							activity.complete(
									Weight.of(new BigDecimal("12.10")),
									COMPLETED_AT,
									false);
							completionThread.set(Thread.currentThread());
							completionEntered.countDown();
							try {
								if (!releaseCompletion.await(5, TimeUnit.SECONDS)) {
									throw new IllegalStateException("completion was not released");
								}
							}
							catch (InterruptedException exception) {
								Thread.currentThread().interrupt();
								throw new IllegalStateException("completion was interrupted", exception);
							}
						}));
				awaitWorkerSignal(completionEntered, completion, "completion callback was not entered");

				Future<Collection<UsageActivity>> query = executor.submit(() -> {
					queryThread.set(Thread.currentThread());
					queryStarted.countDown();
					return store.findPendingUsageActivities();
				});
				awaitWorkerSignal(queryStarted, query, "query worker did not start");
				ThreadInfo blockedQuery = awaitBlockedQuery(
						query,
						queryThread.get(),
						completionThread.get());
				assertEquals(Thread.State.BLOCKED, blockedQuery.getThreadState());
				assertEquals(completionThread.get().threadId(), blockedQuery.getLockOwnerId());

				releaseCompletion.countDown();
				assertTrue(completion.get(5, TimeUnit.SECONDS).isPresent());
				assertTrue(query.get(5, TimeUnit.SECONDS).isEmpty());
			}
			finally {
				releaseCompletion.countDown();
				executor.shutdownNow();
				assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
			}
		});
	}

	@Test
	void shouldInvokeStartOnceAndReturnFreshDetachedActivity() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
		AtomicInteger calls = new AtomicInteger();
		AtomicReference<UsageActivity> callbackActivity = new AtomicReference<>();

		UsageActivity returned = store.startAtomically(FIRST_SEAL, activities -> {
			calls.incrementAndGet();
			UsageActivity activity = STARTER.start(
					readyCylinder(FIRST_SEAL.value()),
					DEPARTURE_WEIGHT,
					ACTIVITY_LOCATION,
					STARTED_AT,
					activities);
			callbackActivity.set(activity);
			return activity;
		});

		assertEquals(1, calls.get());
		assertNotSame(callbackActivity.get(), returned);
		assertEquals(ACTIVITY_LOCATION, returned.activityLocation());
		returned.complete(Weight.of(new BigDecimal("12.10")), COMPLETED_AT, false);
		AtomicReference<ActivityStatus> storedStatus = new AtomicReference<>();
		store.completePendingAtomically(FIRST_SEAL, activity -> {
			storedStatus.set(activity.status());
			activity.complete(Weight.of(new BigDecimal("12.10")), COMPLETED_AT, false);
		});
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, storedStatus.get());
	}

	@Test
	void shouldSupplyUnmodifiableCompleteHistoryAndPreserveCompletedActivities() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
		start(store, FIRST_SEAL, STARTED_AT);
		store.completePendingAtomically(
				FIRST_SEAL,
				activity -> activity.complete(Weight.of(new BigDecimal("12.10")), COMPLETED_AT, false));

		UsageActivity second = store.startAtomically(FIRST_SEAL, activities -> {
			assertEquals(1, activities.size());
			assertTrue(activities.stream().allMatch(Objects::nonNull));
			UsageActivity completed = activities.iterator().next();
			assertEquals(ActivityStatus.COMPLETED, completed.status());
			assertEquals(FIRST_SEAL, completed.cylinder().sealNumber());
			assertEquals(ACTIVITY_LOCATION, completed.activityLocation());
			assertThrows(UnsupportedOperationException.class, () -> activities.add(completed));
			return STARTER.start(
					readyCylinder(FIRST_SEAL.value()),
					Weight.of(new BigDecimal("12.10")),
					"Second technical room",
					COMPLETED_AT.plusSeconds(60),
					activities);
		});

		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, second.status());
		RuntimeException inspectionComplete = new RuntimeException("history inspected");
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> store.startAtomically(
				FIRST_SEAL,
				activities -> {
					assertEquals(2, activities.size());
					assertEquals(1, activities.stream()
							.filter(activity -> activity.status() == ActivityStatus.COMPLETED)
							.count());
					assertEquals(1, activities.stream()
							.filter(UsageActivity::isAwaitingReturnWeight)
							.count());
					throw inspectionComplete;
				}));
		assertSame(inspectionComplete, thrown);
	}

	@Test
	void shouldSupplyDetachedHistoryElementsWithoutPersistingTheirMutation() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
		UsageActivity returned = start(store, FIRST_SEAL, STARTED_AT);
		RuntimeException failure = new RuntimeException("detached history mutated");

		RuntimeException thrown = assertThrows(RuntimeException.class, () -> store.startAtomically(
				FIRST_SEAL,
				activities -> {
					UsageActivity detachedHistory = activities.iterator().next();
					assertNotSame(returned, detachedHistory);
					detachedHistory.complete(Weight.of(new BigDecimal("12.10")), COMPLETED_AT, false);
					throw failure;
				}));

		assertSame(failure, thrown);
		assertTrue(store.completePendingAtomically(
				FIRST_SEAL,
				activity -> activity.complete(Weight.of(new BigDecimal("12.10")), COMPLETED_AT, false))
				.isPresent());
	}

	@Test
	void shouldAllowLaterStartAfterAllPreservedHistoryIsCompleted() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
		start(store, FIRST_SEAL, STARTED_AT);
		store.completePendingAtomically(
				FIRST_SEAL,
				activity -> activity.complete(Weight.of(new BigDecimal("12.10")), COMPLETED_AT, false));
		start(store, FIRST_SEAL, COMPLETED_AT.plusSeconds(60));
		store.completePendingAtomically(
				FIRST_SEAL,
				activity -> activity.complete(
						Weight.of(new BigDecimal("11.00")),
						COMPLETED_AT.plusSeconds(120),
						false));

		UsageActivity third = store.startAtomically(FIRST_SEAL, activities -> {
			assertEquals(2, activities.size());
			assertTrue(activities.stream().allMatch(activity -> activity.status() == ActivityStatus.COMPLETED));
			return STARTER.start(
					readyCylinder(FIRST_SEAL.value()),
					Weight.of(new BigDecimal("11.00")),
					"Third technical room",
					COMPLETED_AT.plusSeconds(180),
					activities);
		});

		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, third.status());
	}

	@Test
	void shouldPropagateStartFailureUnchangedAndStoreNothing() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
		AtomicInteger calls = new AtomicInteger();
		RuntimeException failure = new RuntimeException("start failure");

		RuntimeException thrown = assertThrows(RuntimeException.class, () -> store.startAtomically(
				FIRST_SEAL,
				activities -> {
					calls.incrementAndGet();
					throw failure;
				}));

		assertSame(failure, thrown);
		assertEquals(1, calls.get());
		assertTrue(store.findPendingUsageActivities().isEmpty());
	}

	@Test
	void shouldRejectNullStartResultAndWrongSealWithoutSaving() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();

		assertThrows(NullPointerException.class, () -> store.startAtomically(FIRST_SEAL, activities -> null));
		assertThrows(
				IllegalArgumentException.class,
				() -> store.startAtomically(
						FIRST_SEAL,
						activities -> STARTER.start(
								readyCylinder(SECOND_SEAL.value()),
								DEPARTURE_WEIGHT,
								ACTIVITY_LOCATION,
								STARTED_AT,
								activities)));

		assertTrue(store.findPendingUsageActivities().isEmpty());
	}

	@Test
	void shouldPropagateCompletionFailureUnchangedAndAllowRetry() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
		start(store, FIRST_SEAL, STARTED_AT);
		AtomicInteger calls = new AtomicInteger();
		RuntimeException failure = new RuntimeException("completion failure");

		RuntimeException thrown = assertThrows(RuntimeException.class, () -> store.completePendingAtomically(
				FIRST_SEAL,
				activity -> {
					calls.incrementAndGet();
					activity.complete(Weight.of(new BigDecimal("12.10")), COMPLETED_AT, false);
					throw failure;
				}));

		assertSame(failure, thrown);
		assertEquals(1, calls.get());
		UsageActivity completed = store.completePendingAtomically(
				FIRST_SEAL,
				activity -> {
					assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, activity.status());
					assertEquals(FIRST_SEAL, activity.cylinder().sealNumber());
					assertEquals(DEPARTURE_WEIGHT.inKilograms(), activity.departureGrossWeight().inKilograms());
					assertEquals(ACTIVITY_LOCATION, activity.activityLocation());
					assertEquals(STARTED_AT, activity.startedAt());
					assertTrue(activity.returnGrossWeight().isEmpty());
					assertTrue(activity.completedAt().isEmpty());
					assertTrue(activity.consumedQuantity().isEmpty());
					assertFalse(activity.zeroConsumptionConfirmed());
					activity.complete(Weight.of(new BigDecimal("12.10")), COMPLETED_AT, false);
				})
				.orElseThrow();
		assertEquals(ActivityStatus.COMPLETED, completed.status());
		assertFalse(completed.zeroConsumptionConfirmed());
	}

	@Test
	void shouldPreservePendingSnapshotAfterUnconfirmedZeroAndReconstructConfirmedZero() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
		start(store, FIRST_SEAL, STARTED_AT);
		Weight numericallyEqualReturn = Weight.of(new BigDecimal("15.140"));

		assertThrows(
				IllegalStateException.class,
				() -> store.completePendingAtomically(
						FIRST_SEAL,
						activity -> activity.complete(numericallyEqualReturn, COMPLETED_AT, false)));

		UsageActivity completed = store.completePendingAtomically(
				FIRST_SEAL,
				activity -> {
					assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, activity.status());
					assertEquals(DEPARTURE_WEIGHT.inKilograms(), activity.departureGrossWeight().inKilograms());
					assertTrue(activity.returnGrossWeight().isEmpty());
					assertTrue(activity.completedAt().isEmpty());
					assertTrue(activity.consumedQuantity().isEmpty());
					assertFalse(activity.zeroConsumptionConfirmed());
					activity.complete(numericallyEqualReturn, COMPLETED_AT, true);
				})
				.orElseThrow();

		assertEquals(ActivityStatus.COMPLETED, completed.status());
		assertEquals(FIRST_SEAL, completed.cylinder().sealNumber());
		assertEquals("R410A", completed.cylinder().refrigerantGas().operationalName());
		assertEquals(ACTIVITY_LOCATION, completed.activityLocation());
		assertEquals(new BigDecimal("15.14"), completed.departureGrossWeight().inKilograms());
		assertEquals(2, completed.departureGrossWeight().inKilograms().scale());
		assertEquals(numericallyEqualReturn, completed.returnGrossWeight().orElseThrow());
		assertEquals(new BigDecimal("15.140"), completed.returnGrossWeight().orElseThrow().inKilograms());
		assertEquals(3, completed.returnGrossWeight().orElseThrow().inKilograms().scale());
		assertEquals(COMPLETED_AT, completed.completedAt().orElseThrow());
		assertEquals(new BigDecimal("0.000"), completed.consumedQuantity().orElseThrow().inKilograms());
		assertEquals(3, completed.consumedQuantity().orElseThrow().inKilograms().scale());
		assertTrue(completed.zeroConsumptionConfirmed());
		assertTrue(store.completePendingAtomically(FIRST_SEAL, activity -> {
		}).isEmpty());
	}

	@Test
	void shouldReturnFreshDetachedCompletedActivity() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
		start(store, FIRST_SEAL, STARTED_AT);
		AtomicReference<UsageActivity> callbackActivity = new AtomicReference<>();

		UsageActivity returned = store.completePendingAtomically(FIRST_SEAL, activity -> {
			callbackActivity.set(activity);
			activity.complete(Weight.of(new BigDecimal("12.10")), COMPLETED_AT, false);
		}).orElseThrow();

		assertNotSame(callbackActivity.get(), returned);
		assertEquals(ActivityStatus.COMPLETED, returned.status());
		assertEquals(ACTIVITY_LOCATION, returned.activityLocation());
		assertFalse(returned.zeroConsumptionConfirmed());
		assertTrue(store.completePendingAtomically(FIRST_SEAL, activity -> {
		}).isEmpty());
		RuntimeException inspected = new RuntimeException("completed history inspected");
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> store.startAtomically(
				FIRST_SEAL,
				activities -> {
					UsageActivity storedHistory = activities.iterator().next();
					assertNotSame(returned, storedHistory);
					assertEquals(ActivityStatus.COMPLETED, storedHistory.status());
					assertFalse(storedHistory.zeroConsumptionConfirmed());
					throw inspected;
				}));
		assertSame(inspected, thrown);
	}

	@Test
	void shouldProvideOnlyActivitiesForRequestedSeal() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
		start(store, FIRST_SEAL, STARTED_AT);

		UsageActivity second = store.startAtomically(SECOND_SEAL, activities -> {
			assertTrue(activities.isEmpty());
			return STARTER.start(
					readyCylinder(SECOND_SEAL.value()),
					DEPARTURE_WEIGHT,
					ACTIVITY_LOCATION,
					STARTED_AT,
					activities);
		});

		assertEquals(SECOND_SEAL, second.cylinder().sealNumber());
	}

	@Test
	void shouldRejectNullCallbacksAndSkipCompletionWhenPendingIsMissing() {
		InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
		AtomicInteger calls = new AtomicInteger();

		assertThrows(NullPointerException.class, () -> store.startAtomically(null, activities -> null));
		assertThrows(NullPointerException.class, () -> store.startAtomically(FIRST_SEAL, null));
		assertThrows(NullPointerException.class, () -> store.completePendingAtomically(null, activity -> {
		}));
		assertThrows(NullPointerException.class, () -> store.completePendingAtomically(FIRST_SEAL, null));
		Optional<UsageActivity> result = store.completePendingAtomically(
				FIRST_SEAL,
				activity -> calls.incrementAndGet());
		assertTrue(result.isEmpty());
		assertEquals(0, calls.get());
	}

	@Test
	void shouldAllowOnlyOneConcurrentStartForSameSeal() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			InMemoryUsageActivityStore store = new InMemoryUsageActivityStore();
			int callers = 8;
			ExecutorService executor = Executors.newFixedThreadPool(callers);
			CountDownLatch ready = new CountDownLatch(callers);
			CountDownLatch start = new CountDownLatch(1);
			try {
				List<Future<Boolean>> futures = java.util.stream.IntStream.range(0, callers)
						.mapToObj(index -> executor.submit(() -> {
							ready.countDown();
							if (!start.await(5, TimeUnit.SECONDS)) {
								throw new IllegalStateException("concurrent start was not released");
							}
							try {
								store.startAtomically(
										FIRST_SEAL,
										activities -> STARTER.start(
												readyCylinder(FIRST_SEAL.value()),
												DEPARTURE_WEIGHT,
												ACTIVITY_LOCATION,
												STARTED_AT.plusSeconds(index),
												activities));
								return true;
							} catch (IllegalStateException exception) {
								return false;
							}
						}))
						.toList();
				assertTrue(ready.await(5, TimeUnit.SECONDS));
				start.countDown();
				int successes = 0;
				for (Future<Boolean> future : futures) {
					if (future.get(5, TimeUnit.SECONDS)) {
						successes++;
					}
				}
				assertEquals(1, successes);
				assertEquals(1, store.findPendingUsageActivities().size());
			} finally {
				start.countDown();
				executor.shutdownNow();
				assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
			}
		});
	}

	@Test
	void shouldIsolateAdapterInstances() {
		InMemoryUsageActivityStore firstStore = new InMemoryUsageActivityStore();
		InMemoryUsageActivityStore secondStore = new InMemoryUsageActivityStore();
		start(firstStore, FIRST_SEAL, STARTED_AT);

		assertEquals(Set.of(FIRST_SEAL), sealNumbers(firstStore.findPendingUsageActivities()));
		assertTrue(secondStore.findPendingUsageActivities().isEmpty());
		assertTrue(secondStore.completePendingAtomically(FIRST_SEAL, activity -> {
		}).isEmpty());
		assertTrue(firstStore.completePendingAtomically(
				FIRST_SEAL,
				activity -> activity.complete(Weight.of(new BigDecimal("12.10")), COMPLETED_AT, false))
				.isPresent());
		assertTrue(firstStore.findPendingUsageActivities().isEmpty());
		assertTrue(secondStore.findPendingUsageActivities().isEmpty());
	}

	private static ThreadInfo awaitBlockedQuery(
			Future<?> query,
			Thread queryThread,
			Thread completionThread) throws Exception {
		Thread requiredQueryThread = Objects.requireNonNull(queryThread, "query thread must be captured");
		Thread requiredCompletionThread = Objects.requireNonNull(
				completionThread,
				"completion thread must be captured");
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		ThreadInfo queryInfo = null;
		do {
			if (query.isDone()) {
				query.get();
				throw new AssertionError("query completed before blocking on the adapter monitor");
			}
			queryInfo = threadMXBean.getThreadInfo(requiredQueryThread.threadId());
			if (queryInfo != null
					&& queryInfo.getThreadState() == Thread.State.BLOCKED
					&& queryInfo.getLockOwnerId() == requiredCompletionThread.threadId()) {
				return queryInfo;
			}
			Thread.onSpinWait();
		}
		while (System.nanoTime() < deadline);

		if (query.isDone()) {
			query.get();
			throw new AssertionError("query completed without blocking on the adapter monitor");
		}
		String observedState = queryInfo == null ? "unavailable" : queryInfo.getThreadState().name();
		long observedLockOwnerId = queryInfo == null ? -1 : queryInfo.getLockOwnerId();
		throw new AssertionError(
				"query did not block on the completion worker; state="
						+ observedState
						+ ", lockOwnerId="
						+ observedLockOwnerId);
	}

	private static void awaitWorkerSignal(
			CountDownLatch signal,
			Future<?> worker,
			String timeoutMessage) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		do {
			if (signal.getCount() == 0) {
				return;
			}
			if (worker.isDone()) {
				worker.get();
				throw new AssertionError(timeoutMessage + "; worker completed before signaling");
			}
			signal.await(10, TimeUnit.MILLISECONDS);
		}
		while (System.nanoTime() < deadline);

		if (worker.isDone()) {
			worker.get();
		}
		throw new AssertionError(timeoutMessage);
	}

	private static UsageActivity start(
			InMemoryUsageActivityStore store,
			SealNumber sealNumber,
			Instant startedAt) {
		return start(store, sealNumber, DEPARTURE_WEIGHT, ACTIVITY_LOCATION, startedAt);
	}

	private static UsageActivity start(
			InMemoryUsageActivityStore store,
			SealNumber sealNumber,
			Weight departureGrossWeight,
			String activityLocation,
			Instant startedAt) {
		return store.startAtomically(
				sealNumber,
				activities -> STARTER.start(
						readyCylinder(sealNumber.value()),
						departureGrossWeight,
						activityLocation,
						startedAt,
						activities));
	}

	private static Set<SealNumber> sealNumbers(Collection<UsageActivity> activities) {
		Set<SealNumber> sealNumbers = new HashSet<>();
		for (UsageActivity activity : activities) {
			sealNumbers.add(activity.cylinder().sealNumber());
		}
		return sealNumbers;
	}

	private static UsageActivity activityForSeal(
			Collection<UsageActivity> activities,
			SealNumber sealNumber) {
		List<UsageActivity> matchingActivities = activities.stream()
				.filter(activity -> activity.cylinder().sealNumber().equals(sealNumber))
				.toList();
		assertEquals(1, matchingActivities.size());
		return matchingActivities.getFirst();
	}

	private static void assertPendingActivity(
			UsageActivity activity,
			SealNumber sealNumber,
			Weight departureGrossWeight,
			String activityLocation,
			Instant startedAt) {
		assertEquals(sealNumber, activity.cylinder().sealNumber());
		assertEquals(departureGrossWeight.inKilograms(), activity.departureGrossWeight().inKilograms());
		assertEquals(
				departureGrossWeight.inKilograms().scale(),
				activity.departureGrossWeight().inKilograms().scale());
		assertEquals(activityLocation, activity.activityLocation());
		assertEquals(startedAt, activity.startedAt());
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, activity.status());
		assertTrue(activity.returnGrossWeight().isEmpty());
		assertTrue(activity.completedAt().isEmpty());
		assertTrue(activity.consumedQuantity().isEmpty());
		assertFalse(activity.zeroConsumptionConfirmed());
	}

	private static Cylinder readyCylinder(String sealNumber) {
		Cylinder cylinder = Cylinder.register(SealNumber.of(sealNumber), RefrigerantGas.of("R410A"));
		cylinder.registerInitialGrossWeight(Weight.of(new BigDecimal("16.00")));
		return cylinder;
	}
}
