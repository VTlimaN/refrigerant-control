package dev.sasser.refrigerantcontrol.domain;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeightTest {

	@Test
	void shouldCreateWeightInKilograms() {
		Weight weight = Weight.of(new BigDecimal("15.14"));

		assertEquals(new BigDecimal("15.14"), weight.inKilograms());
	}

	@Test
	void shouldRejectNegativeWeight() {
		assertThrows(IllegalArgumentException.class, () -> Weight.of(new BigDecimal("-0.01")));
	}

	@Test
	void shouldRejectNullWeight() {
		assertThrows(NullPointerException.class, () -> Weight.of(null));
	}

	@Test
	void shouldTreatDifferentBigDecimalScalesAsEqual() {
		Weight first = Weight.of(new BigDecimal("15.14"));
		Weight second = Weight.of(new BigDecimal("15.140"));

		assertEquals(first, second);
		assertEquals(0, first.compareTo(second));
		assertEquals(first.hashCode(), second.hashCode());
	}

	@Test
	void shouldPreserveValueWithoutRounding() {
		Weight weight = Weight.of(new BigDecimal("15.145"));

		assertEquals(new BigDecimal("15.145"), weight.inKilograms());
	}

	@Test
	void shouldSubtractWeightsWithoutChangingOperands() {
		Weight departure = Weight.of(new BigDecimal("15.14"));
		Weight returned = Weight.of(new BigDecimal("12.10"));

		Weight result = departure.subtract(returned);

		assertEquals(Weight.of(new BigDecimal("3.04")), result);
		assertNotSame(departure, result);
		assertNotSame(returned, result);
		assertEquals(new BigDecimal("15.14"), departure.inKilograms());
		assertEquals(new BigDecimal("12.10"), returned.inKilograms());
	}

	@Test
	void shouldReturnNewZeroWeightFromSubtraction() {
		Weight weight = Weight.of(new BigDecimal("10.00"));

		Weight result = weight.subtract(Weight.of(new BigDecimal("10.0")));

		assertEquals(Weight.of(BigDecimal.ZERO), result);
		assertNotSame(weight, result);
	}

	@Test
	void shouldRejectNegativeSubtractionWithoutChangingOperands() {
		Weight smaller = Weight.of(new BigDecimal("4.00"));
		Weight larger = Weight.of(new BigDecimal("5.00"));

		assertThrows(IllegalArgumentException.class, () -> smaller.subtract(larger));
		assertEquals(new BigDecimal("4.00"), smaller.inKilograms());
		assertEquals(new BigDecimal("5.00"), larger.inKilograms());
	}

	@Test
	void shouldRejectNullSubtrahend() {
		Weight weight = Weight.of(BigDecimal.ONE);

		assertThrows(NullPointerException.class, () -> weight.subtract(null));
	}
}
