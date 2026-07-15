package dev.sasser.refrigerantcontrol.application;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.sasser.refrigerantcontrol.domain.SealNumber;
import dev.sasser.refrigerantcontrol.infrastructure.memory.InMemoryCylinderStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CylinderUseCasesTest {

	private static final String SEAL_NUMBER = "LACRE-001";

	private final InMemoryCylinderStore cylinderStore = new InMemoryCylinderStore();
	private final CylinderUseCases useCases = new CylinderUseCases(cylinderStore);

	@Test
	void shouldRegisterCylinderWithoutInitialGrossWeight() {
		CylinderResult result = useCases.registerCylinder(SEAL_NUMBER, "R410A");

		assertEquals(SEAL_NUMBER, result.sealNumber());
		assertEquals("R410A", result.operationalRefrigerantName());
		assertTrue(result.initialGrossWeight().isEmpty());
		assertTrue(cylinderStore.findBySealNumber(SealNumber.of(SEAL_NUMBER)).isPresent());
	}

	@Test
	void shouldRejectDuplicateSealAcrossSeparateRegistrations() {
		useCases.registerCylinder(new String(SEAL_NUMBER), "R410A");

		CylinderAlreadyRegisteredException exception = assertThrows(
				CylinderAlreadyRegisteredException.class,
				() -> useCases.registerCylinder(new String(SEAL_NUMBER), "R32"));

		assertEquals("Cylinder is already registered for seal number: " + SEAL_NUMBER, exception.getMessage());
		assertEquals(
				"R410A",
				cylinderStore.findBySealNumber(SealNumber.of(SEAL_NUMBER))
						.orElseThrow()
						.refrigerantGas()
						.operationalName());
	}

	@Test
	void shouldDelegateUnsupportedOperationalNameToDomain() {
		assertThrows(
				IllegalArgumentException.class,
				() -> useCases.registerCylinder(SEAL_NUMBER, "R22"));

		assertTrue(cylinderStore.findBySealNumber(SealNumber.of(SEAL_NUMBER)).isEmpty());
	}

	@Test
	void shouldRegisterInitialGrossWeightSeparatelyAndPreserveScale() {
		useCases.registerCylinder(SEAL_NUMBER, "R410A");

		CylinderResult result = useCases.registerInitialGrossWeight(SEAL_NUMBER, new BigDecimal("16.140"));

		assertEquals(new BigDecimal("16.140"), result.initialGrossWeight().orElseThrow());
		assertEquals(
				new BigDecimal("16.140"),
				cylinderStore.findBySealNumber(SealNumber.of(SEAL_NUMBER))
						.orElseThrow()
						.initialGrossWeight()
						.orElseThrow()
						.inKilograms());
	}

	@Test
	void shouldFailClearlyWhenRegisteringWeightForUnknownCylinder() {
		CylinderNotFoundException exception = assertThrows(
				CylinderNotFoundException.class,
				() -> useCases.registerInitialGrossWeight(SEAL_NUMBER, new BigDecimal("16.00")));

		assertEquals("Cylinder was not found for seal number: " + SEAL_NUMBER, exception.getMessage());
	}

	@Test
	void shouldPreserveStoredCylinderAfterInvalidOrSecondInitialWeightRegistration() {
		useCases.registerCylinder(SEAL_NUMBER, "R410A");

		assertThrows(
				IllegalArgumentException.class,
				() -> useCases.registerInitialGrossWeight(SEAL_NUMBER, new BigDecimal("-0.01")));
		assertTrue(cylinderStore.findBySealNumber(SealNumber.of(SEAL_NUMBER))
				.orElseThrow()
				.initialGrossWeight()
				.isEmpty());

		useCases.registerInitialGrossWeight(SEAL_NUMBER, new BigDecimal("16.00"));
		assertThrows(
				IllegalStateException.class,
				() -> useCases.registerInitialGrossWeight(SEAL_NUMBER, new BigDecimal("17.00")));
		assertEquals(
				new BigDecimal("16.00"),
				cylinderStore.findBySealNumber(SealNumber.of(SEAL_NUMBER))
						.orElseThrow()
						.initialGrossWeight()
						.orElseThrow()
						.inKilograms());
	}

	@Test
	void shouldRejectNullDependenciesAndInputs() {
		assertThrows(NullPointerException.class, () -> new CylinderUseCases(null));
		assertThrows(NullPointerException.class, () -> useCases.registerCylinder(null, "R410A"));
		assertThrows(NullPointerException.class, () -> useCases.registerCylinder(SEAL_NUMBER, null));
		assertThrows(
				NullPointerException.class,
				() -> useCases.registerInitialGrossWeight(null, new BigDecimal("16.00")));
		assertThrows(
				NullPointerException.class,
				() -> useCases.registerInitialGrossWeight(SEAL_NUMBER, null));
	}

	@Test
	void shouldEnforceCylinderResultCanonicalConstructorContract() {
		BigDecimal preservedWeight = new BigDecimal("16.140");
		CylinderResult result = new CylinderResult(SEAL_NUMBER, "R410A", Optional.of(preservedWeight));

		assertEquals(preservedWeight, result.initialGrossWeight().orElseThrow());
		assertEquals(3, result.initialGrossWeight().orElseThrow().scale());
		assertFalse(result.initialGrossWeight().isEmpty());
		assertThrows(NullPointerException.class, () -> new CylinderResult(null, "R410A", Optional.empty()));
		assertThrows(NullPointerException.class, () -> new CylinderResult(SEAL_NUMBER, null, Optional.empty()));
		assertThrows(NullPointerException.class, () -> new CylinderResult(SEAL_NUMBER, "R410A", null));
	}
}
