package dev.sasser.refrigerantcontrol.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsageActivityStarterTest {

	private static final Instant STARTED_AT = Instant.parse("2026-07-14T12:00:00Z");
	private static final Weight DEPARTURE_WEIGHT = Weight.of(new BigDecimal("15.14"));
	private static final String ACTIVITY_LOCATION = "Technical room";

	private final UsageActivityStarter starter = new UsageActivityStarter();

	@Test
	void shouldRequireInitialGrossWeightBeforeFirstActivity() {
		Cylinder cylinder = cylinder("LACRE-001");

		assertThrows(
				IllegalStateException.class,
				() -> starter.start(cylinder, DEPARTURE_WEIGHT, ACTIVITY_LOCATION, STARTED_AT, List.of()));
	}

	@Test
	void shouldStartActivityAfterSeparateInitialWeightRegistration() {
		Cylinder cylinder = readyCylinder("LACRE-001");

		UsageActivity activity = starter.start(
				cylinder,
				DEPARTURE_WEIGHT,
				ACTIVITY_LOCATION,
				STARTED_AT,
				List.of());

		assertEquals(cylinder, activity.cylinder());
		assertEquals(DEPARTURE_WEIGHT, activity.departureGrossWeight());
		assertEquals(ACTIVITY_LOCATION, activity.activityLocation());
		assertEquals(STARTED_AT, activity.startedAt());
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, activity.status());
	}

	@Test
	void shouldBlockAnotherPendingActivityForSameCylinder() {
		Cylinder cylinder = readyCylinder("LACRE-001");
		UsageActivity existingActivity = starter.start(
				cylinder, DEPARTURE_WEIGHT, ACTIVITY_LOCATION, STARTED_AT, List.of());

		assertThrows(
				IllegalStateException.class,
				() -> starter.start(
						cylinder,
						DEPARTURE_WEIGHT,
						ACTIVITY_LOCATION,
						STARTED_AT.plusSeconds(60),
						List.of(existingActivity)));
	}

	@Test
	void shouldBlockPendingActivityAcrossCylinderInstancesWithSameSealNumber() {
		Cylinder existingCylinder = readyCylinder("LACRE-001");
		UsageActivity existingActivity = starter.start(
				existingCylinder,
				DEPARTURE_WEIGHT,
				ACTIVITY_LOCATION,
				STARTED_AT,
				List.of());
		Cylinder sameIdentity = readyCylinder("LACRE-001");

		assertThrows(
				IllegalStateException.class,
				() -> starter.start(
						sameIdentity,
						DEPARTURE_WEIGHT,
						ACTIVITY_LOCATION,
						STARTED_AT.plusSeconds(60),
						List.of(existingActivity)));
	}

	@Test
	void shouldAllowActivityAfterPreviousActivityIsCompleted() {
		Cylinder cylinder = readyCylinder("LACRE-001");
		UsageActivity previousActivity = starter.start(
				cylinder, DEPARTURE_WEIGHT, ACTIVITY_LOCATION, STARTED_AT, List.of());
		previousActivity.complete(
				Weight.of(new BigDecimal("12.10")),
				STARTED_AT.plusSeconds(60),
				false);

		UsageActivity nextActivity = starter.start(
				cylinder,
				Weight.of(new BigDecimal("12.10")),
				"Second location",
				STARTED_AT.plusSeconds(120),
				List.of(previousActivity));

		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, nextActivity.status());
	}

	@Test
	void shouldIgnorePendingActivityForDifferentCylinder() {
		Cylinder firstCylinder = readyCylinder("LACRE-001");
		UsageActivity existingActivity = starter.start(
				firstCylinder,
				DEPARTURE_WEIGHT,
				ACTIVITY_LOCATION,
				STARTED_AT,
				List.of());
		Cylinder otherCylinder = readyCylinder("LACRE-002");

		UsageActivity activity = starter.start(
				otherCylinder,
				DEPARTURE_WEIGHT,
				"Other location",
				STARTED_AT.plusSeconds(60),
				List.of(existingActivity));

		assertEquals(otherCylinder, activity.cylinder());
	}

	@Test
	void shouldRejectNullArgumentsAndCollectionElements() {
		Cylinder cylinder = readyCylinder("LACRE-001");

		assertThrows(
				NullPointerException.class,
				() -> starter.start(null, DEPARTURE_WEIGHT, ACTIVITY_LOCATION, STARTED_AT, List.of()));
		assertThrows(
				NullPointerException.class,
				() -> starter.start(cylinder, null, ACTIVITY_LOCATION, STARTED_AT, List.of()));
		assertThrows(
				NullPointerException.class,
				() -> starter.start(cylinder, DEPARTURE_WEIGHT, ACTIVITY_LOCATION, null, List.of()));
		assertThrows(
				NullPointerException.class,
				() -> starter.start(cylinder, DEPARTURE_WEIGHT, ACTIVITY_LOCATION, STARTED_AT, null));
		assertThrows(
				NullPointerException.class,
				() -> starter.start(
						cylinder,
						DEPARTURE_WEIGHT,
						ACTIVITY_LOCATION,
						STARTED_AT,
						Collections.singletonList(null)));
	}

	@Test
	void shouldPreserveNonblankLocationExactly() {
		Cylinder cylinder = readyCylinder("LACRE-001");
		String location = "  Oficina técnica — Área 1  ";

		UsageActivity activity = starter.start(
				cylinder,
				DEPARTURE_WEIGHT,
				location,
				STARTED_AT,
				List.of());

		assertEquals(location, activity.activityLocation());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void shouldRejectBlankLocation(String location) {
		Cylinder cylinder = readyCylinder("LACRE-001");

		assertThrows(
				IllegalArgumentException.class,
				() -> starter.start(cylinder, DEPARTURE_WEIGHT, location, STARTED_AT, List.of()));
	}

	private static Cylinder readyCylinder(String sealNumber) {
		Cylinder cylinder = cylinder(sealNumber);
		cylinder.registerInitialGrossWeight(Weight.of(new BigDecimal("16.00")));
		return cylinder;
	}

	private static Cylinder cylinder(String sealNumber) {
		return Cylinder.register(SealNumber.of(sealNumber), RefrigerantGas.of("R410A"));
	}
}
