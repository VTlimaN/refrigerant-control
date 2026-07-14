package dev.sasser.refrigerantcontrol.domain;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CylinderTest {

	@Test
	void shouldRegisterCylinderWithoutInitialGrossWeight() {
		SealNumber sealNumber = SealNumber.of("LACRE-001");
		RefrigerantGas refrigerantGas = RefrigerantGas.of("R410A");

		Cylinder cylinder = Cylinder.register(sealNumber, refrigerantGas);

		assertSame(sealNumber, cylinder.sealNumber());
		assertSame(refrigerantGas, cylinder.refrigerantGas());
		assertTrue(cylinder.initialGrossWeight().isEmpty());
	}

	@Test
	void shouldRegisterInitialGrossWeightSeparately() {
		Cylinder cylinder = cylinder("LACRE-001", "R410A");
		Weight initialGrossWeight = Weight.of(new BigDecimal("15.14"));

		cylinder.registerInitialGrossWeight(initialGrossWeight);

		assertEquals(initialGrossWeight, cylinder.initialGrossWeight().orElseThrow());
	}

	@Test
	void shouldRejectSecondInitialGrossWeightRegistration() {
		Cylinder cylinder = cylinder("LACRE-001", "R410A");
		Weight originalWeight = Weight.of(new BigDecimal("15.14"));
		cylinder.registerInitialGrossWeight(originalWeight);

		assertThrows(
				IllegalStateException.class,
				() -> cylinder.registerInitialGrossWeight(Weight.of(new BigDecimal("14.90"))));
		assertEquals(originalWeight, cylinder.initialGrossWeight().orElseThrow());
	}

	@Test
	void shouldUseOnlySealNumberForIdentity() {
		Cylinder first = cylinder("LACRE-001", "R410A");
		first.registerInitialGrossWeight(Weight.of(new BigDecimal("15.14")));
		Cylinder sameIdentity = cylinder("LACRE-001", "R32");
		Cylinder differentIdentity = cylinder("LACRE-002", "R410A");

		assertEquals(first, sameIdentity);
		assertEquals(first.hashCode(), sameIdentity.hashCode());
		assertNotEquals(first, differentIdentity);
		assertTrue(sameIdentity.initialGrossWeight().isEmpty());
	}

	@Test
	void shouldKeepSealNumberAndRefrigerantAssociationUnchanged() {
		Cylinder cylinder = cylinder("LACRE-001", "R404");
		SealNumber sealNumber = cylinder.sealNumber();
		RefrigerantGas refrigerantGas = cylinder.refrigerantGas();

		cylinder.registerInitialGrossWeight(Weight.of(new BigDecimal("12.50")));

		assertSame(sealNumber, cylinder.sealNumber());
		assertSame(refrigerantGas, cylinder.refrigerantGas());
	}

	@Test
	void shouldRejectNullRegistrationData() {
		RefrigerantGas refrigerantGas = RefrigerantGas.of("R410A");
		SealNumber sealNumber = SealNumber.of("LACRE-001");

		assertThrows(NullPointerException.class, () -> Cylinder.register(null, refrigerantGas));
		assertThrows(NullPointerException.class, () -> Cylinder.register(sealNumber, null));
		assertThrows(
				NullPointerException.class,
				() -> Cylinder.register(sealNumber, refrigerantGas).registerInitialGrossWeight(null));
	}

	private static Cylinder cylinder(String sealNumber, String operationalName) {
		return Cylinder.register(SealNumber.of(sealNumber), RefrigerantGas.of(operationalName));
	}
}
