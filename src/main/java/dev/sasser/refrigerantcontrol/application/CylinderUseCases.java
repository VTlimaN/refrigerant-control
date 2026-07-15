package dev.sasser.refrigerantcontrol.application;

import java.math.BigDecimal;
import java.util.Objects;

import dev.sasser.refrigerantcontrol.application.port.CylinderStore;
import dev.sasser.refrigerantcontrol.domain.Cylinder;
import dev.sasser.refrigerantcontrol.domain.RefrigerantGas;
import dev.sasser.refrigerantcontrol.domain.SealNumber;
import dev.sasser.refrigerantcontrol.domain.Weight;

public final class CylinderUseCases {

	private final CylinderStore cylinderStore;

	public CylinderUseCases(CylinderStore cylinderStore) {
		this.cylinderStore = Objects.requireNonNull(cylinderStore, "cylinder store must not be null");
	}

	public CylinderResult registerCylinder(String sealNumber, String operationalRefrigerantName) {
		SealNumber seal = SealNumber.of(sealNumber);
		RefrigerantGas refrigerantGas = RefrigerantGas.of(operationalRefrigerantName);
		Cylinder cylinder = Cylinder.register(seal, refrigerantGas);

		if (!cylinderStore.addIfAbsent(cylinder)) {
			throw new CylinderAlreadyRegisteredException(seal);
		}

		return CylinderResult.from(cylinder);
	}

	public CylinderResult registerInitialGrossWeight(String sealNumber, BigDecimal initialGrossWeight) {
		SealNumber seal = SealNumber.of(sealNumber);
		Weight weight = Weight.of(initialGrossWeight);

		Cylinder updatedCylinder = cylinderStore.updateAtomically(
				seal,
				cylinder -> cylinder.registerInitialGrossWeight(weight))
				.orElseThrow(() -> new CylinderNotFoundException(seal));

		return CylinderResult.from(updatedCylinder);
	}
}
