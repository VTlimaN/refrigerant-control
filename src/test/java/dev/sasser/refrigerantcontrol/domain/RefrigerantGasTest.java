package dev.sasser.refrigerantcontrol.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefrigerantGasTest {

	@ParameterizedTest
	@ValueSource(strings = {"R410A", "R32", "R-22", "R407C", "R134A", "R404", "141B"})
	void shouldPreserveConfirmedOperationalName(String operationalName) {
		RefrigerantGas refrigerantGas = RefrigerantGas.of(operationalName);

		assertEquals(operationalName, refrigerantGas.operationalName());
	}

	@Test
	void shouldCompareByOperationalName() {
		RefrigerantGas first = RefrigerantGas.of("R410A");
		RefrigerantGas second = RefrigerantGas.of("R410A");

		assertEquals(first, second);
		assertEquals(first.hashCode(), second.hashCode());
	}

	@Test
	void shouldRejectNullOperationalName() {
		assertThrows(NullPointerException.class, () -> RefrigerantGas.of(null));
	}

	@Test
	void shouldRejectBlankOperationalName() {
		assertThrows(IllegalArgumentException.class, () -> RefrigerantGas.of("   "));
	}

	@Test
	void shouldRejectUnsupportedOperationalName() {
		assertThrows(IllegalArgumentException.class, () -> RefrigerantGas.of("R507"));
	}

	@Test
	void shouldNotRenameUnsupportedStandardizedNames() {
		assertThrows(IllegalArgumentException.class, () -> RefrigerantGas.of("R404A"));
		assertThrows(IllegalArgumentException.class, () -> RefrigerantGas.of("HCFC-141b"));
	}
}
