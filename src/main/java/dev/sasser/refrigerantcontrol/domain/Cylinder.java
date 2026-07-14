package dev.sasser.refrigerantcontrol.domain;

import java.util.Objects;
import java.util.Optional;

public final class Cylinder {

	private final SealNumber sealNumber;
	private final RefrigerantGas refrigerantGas;
	private Weight initialGrossWeight;

	private Cylinder(SealNumber sealNumber, RefrigerantGas refrigerantGas) {
		this.sealNumber = Objects.requireNonNull(sealNumber, "seal number must not be null");
		this.refrigerantGas = Objects.requireNonNull(refrigerantGas, "refrigerant gas must not be null");
	}

	public static Cylinder register(SealNumber sealNumber, RefrigerantGas refrigerantGas) {
		return new Cylinder(sealNumber, refrigerantGas);
	}

	public SealNumber sealNumber() {
		return sealNumber;
	}

	public RefrigerantGas refrigerantGas() {
		return refrigerantGas;
	}

	public Optional<Weight> initialGrossWeight() {
		return Optional.ofNullable(initialGrossWeight);
	}

	public void registerInitialGrossWeight(Weight initialGrossWeight) {
		Weight requiredWeight = Objects.requireNonNull(initialGrossWeight, "initial gross weight must not be null");
		if (this.initialGrossWeight != null) {
			throw new IllegalStateException("initial gross weight is already registered");
		}
		this.initialGrossWeight = requiredWeight;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Cylinder cylinder)) {
			return false;
		}
		return sealNumber.equals(cylinder.sealNumber);
	}

	@Override
	public int hashCode() {
		return sealNumber.hashCode();
	}
}
