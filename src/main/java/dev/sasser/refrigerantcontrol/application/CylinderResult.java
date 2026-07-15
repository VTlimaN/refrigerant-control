package dev.sasser.refrigerantcontrol.application;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import dev.sasser.refrigerantcontrol.domain.Cylinder;
import dev.sasser.refrigerantcontrol.domain.Weight;

public record CylinderResult(
		String sealNumber,
		String operationalRefrigerantName,
		Optional<BigDecimal> initialGrossWeight) {

	public CylinderResult {
		Objects.requireNonNull(sealNumber, "seal number must not be null");
		Objects.requireNonNull(operationalRefrigerantName, "operational refrigerant name must not be null");
		Objects.requireNonNull(initialGrossWeight, "initial gross weight must not be null");
	}

	static CylinderResult from(Cylinder cylinder) {
		Cylinder requiredCylinder = Objects.requireNonNull(cylinder, "cylinder must not be null");
		return new CylinderResult(
				requiredCylinder.sealNumber().value(),
				requiredCylinder.refrigerantGas().operationalName(),
				requiredCylinder.initialGrossWeight().map(Weight::inKilograms));
	}
}
