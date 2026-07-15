package dev.sasser.refrigerantcontrol.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.sasser.refrigerantcontrol.domain.ActivityStatus;
import dev.sasser.refrigerantcontrol.domain.SealNumber;
import dev.sasser.refrigerantcontrol.infrastructure.memory.InMemoryCylinderStore;
import dev.sasser.refrigerantcontrol.infrastructure.memory.InMemoryUsageActivityStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsageActivityUseCasesTest {

	private static final String FIRST_SEAL = "LACRE-001";
	private static final String SECOND_SEAL = "LACRE-002";
	private static final Instant STARTED_AT = Instant.parse("2026-07-15T12:00:00Z");
	private static final Instant COMPLETED_AT = Instant.parse("2026-07-15T13:00:00Z");

	private final InMemoryCylinderStore cylinderStore = new InMemoryCylinderStore();
	private final InMemoryUsageActivityStore activityStore = new InMemoryUsageActivityStore();
	private final CylinderUseCases cylinderUseCases = new CylinderUseCases(cylinderStore);

	@Test
	void shouldStartActivityWithFixedClockAndPreserveDepartureScale() {
		registerReadyCylinder(FIRST_SEAL);
		UsageActivityUseCases useCases = usageUseCasesAt(STARTED_AT);

		UsageActivityResult result = useCases.startUsageActivity(FIRST_SEAL, new BigDecimal("15.140"));

		assertEquals(FIRST_SEAL, result.sealNumber());
		assertEquals(new BigDecimal("15.140"), result.departureGrossWeight());
		assertEquals(3, result.departureGrossWeight().scale());
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
				() -> usageUseCasesAt(STARTED_AT).startUsageActivity(FIRST_SEAL, new BigDecimal("15.14")));

		assertEquals("Cylinder was not found for seal number: " + FIRST_SEAL, exception.getMessage());
	}

	@Test
	void shouldBlockStartWithoutInitialWeightAndStoreNothing() {
		cylinderUseCases.registerCylinder(FIRST_SEAL, "R410A");

		assertThrows(
				IllegalStateException.class,
				() -> usageUseCasesAt(STARTED_AT).startUsageActivity(FIRST_SEAL, new BigDecimal("15.14")));
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
		assertNotSame(
				cylinderStore.findBySealNumber(SealNumber.of(FIRST_SEAL)).orElseThrow(),
				cylinderStore.findBySealNumber(SealNumber.of(FIRST_SEAL)).orElseThrow());
		usageUseCasesAt(STARTED_AT).startUsageActivity(FIRST_SEAL, new BigDecimal("15.14"));

		assertThrows(
				IllegalStateException.class,
				() -> usageUseCasesAt(STARTED_AT.plusSeconds(60))
						.startUsageActivity(FIRST_SEAL, new BigDecimal("14.00")));
		usageUseCasesAt(STARTED_AT.plusSeconds(120))
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.10"));
		assertThrows(
				PendingUsageActivityNotFoundException.class,
				() -> usageUseCasesAt(STARTED_AT.plusSeconds(180))
						.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("11.00")));
	}

	@Test
	void shouldAllowPendingActivitiesForDifferentSeals() {
		registerReadyCylinder(FIRST_SEAL);
		registerReadyCylinder(SECOND_SEAL);

		UsageActivityResult first = usageUseCasesAt(STARTED_AT)
				.startUsageActivity(FIRST_SEAL, new BigDecimal("15.14"));
		UsageActivityResult second = usageUseCasesAt(STARTED_AT.plusSeconds(60))
				.startUsageActivity(SECOND_SEAL, new BigDecimal("14.00"));

		assertEquals(FIRST_SEAL, first.sealNumber());
		assertEquals(SECOND_SEAL, second.sealNumber());
	}

	@Test
	void shouldCompleteActivityWithFixedClockAndDerivedConsumption() {
		registerReadyCylinder(FIRST_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(FIRST_SEAL, new BigDecimal("15.140"));

		UsageActivityResult result = usageUseCasesAt(COMPLETED_AT)
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("12.100"));

		assertEquals(ActivityStatus.COMPLETED, result.status());
		assertEquals(new BigDecimal("12.100"), result.returnGrossWeight().orElseThrow());
		assertEquals(3, result.returnGrossWeight().orElseThrow().scale());
		assertEquals(COMPLETED_AT, result.completedAt().orElseThrow());
		assertEquals(new BigDecimal("3.040"), result.consumedQuantity().orElseThrow());
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
		usageUseCasesAt(STARTED_AT).startUsageActivity(FIRST_SEAL, new BigDecimal("15.14"));

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
		usageUseCasesAt(STARTED_AT).startUsageActivity(FIRST_SEAL, new BigDecimal("15.14"));

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
		usageUseCasesAt(STARTED_AT).startUsageActivity(FIRST_SEAL, new BigDecimal("15.140"));
		UsageActivityResult completed = usageUseCasesAt(COMPLETED_AT)
				.completePendingUsageActivity(FIRST_SEAL, new BigDecimal("15.14"));

		assertEquals(new BigDecimal("0.000"), completed.consumedQuantity().orElseThrow());
		UsageActivityResult next = usageUseCasesAt(COMPLETED_AT.plusSeconds(60))
				.startUsageActivity(FIRST_SEAL, new BigDecimal("15.14"));
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, next.status());
	}

	@Test
	void shouldNoLongerFindCompletedActivityAsPending() {
		registerReadyCylinder(FIRST_SEAL);
		usageUseCasesAt(STARTED_AT).startUsageActivity(FIRST_SEAL, new BigDecimal("15.14"));
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
				() -> usageUseCasesAt(STARTED_AT).startUsageActivity(null, new BigDecimal("15.14")));
		assertThrows(
				NullPointerException.class,
				() -> usageUseCasesAt(STARTED_AT).startUsageActivity(FIRST_SEAL, null));
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
				STARTED_AT,
				ActivityStatus.COMPLETED,
				Optional.of(returned),
				Optional.of(COMPLETED_AT),
				Optional.of(consumed));

		assertEquals(3, result.departureGrossWeight().scale());
		assertEquals(3, result.returnGrossWeight().orElseThrow().scale());
		assertEquals(3, result.consumedQuantity().orElseThrow().scale());
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				null, departure, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed)));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, null, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed)));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, null, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed)));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, STARTED_AT, null,
				Optional.of(returned), Optional.of(COMPLETED_AT), Optional.of(consumed)));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, STARTED_AT, ActivityStatus.COMPLETED,
				null, Optional.of(COMPLETED_AT), Optional.of(consumed)));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), null, Optional.of(consumed)));
		assertThrows(NullPointerException.class, () -> new UsageActivityResult(
				FIRST_SEAL, departure, STARTED_AT, ActivityStatus.COMPLETED,
				Optional.of(returned), Optional.of(COMPLETED_AT), null));
	}

	private void registerReadyCylinder(String sealNumber) {
		cylinderUseCases.registerCylinder(sealNumber, "R410A");
		cylinderUseCases.registerInitialGrossWeight(sealNumber, new BigDecimal("16.00"));
	}

	private UsageActivityUseCases usageUseCasesAt(Instant instant) {
		return new UsageActivityUseCases(
				cylinderStore,
				activityStore,
				Clock.fixed(instant, ZoneOffset.UTC));
	}
}
