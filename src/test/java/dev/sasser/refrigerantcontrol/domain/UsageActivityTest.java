package dev.sasser.refrigerantcontrol.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsageActivityTest {

	private static final Instant STARTED_AT = Instant.parse("2026-07-14T12:00:00Z");
	private static final Instant COMPLETED_AT = Instant.parse("2026-07-14T13:00:00Z");

	@Test
	void shouldStartAwaitingReturnWeight() {
		UsageActivity activity = startActivity(Weight.of(new BigDecimal("15.14")));

		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, activity.status());
		assertEquals(STARTED_AT, activity.startedAt());
		assertPending(activity);
	}

	@Test
	void shouldCompleteActivityAndDeriveConsumption() {
		UsageActivity activity = startActivity(Weight.of(new BigDecimal("15.14")));
		Weight returnGrossWeight = Weight.of(new BigDecimal("12.10"));

		activity.complete(returnGrossWeight, COMPLETED_AT);

		assertEquals(ActivityStatus.COMPLETED, activity.status());
		assertEquals(returnGrossWeight, activity.returnGrossWeight().orElseThrow());
		assertEquals(COMPLETED_AT, activity.completedAt().orElseThrow());
		assertEquals(Weight.of(new BigDecimal("3.04")), activity.consumedQuantity().orElseThrow());
	}

	@Test
	void shouldAllowZeroConsumption() {
		Weight departureGrossWeight = Weight.of(new BigDecimal("15.140"));
		UsageActivity activity = startActivity(departureGrossWeight);

		activity.complete(Weight.of(new BigDecimal("15.14")), COMPLETED_AT);

		assertEquals(Weight.of(BigDecimal.ZERO), activity.consumedQuantity().orElseThrow());
	}

	@Test
	void shouldRemainPendingWhenReturnWeightIsGreaterThanDepartureWeight() {
		UsageActivity activity = startActivity(Weight.of(new BigDecimal("15.14")));

		assertThrows(
				IllegalArgumentException.class,
				() -> activity.complete(Weight.of(new BigDecimal("15.15")), COMPLETED_AT));
		assertPending(activity);
	}

	@Test
	void shouldRemainPendingWhenCompletionInstantIsBeforeStart() {
		UsageActivity activity = startActivity(Weight.of(new BigDecimal("15.14")));

		assertThrows(
				IllegalArgumentException.class,
				() -> activity.complete(
						Weight.of(new BigDecimal("12.10")),
						STARTED_AT.minusSeconds(1)));
		assertPending(activity);
	}

	@Test
	void shouldRemainPendingWhenCompletionArgumentsAreNull() {
		UsageActivity activity = startActivity(Weight.of(new BigDecimal("15.14")));

		assertThrows(NullPointerException.class, () -> activity.complete(null, COMPLETED_AT));
		assertPending(activity);
		assertThrows(
				NullPointerException.class,
				() -> activity.complete(Weight.of(new BigDecimal("12.10")), null));
		assertPending(activity);
	}

	@Test
	void shouldPreserveFirstCompletionWhenCompletedAgain() {
		UsageActivity activity = startActivity(Weight.of(new BigDecimal("15.14")));
		Weight firstReturnWeight = Weight.of(new BigDecimal("12.10"));
		activity.complete(firstReturnWeight, COMPLETED_AT);
		Weight firstConsumption = activity.consumedQuantity().orElseThrow();

		assertThrows(
				IllegalStateException.class,
				() -> activity.complete(
						Weight.of(new BigDecimal("11.00")),
						COMPLETED_AT.plusSeconds(60)));
		assertEquals(ActivityStatus.COMPLETED, activity.status());
		assertEquals(firstReturnWeight, activity.returnGrossWeight().orElseThrow());
		assertEquals(COMPLETED_AT, activity.completedAt().orElseThrow());
		assertEquals(firstConsumption, activity.consumedQuantity().orElseThrow());
	}

	private static UsageActivity startActivity(Weight departureGrossWeight) {
		Cylinder cylinder = Cylinder.register(SealNumber.of("LACRE-001"), RefrigerantGas.of("R410A"));
		cylinder.registerInitialGrossWeight(Weight.of(new BigDecimal("16.00")));
		return new UsageActivityStarter().start(cylinder, departureGrossWeight, STARTED_AT, List.of());
	}

	private static void assertPending(UsageActivity activity) {
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, activity.status());
		assertTrue(activity.returnGrossWeight().isEmpty());
		assertTrue(activity.completedAt().isEmpty());
		assertTrue(activity.consumedQuantity().isEmpty());
	}
}
