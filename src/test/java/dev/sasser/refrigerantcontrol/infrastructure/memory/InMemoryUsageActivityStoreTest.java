package dev.sasser.refrigerantcontrol.infrastructure.memory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
		AtomicReference<Integer> relevantCount = new AtomicReference<>();
		RuntimeException inspected = new RuntimeException("inspected");
		assertThrows(RuntimeException.class, () -> store.startAtomically(FIRST_SEAL, activities -> {
			relevantCount.set(activities.size());
			throw inspected;
		}));
		assertEquals(0, relevantCount.get());
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

		AtomicReference<Integer> relevantCount = new AtomicReference<>();
		RuntimeException inspected = new RuntimeException("inspected");
		assertThrows(RuntimeException.class, () -> store.startAtomically(FIRST_SEAL, activities -> {
			relevantCount.set(activities.size());
			throw inspected;
		}));
		assertEquals(0, relevantCount.get());
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
				AtomicReference<Integer> historySize = new AtomicReference<>();
				RuntimeException inspected = new RuntimeException("inspected");
				assertThrows(RuntimeException.class, () -> store.startAtomically(FIRST_SEAL, activities -> {
					historySize.set(activities.size());
					throw inspected;
				}));
				assertEquals(1, historySize.get());
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

		assertTrue(secondStore.completePendingAtomically(FIRST_SEAL, activity -> {
		}).isEmpty());
		assertTrue(firstStore.completePendingAtomically(
				FIRST_SEAL,
				activity -> activity.complete(Weight.of(new BigDecimal("12.10")), COMPLETED_AT, false))
				.isPresent());
	}

	private static UsageActivity start(
			InMemoryUsageActivityStore store,
			SealNumber sealNumber,
			Instant startedAt) {
		return store.startAtomically(
				sealNumber,
				activities -> STARTER.start(
						readyCylinder(sealNumber.value()),
						DEPARTURE_WEIGHT,
						ACTIVITY_LOCATION,
						startedAt,
						activities));
	}

	private static Cylinder readyCylinder(String sealNumber) {
		Cylinder cylinder = Cylinder.register(SealNumber.of(sealNumber), RefrigerantGas.of("R410A"));
		cylinder.registerInitialGrossWeight(Weight.of(new BigDecimal("16.00")));
		return cylinder;
	}
}
