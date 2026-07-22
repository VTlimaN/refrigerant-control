package dev.sasser.refrigerantcontrol.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
	void shouldReturnEmptyImmutablePendingActivityResultsWithoutReadingClock() {
		Collection<UsageActivityResult> results = usageUseCasesWithClock(new UnreadableClock())
				.listPendingUsageActivities();

		assertTrue(results.isEmpty());
		assertThrows(UnsupportedOperationException.class, results::clear);
	}

	@Test
	void shouldReturnAllPendingActivitiesWithExactValuesWithoutDependingOnOrder() {
		String firstLocation = "  Oficina técnica — Área 7!  ";
		String secondLocation = "Casa de máquinas B";
		Instant secondStartedAt = STARTED_AT.plusSeconds(60);
		registerReadyCylinder(FIRST_SEAL);
		registerReadyCylinder(SECOND_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				FIRST_SEAL,
				new BigDecimal("15.140"),
				firstLocation);
		usageUseCasesAt(secondStartedAt).startUsageActivity(
				SECOND_SEAL,
				new BigDecimal("14.00"),
				secondLocation);

		Collection<UsageActivityResult> results = usageUseCasesWithClock(new UnreadableClock())
				.listPendingUsageActivities();

		assertEquals(2, results.size());
		assertEquals(Set.of(FIRST_SEAL, SECOND_SEAL), sealNumbers(results));
		assertPendingResult(
				resultForSeal(results, FIRST_SEAL),
				FIRST_SEAL,
				new BigDecimal("15.140"),
				firstLocation,
				STARTED_AT);
		assertPendingResult(
				resultForSeal(results, SECOND_SEAL),
				SECOND_SEAL,
				new BigDecimal("14.00"),
				secondLocation,
				secondStartedAt);
	}

	@Test
	void shouldExcludeCompletedActivitiesAndPreserveStateAcrossRepeatedQueries() {
		Instant secondStartedAt = STARTED_AT.plusSeconds(60);
		registerReadyCylinder(FIRST_SEAL);
		registerReadyCylinder(SECOND_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				FIRST_SEAL,
				new BigDecimal("15.140"),
				ACTIVITY_LOCATION);
		usageUseCasesAt(secondStartedAt).startUsageActivity(
				SECOND_SEAL,
				new BigDecimal("14.00"),
				"Second location");
		usageUseCasesAt(COMPLETED_AT).completePendingUsageActivity(
				FIRST_SEAL,
				new BigDecimal("12.100"),
				false);
		UsageActivityUseCases queryUseCases = usageUseCasesWithClock(new UnreadableClock());

		Collection<UsageActivityResult> firstQuery = queryUseCases.listPendingUsageActivities();
		Collection<UsageActivityResult> secondQuery = queryUseCases.listPendingUsageActivities();

		assertEquals(Set.of(SECOND_SEAL), sealNumbers(firstQuery));
		assertEquals(Set.of(SECOND_SEAL), sealNumbers(secondQuery));
		assertPendingResult(
				resultForSeal(firstQuery, SECOND_SEAL),
				SECOND_SEAL,
				new BigDecimal("14.00"),
				"Second location",
				secondStartedAt);
		assertPendingResult(
				resultForSeal(secondQuery, SECOND_SEAL),
				SECOND_SEAL,
				new BigDecimal("14.00"),
				"Second location",
				secondStartedAt);
		UsageActivityResult completed = usageUseCasesAt(COMPLETED_AT.plusSeconds(60))
				.completePendingUsageActivity(SECOND_SEAL, new BigDecimal("12.00"), false);
		assertEquals(ActivityStatus.COMPLETED, completed.status());
	}

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
		assertFalse(result.zeroConsumptionConfirmed());
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
		assertTrue(activityStore.findPendingUsageActivities().isEmpty());
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
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10"), false);
		assertThrows(
				PendingUsageActivityNotFoundException.class,
				() -> usageUseCasesAt(STARTED_AT.plusSeconds(180))
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("11.00"), false));
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
		assertEquals(0, pendingActivityCount(FIRST_SEAL));

		UsageActivityResult result = usageUseCasesAt(STARTED_AT.plusSeconds(60)).startUsageActivity(
				FIRST_SEAL,
				new BigDecimal("15.14"),
				ACTIVITY_LOCATION);

		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, result.status());
		assertEquals(ACTIVITY_LOCATION, result.activityLocation());
		assertEquals(1, pendingActivityCount(FIRST_SEAL));
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
				assertEquals(1, pendingActivityCount(FIRST_SEAL));
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
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.100"), false);

		assertEquals(FIRST_SEAL, result.sealNumber());
		assertEquals(new BigDecimal("15.140"), result.departureGrossWeight());
		assertEquals(3, result.departureGrossWeight().scale());
		assertEquals(ActivityStatus.COMPLETED, result.status());
		assertEquals(new BigDecimal("12.100"), result.returnGrossWeight().orElseThrow());
		assertEquals(3, result.returnGrossWeight().orElseThrow().scale());
		assertEquals(COMPLETED_AT, result.completedAt().orElseThrow());
		assertEquals(new BigDecimal("3.040"), result.consumedQuantity().orElseThrow());
		assertEquals(ACTIVITY_LOCATION, result.activityLocation());
		assertFalse(result.zeroConsumptionConfirmed());
	}

	@Test
	void shouldNormalizeConfirmationToFalseForNonzeroCompletionAndPreserveExactIdentity() {
		String sealNumber = "  R 22/001+X Á  ";
		String location = "  Oficina técnica — Área 7!  ";
		registerReadyCylinder(sealNumber);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				sealNumber,
				new BigDecimal("15.140"),
				location);

		UsageActivityResult result = usageUseCasesAt(COMPLETED_AT)
				.completePendingUsageActivity(sealNumber, new BigDecimal("12.100"), true);

		assertEquals(sealNumber, result.sealNumber());
		assertEquals(location, result.activityLocation());
		assertEquals(new BigDecimal("15.140"), result.departureGrossWeight());
		assertEquals(new BigDecimal("12.100"), result.returnGrossWeight().orElseThrow());
		assertEquals(new BigDecimal("3.040"), result.consumedQuantity().orElseThrow());
		assertEquals(COMPLETED_AT, result.completedAt().orElseThrow());
		assertEquals(ActivityStatus.COMPLETED, result.status());
		assertFalse(result.zeroConsumptionConfirmed());
	}

	@Test
	void shouldDistinguishUnknownCylinderFromMissingPendingActivity() {
		assertThrows(
				CylinderNotFoundException.class,
				() -> usageUseCasesWithClock(new UnreadableClock())
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10"), false));

		registerReadyCylinder(FIRST_SEAL);
		PendingUsageActivityNotFoundException exception = assertThrows(
				PendingUsageActivityNotFoundException.class,
				() -> usageUseCasesWithClock(new UnreadableClock())
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10"), false));
		assertEquals(
				"Pending usage activity was not found for seal number: " + FIRST_SEAL,
				exception.getMessage());
	}

	@Test
	void shouldReturnTypedGreaterFailurePreservePendingStateAndAllowValidRetry() {
		registerReadyCylinder(FIRST_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				FIRST_SEAL, new BigDecimal("15.14"), ACTIVITY_LOCATION);

		assertThrows(
				ReturnGrossWeightGreaterThanDepartureException.class,
				() -> usageUseCasesWithClock(new UnreadableClock())
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("15.15"), false));
		assertPendingActivity(FIRST_SEAL, new BigDecimal("15.14"));

		UsageActivityResult result = usageUseCasesAt(COMPLETED_AT)
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10"), false);
		assertEquals(ActivityStatus.COMPLETED, result.status());
		assertEquals(new BigDecimal("3.04"), result.consumedQuantity().orElseThrow());
		assertFalse(result.zeroConsumptionConfirmed());
	}

	@Test
	void shouldReturnTypedNegativeFailurePreservePendingStateAndAllowValidRetry() {
		registerReadyCylinder(FIRST_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				FIRST_SEAL, new BigDecimal("15.140"), ACTIVITY_LOCATION);

		assertThrows(
				NegativeReturnGrossWeightException.class,
				() -> usageUseCasesWithClock(new UnreadableClock())
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("-0.001"), false));
		assertPendingActivity(FIRST_SEAL, new BigDecimal("15.140"));

		UsageActivityResult result = usageUseCasesAt(COMPLETED_AT)
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.100"), false);

		assertEquals(ActivityStatus.COMPLETED, result.status());
		assertEquals(new BigDecimal("12.100"), result.returnGrossWeight().orElseThrow());
		assertEquals(COMPLETED_AT, result.completedAt().orElseThrow());
		assertFalse(result.zeroConsumptionConfirmed());
	}

	@Test
	void shouldPreservePendingStateAfterCompletionBeforeStartAndAllowRetry() {
		registerReadyCylinder(FIRST_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				FIRST_SEAL, new BigDecimal("15.14"), ACTIVITY_LOCATION);

		assertThrows(
				IllegalArgumentException.class,
				() -> usageUseCasesAt(STARTED_AT.minusSeconds(1))
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10"), false));
		assertPendingActivity(FIRST_SEAL, new BigDecimal("15.14"));

		UsageActivityResult result = usageUseCasesAt(COMPLETED_AT)
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10"), false);
		assertEquals(COMPLETED_AT, result.completedAt().orElseThrow());
	}

	@Test
	void shouldRequireZeroConfirmationPreservePendingStateAndAllowConfirmedRetry() {
		registerReadyCylinder(FIRST_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				FIRST_SEAL, new BigDecimal("15.140"), ACTIVITY_LOCATION);
		assertThrows(
				ZeroConsumptionConfirmationRequiredException.class,
				() -> usageUseCasesWithClock(new UnreadableClock())
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("15.14"), false));
		assertPendingActivity(FIRST_SEAL, new BigDecimal("15.140"));

		UsageActivityResult completed = usageUseCasesAt(COMPLETED_AT)
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("15.14"), true);

		assertEquals(new BigDecimal("0.000"), completed.consumedQuantity().orElseThrow());
		assertEquals(3, completed.consumedQuantity().orElseThrow().scale());
		assertEquals(new BigDecimal("15.14"), completed.returnGrossWeight().orElseThrow());
		assertEquals(2, completed.returnGrossWeight().orElseThrow().scale());
		assertEquals(COMPLETED_AT, completed.completedAt().orElseThrow());
		assertTrue(completed.zeroConsumptionConfirmed());
		UsageActivityResult next = usageUseCasesAt(COMPLETED_AT.plusSeconds(60))
				.startUsageActivity(FIRST_SEAL, new BigDecimal("15.14"), "Second location");
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, next.status());
	}

	@Test
	void shouldNoLongerFindCompletedActivityAsPending() {
		registerReadyCylinder(FIRST_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(
				FIRST_SEAL, new BigDecimal("15.14"), ACTIVITY_LOCATION);
		usageUseCasesAt(COMPLETED_AT)
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10"), false);

		assertThrows(
				PendingUsageActivityNotFoundException.class,
				() -> usageUseCasesWithClock(new UnreadableClock())
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("11.00"), false));
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
						.completePendingUsageActivity(null, new BigDecimal("12.10"), false));
		assertThrows(
				NullPointerException.class,
				() -> usageUseCasesAt(COMPLETED_AT)
						.completePendingUsageActivity(FIRST_SEAL, null, false));
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
				Optional.of(consumed),
				false);

		assertEquals(3, result.departureGrossWeight().scale());
		assertEquals(ACTIVITY_LOCATION, result.activityLocation());
		assertEquals(3, result.returnGrossWeight().orElseThrow().scale());
		assertEquals(3, result.consumedQuantity().orElseThrow().scale());
		assertFalse(result.zeroConsumptionConfirmed());
		UsageActivityResult confirmedZero = new UsageActivityResult(
				FIRST_SEAL,
				departure,
				ACTIVITY_LOCATION,
				STARTED_AT,
				ActivityStatus.COMPLETED,
				Optional.of(departure),
				Optional.of(COMPLETED_AT),
				Optional.of(new BigDecimal("0.000")),
				true);
		assertTrue(confirmedZero.zeroConsumptionConfirmed());
		assertThrows(IllegalArgumentException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, ACTIVITY_LOCATION, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(departure), Optional.of(COMPLETED_AT), Optional.of(new BigDecimal("0.000")), false));
		assertThrows(IllegalArgumentException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, ACTIVITY_LOCATION, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed), true));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				null, departure, ACTIVITY_LOCATION, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed), false));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, null, ACTIVITY_LOCATION, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed), false));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, ACTIVITY_LOCATION, null, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed), false));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, ACTIVITY_LOCATION, STARTED_AT, null,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed), false));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, ACTIVITY_LOCATION, STARTED_AT, ActivityStatus.COMPLETED,
				null, Optional.of(COMPLETED_AT), Optional.of(consumed), false));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, ACTIVITY_LOCATION, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), null, Optional.of(consumed), false));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, ACTIVITY_LOCATION, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), null, false));
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
				Optional.empty(),
				false));
	}

	private void registerReadyCylinder(String sealNumber) {
		cylinderUseCases.registerCylinder(sealNumber, "R410A");
		cylinderUseCases.registerInitialGrossWeight(sealNumber, new BigDecimal("16.00"));
	}

	private UsageActivityUseCases usageUseCasesAt(Instant instant) {
		return usageUseCasesAt(instant, activityStore);
	}

	private UsageActivityUseCases usageUseCasesAt(Instant instant, UsageActivityStore usageActivityStore) {
		return usageUseCasesWithClock(Clock.fixed(instant, ZoneOffset.UTC), usageActivityStore);
	}

	private UsageActivityUseCases usageUseCasesWithClock(Clock clock) {
		return usageUseCasesWithClock(clock, activityStore);
	}

	private UsageActivityUseCases usageUseCasesWithClock(Clock clock, UsageActivityStore usageActivityStore) {
		return new UsageActivityUseCases(
				cylinderStore,
				usageActivityStore,
				clock);
	}

	private int pendingActivityCount(String sealNumber) {
		int count = 0;
		for (UsageActivity activity : activityStore.findPendingUsageActivities()) {
			if (activity.cylinder().sealNumber().equals(SealNumber.of(sealNumber))) {
				count++;
			}
		}
		return count;
	}

	private void assertPendingActivity(String sealNumber, BigDecimal departureGrossWeight) {
		List<UsageActivity> matchingActivities = activityStore.findPendingUsageActivities()
				.stream()
				.filter(activity -> activity.cylinder().sealNumber().equals(SealNumber.of(sealNumber)))
				.toList();
		assertEquals(1, matchingActivities.size());
		UsageActivity activity = matchingActivities.getFirst();
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, activity.status());
		assertEquals(departureGrossWeight, activity.departureGrossWeight().inKilograms());
		assertTrue(activity.returnGrossWeight().isEmpty());
		assertTrue(activity.completedAt().isEmpty());
		assertTrue(activity.consumedQuantity().isEmpty());
		assertFalse(activity.zeroConsumptionConfirmed());
	}

	private static Set<String> sealNumbers(Collection<UsageActivityResult> results) {
		Set<String> sealNumbers = new HashSet<>();
		for (UsageActivityResult result : results) {
			sealNumbers.add(result.sealNumber());
		}
		return sealNumbers;
	}

	private static UsageActivityResult resultForSeal(
			Collection<UsageActivityResult> results,
			String sealNumber) {
		List<UsageActivityResult> matchingResults = results.stream()
				.filter(result -> result.sealNumber().equals(sealNumber))
				.toList();
		assertEquals(1, matchingResults.size());
		return matchingResults.getFirst();
	}

	private static void assertPendingResult(
			UsageActivityResult result,
			String sealNumber,
			BigDecimal departureGrossWeight,
			String activityLocation,
			Instant startedAt) {
		assertEquals(sealNumber, result.sealNumber());
		assertEquals(departureGrossWeight, result.departureGrossWeight());
		assertEquals(departureGrossWeight.scale(), result.departureGrossWeight().scale());
		assertEquals(activityLocation, result.activityLocation());
		assertEquals(startedAt, result.startedAt());
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, result.status());
		assertTrue(result.returnGrossWeight().isEmpty());
		assertTrue(result.completedAt().isEmpty());
		assertTrue(result.consumedQuantity().isEmpty());
		assertFalse(result.zeroConsumptionConfirmed());
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
		public Collection<UsageActivity> findPendingUsageActivities() {
			return delegate.findPendingUsageActivities();
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

	private static final class UnreadableClock extends Clock {

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			throw new AssertionError("clock must not be read");
		}
	}
}
