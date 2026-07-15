package dev.sasser.refrigerantcontrol.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.sasser.refrigerantcontrol.application.port.UsageActivityStore;
import dev.sasser.refrigerantcontrol.domain.ActivityStatus;
import dev.sasser.refrigerantcontrol.domain.SealNumber;
import dev.sasser.refrigerantcontrol.domain.UsageActivity;
import dev.sasser.refrigerantcontrol.infrastructure.memory.InMemoryCylinderStore;
import dev.sasser.refrigerantcontrol.infrastructure.memory.InMemoryUsageActivityStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsageActivityUseCasesTest {

	private static final String FIRST_SEAL = "LACRE-001";
	private static final String SECOND_SEAL = "LACRE-002";
	private static final Instant STARTED_AT = Instant.parse("2026-07-15T12:00:00Z");
	private static final Instant COMPLETED_AT = Instant.parse("2026-07-15T13:00:00Z");
	private static final String ACTIVITY_LOCATION = "Technical room — Área A";

	private final InMemoryCylinderStore cylinderStore = new InMemoryCylinderStore();
	private final InMemoryUsageActivityStore activityStore = new InMemoryUsageActivityStore();
	private final CylinderUseCases cylinderUseCases = new CylinderUseCases(cylinderStore);

	@Test
	void shouldStartActivityWithFixedClockAndPreserveDepartureScale() {
		registerReadyCylinder(FIRST_SEAL);
		UsageActivityUseCases useCases = usageUseCasesAt(STARTED_AT);

		String location = "  Technical room — Área A  ";
		UsageActivityResult result = useCases.startUsageActivity(
				FIRST_SEAL,
				new BigDecimal("15.140"),
				location);

		assertEquals(FIRST_SEAL, result.sealNumber());
		assertEquals(new BigDecimal("15.140"), result.departureGrossWeight());
		assertEquals(3, result.departureGrossWeight().scale());
		assertEquals(location, result.activityLocation());
		assertEquals(STARTED_AT, result.startedAt());
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, result.status());
		assertTrue(result.returnGrossWeight().isEmpty());
		assertTrue(result.completedAt().isEmpty());
		assertTrue(result.consumedQuantity().isEmpty());
	}

	@Test
	void shouldFailStartWhenCylinderDoesNotExist() {
		CylinderNotFoundException exception = assertThrows(
				CylinderNotFoundException.class,
				() -> usageUseCasesAt(STARTED_AT).startUsageActivity(
						FIRST_SEAL,
						new BigDecimal("15.14"),
						ACTIVITY_LOCATION));

		assertEquals("Cylinder was not found for seal number: " + FIRST_SEAL, exception.getMessage());
	}

	@Test
	void shouldBlockStartWithoutInitialWeightAndStoreNothing() {
		cylinderUseCases.registerCylinder(FIRST_SEAL, "R410A");

		assertThrows(
				InitialGrossWeightNotRegisteredException.class,
				() -> usageUseCasesAt(STARTED_AT).startUsageActivity(
						FIRST_SEAL,
						new BigDecimal("15.14"),
						ACTIVITY_LOCATION));
		assertTrue(activityStore.completePendingAtomically(
				SealNumber.of(FIRST_SEAL),
				activity -> {
					throw new AssertionError("no activity should have been stored");
				})
				.isEmpty());
	}

	@Test
	void shouldBlockPendingActivityForSameSealAcrossDetachedCylinderInstances() {
		registerReadyCylinder(FIRST_SEAL);
		TrackingUsageActivityStore trackingStore = new TrackingUsageActivityStore(activityStore);
		assertNotSame(
				cylinderStore.findBySealNumber(SealNumber.of(FIRST_SEAL)).orElseThrow(),
				cylinderStore.findBySealNumber(SealNumber.of(FIRST_SEAL)).orElseThrow());
		usageUseCasesAt(STARTED_AT, trackingStore).startUsageActivity(
				FIRST_SEAL,
				new BigDecimal("15.14"),
				ACTIVITY_LOCATION);

		assertThrows(
				PendingUsageActivityAlreadyExistsException.class,
				() -> usageUseCasesAt(STARTED_AT.plusSeconds(60), trackingStore)
						.startUsageActivity(FIRST_SEAL, new BigDecimal("14.00"), "Second location"));
		assertEquals(2, trackingStore.startCallbackCalls());
		usageUseCasesAt(STARTED_AT.plusSeconds(120), trackingStore)
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10"));
		assertThrows(
				PendingUsageActivityNotFoundException.class,
				() -> usageUseCasesAt(STARTED_AT.plusSeconds(180))
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("11.00")));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   " })
	void shouldRejectBlankLocationWithoutStoredStateAndAllowValidRetry(String invalidLocation) {
		registerReadyCylinder(FIRST_SEAL);

		assertThrows(
				IllegalArgumentException.class,
				() -> usageUseCasesAt(STARTED_AT).startUsageActivity(
						FIRST_SEAL,
						new BigDecimal("15.14"),
						invalidLocation));
		assertEquals(0, storedActivityCount(FIRST_SEAL));

		UsageActivityResult result = usageUseCasesAt(STARTED_AT.plusSeconds(60)).startUsageActivity(
				FIRST_SEAL,
				new BigDecimal("15.14"),
				ACTIVITY_LOCATION);

		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, result.status());
		assertEquals(ACTIVITY_LOCATION, result.activityLocation());
		assertEquals(1, storedActivityCount(FIRST_SEAL));
	}

	@Test
	void shouldAllowExactlyOneConcurrentStartAndReturnTypedPendingFailure() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			registerReadyCylinder(FIRST_SEAL);
			UsageActivityUseCases useCases = usageUseCasesAt(STARTED_AT);
			ExecutorService executor = Executors.newFixedThreadPool(2);
			CountDownLatch ready = new CountDownLatch(2);
			CountDownLatch start = new CountDownLatch(1);
			try {
				Future<Boolean> first = concurrentStart(executor, ready, start, useCases, "First location");
				Future<Boolean> second = concurrentStart(executor, ready, start, useCases, "Second location");
				assertTrue(ready.await(5, TimeUnit.SECONDS));
				start.countDown();

				int successes = 0;
				int pendingFailures = 0;
				for (Future<Boolean> future : java.util.List.of(first, second)) {
					if (future.get(5, TimeUnit.SECONDS)) {
						successes++;
					} else {
						pendingFailures++;
					}
				}

				assertEquals(1, successes);
				assertEquals(1, pendingFailures);
				assertEquals(1, storedActivityCount(FIRST_SEAL));
			} finally {
				start.countDown();
				executor.shutdownNow();
				assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
			}
		});
	}

	@Test
	void shouldAllowPendingActivitiesForDifferentSeals() {
		registerReadyCylinder(FIRST_SEAL);
		registerReadyCylinder(SECOND_SEAL);

		UsageActivityResult first = usageUseCasesAt(STARTED_AT)
				.startUsageActivity(FIRST_SEAL, new BigDecimal("15.14"), ACTIVITY_LOCATION);
		UsageActivityResult second = usageUseCasesAt(STARTED_AT.plusSeconds(60))
				.startUsageActivity(SECOND_SEAL, new BigDecimal("14.00"), "Second location");

		assertEquals(FIRST_SEAL, first.sealNumber());
		assertEquals(SECOND_SEAL, second.sealNumber());
	}

	@Test
	void shouldCompleteActivityWithFixedClockAndDerivedConsumption() {
		registerReadyCylinder(FIRST_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				FIRST_SEAL, new BigDecimal("15.140"), ACTIVITY_LOCATION);

		UsageActivityResult result = usageUseCasesAt(COMPLETED_AT)
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.100"));

		assertEquals(ActivityStatus.COMPLETED, result.status());
		assertEquals(new BigDecimal("12.100"), result.returnGrossWeight().orElseThrow());
		assertEquals(3, result.returnGrossWeight().orElseThrow().scale());
		assertEquals(COMPLETED_AT, result.completedAt().orElseThrow());
		assertEquals(new BigDecimal("3.040"), result.consumedQuantity().orElseThrow());
		assertEquals(ACTIVITY_LOCATION, result.activityLocation());
	}

	@Test
	void shouldDistinguishUnknownCylinderFromMissingPendingActivity() {
		assertThrows(
				CylinderNotFoundException.class,
				() -> usageUseCasesAt(COMPLETED_AT)
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10")));

		registerReadyCylinder(FIRST_SEAL);
		PendingUsageActivityNotFoundException exception = assertThrows(
				PendingUsageActivityNotFoundException.class,
				() -> usageUseCasesAt(COMPLETED_AT)
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10")));
		assertEquals(
				"Pending usage activity was not found for seal number: " + FIRST_SEAL,
				exception.getMessage());
	}

	@Test
	void shouldPreservePendingStateAfterInvalidReturnAndAllowValidRetry() {
		registerReadyCylinder(FIRST_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				FIRST_SEAL, new BigDecimal("15.14"), ACTIVITY_LOCATION);

		assertThrows(
				IllegalArgumentException.class,
				() -> usageUseCasesAt(COMPLETED_AT)
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("15.15")));

		UsageActivityResult result = usageUseCasesAt(COMPLETED_AT)
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10"));
		assertEquals(ActivityStatus.COMPLETED, result.status());
		assertEquals(new BigDecimal("3.04"), result.consumedQuantity().orElseThrow());
	}

	@Test
	void shouldPreservePendingStateAfterCompletionBeforeStartAndAllowRetry() {
		registerReadyCylinder(FIRST_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				FIRST_SEAL, new BigDecimal("15.14"), ACTIVITY_LOCATION);

		assertThrows(
				IllegalArgumentException.class,
				() -> usageUseCasesAt(STARTED_AT.minusSeconds(1))
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10")));

		UsageActivityResult result = usageUseCasesAt(COMPLETED_AT)
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10"));
		assertEquals(COMPLETED_AT, result.completedAt().orElseThrow());
	}

	@Test
	void shouldAllowZeroConsumptionAndAllowLaterActivityAfterCompletion() {
		registerReadyCylinder(FIRST_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				FIRST_SEAL, new BigDecimal("15.140"), ACTIVITY_LOCATION);
		UsageActivityResult completed = usageUseCasesAt(COMPLETED_AT)
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("15.14"));

		assertEquals(new BigDecimal("0.000"), completed.consumedQuantity().orElseThrow());
		UsageActivityResult next = usageUseCasesAt(COMPLETED_AT.plusSeconds(60))
				.startUsageActivity(FIRST_SEAL, new BigDecimal("15.14"), "Second location");
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, next.status());
	}

	@Test
	void shouldNoLongerFindCompletedActivityAsPending() {
		registerReadyCylinder(FIRST_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				FIRST_SEAL, new BigDecimal("15.14"), ACTIVITY_LOCATION);
		usageUseCasesAt(COMPLETED_AT).completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10"));

		assertThrows(
				PendingUsageActivityNotFoundException.class,
				() -> usageUseCasesAt(COMPLETED_AT.plusSeconds(1))
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("11.00")));
	}

	@Test
	void shouldRejectNullDependenciesAndInputs() {
		Clock clock = Clock.fixed(STARTED_AT, ZoneOffset.UTC);
		assertThrows(NullPointerException.class, () -> new UsageActivityUseCases(null, activityStore, clock));
		assertThrows(NullPointerException.class, () -> new UsageActivityUseCases(cylinderStore, null, clock));
		assertThrows(NullPointerException.class, () -> new UsageActivityUseCases(cylinderStore, activityStore, null));
		assertThrows(
				NullPointerException.class,
				() -> usageUseCasesAt(STARTED_AT).startUsageActivity(
						null, new BigDecimal("15.14"), ACTIVITY_LOCATION));
		assertThrows(
				NullPointerException.class,
				() -> usageUseCasesAt(STARTED_AT).startUsageActivity(
						FIRST_SEAL, null, ACTIVITY_LOCATION));
		assertThrows(
				NullPointerException.class,
				() -> usageUseCasesAt(COMPLETED_AT)
						.completePendingUsageActivity(null, new BigDecimal("12.10")));
		assertThrows(
				NullPointerException.class,
				() -> usageUseCasesAt(COMPLETED_AT).completePendingUsageActivity(FIRST_SEAL, null));
	}

	@Test
	void shouldEnforceUsageActivityResultCanonicalConstructorContract() {
		BigDecimal departure = new BigDecimal("15.140");
		BigDecimal returned = new BigDecimal("12.100");
		BigDecimal consumed = new BigDecimal("3.040");
		UsageActivityResult result = new UsageActivityResult(
				FIRST_SEAL,
				departure,
				ACTIVITY_LOCATION,
				STARTED_AT,
				ActivityStatus.COMPLETED,
				Optional.of(returned),
				Optional.of(COMPLETED_AT),
				Optional.of(consumed));

		assertEquals(3, result.departureGrossWeight().scale());
		assertEquals(ACTIVITY_LOCATION, result.activityLocation());
		assertEquals(3, result.returnGrossWeight().orElseThrow().scale());
		assertEquals(3, result.consumedQuantity().orElseThrow().scale());
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				null, departure, ACTIVITY_LOCATION, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed)));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, null, ACTIVITY_LOCATION, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed)));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, ACTIVITY_LOCATION, null, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed)));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, ACTIVITY_LOCATION, STARTED_AT, null,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed)));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, ACTIVITY_LOCATION, STARTED_AT, ActivityStatus.COMPLETED,
				null, Optional.of(COMPLETED_AT), Optional.of(consumed)));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, ACTIVITY_LOCATION, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), null, Optional.of(consumed)));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, ACTIVITY_LOCATION, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), null));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   " })
	void shouldRejectBlankLocationInUsageActivityResult(String invalidLocation) {
		assertThrows(IllegalArgumentException.class, () -> new UsageActivityResult(
				FIRST_SEAL,
				new BigDecimal("15.140"),
				invalidLocation,
				STARTED_AT,
				ActivityStatus.AWAITING_RETURN_WEIGHT,
				Optional.empty(),
				Optional.empty(),
				Optional.empty()));
	}

	private void registerReadyCylinder(String sealNumber) {
		cylinderUseCases.registerCylinder(sealNumber, "R410A");
		cylinderUseCases.registerInitialGrossWeight(sealNumber, new BigDecimal("16.00"));
	}

	private UsageActivityUseCases usageUseCasesAt(Instant instant) {
		return usageUseCasesAt(instant, activityStore);
	}

	private UsageActivityUseCases usageUseCasesAt(Instant instant, UsageActivityStore usageActivityStore) {
		return new UsageActivityUseCases(
				cylinderStore,
				usageActivityStore,
				Clock.fixed(instant, ZoneOffset.UTC));
	}

	private int storedActivityCount(String sealNumber) {
		AtomicInteger count = new AtomicInteger();
		assertThrows(InspectionCompleteException.class, () -> activityStore.startAtomically(
				SealNumber.of(sealNumber),
				activities -> {
					count.set(activities.size());
					throw new InspectionCompleteException();
				}));
		return count.get();
	}

	private static Future<Boolean> concurrentStart(
			ExecutorService executor,
			CountDownLatch ready,
			CountDownLatch start,
			UsageActivityUseCases useCases,
			String location) {
		return executor.submit(() -> {
			ready.countDown();
			if (!start.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("concurrent start was not released");
			}
			try {
				useCases.startUsageActivity(FIRST_SEAL, new BigDecimal("15.14"), location);
				return true;
			} catch (PendingUsageActivityAlreadyExistsException exception) {
				return false;
			}
		});
	}

	private static final class TrackingUsageActivityStore implements UsageActivityStore {

		private final UsageActivityStore delegate;
		private final AtomicInteger startCallbackCalls = new AtomicInteger();

		private TrackingUsageActivityStore(UsageActivityStore delegate) {
			this.delegate = delegate;
		}

		@Override
		public UsageActivity startAtomically(
				SealNumber sealNumber,
				Function<Collection<UsageActivity>, UsageActivity> startOperation) {
			return delegate.startAtomically(sealNumber, activities -> {
				startCallbackCalls.incrementAndGet();
				return startOperation.apply(activities);
			});
		}

		@Override
		public Optional<UsageActivity> completePendingAtomically(
				SealNumber sealNumber,
				Consumer<UsageActivity> completion) {
			return delegate.completePendingAtomically(sealNumber, completion);
		}

		private int startCallbackCalls() {
			return startCallbackCalls.get();
		}
	}

	private static final class InspectionCompleteException extends RuntimeException {
	}
}
