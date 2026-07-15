package dev.sasser.refrigerantcontrol.domain;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefrigerantGasSupportedNamesTest {

	private static final List<String> EXPECTED_OPERATIONAL_NAMES = List.of(
			"R410A",
			"R32",
			"R-22",
			"R407C",
			"R134A",
			"R404",
			"141B");

	@Test
	void shouldExposeExactlySevenSupportedOperationalNamesInRequiredOrder() {
		List<String> operationalNames = RefrigerantGas.supportedOperationalNames();

		assertEquals(7, operationalNames.size());
		assertEquals(EXPECTED_OPERATIONAL_NAMES, operationalNames);
	}

	@Test
	void shouldKeepSupportedOperationalNamesUnchangedAfterMutationAttempts() {
		List<String> operationalNames = RefrigerantGas.supportedOperationalNames();

		assertThrows(UnsupportedOperationException.class, () -> operationalNames.add("R507"));
		assertThrows(UnsupportedOperationException.class, () -> operationalNames.set(0, "R404A"));
		assertThrows(UnsupportedOperationException.class, operationalNames::clear);
		assertEquals(EXPECTED_OPERATIONAL_NAMES, RefrigerantGas.supportedOperationalNames());
	}
}
