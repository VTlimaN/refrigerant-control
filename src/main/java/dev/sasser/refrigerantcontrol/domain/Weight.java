package dev.sasser.refrigerantcontrol.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class Weight implements Comparable<Weight> {

	private final BigDecimal kilograms;

	private Weight(BigDecimal kilograms) {
		this.kilograms = Objects.requireNonNull(kilograms, "kilograms must not be null");
		if (kilograms.signum() < 0) {
			throw new IllegalArgumentException("weight must not be negative");
		}
	}

	public static Weight of(BigDecimal kilograms) {
		return new Weight(kilograms);
	}

	public BigDecimal inKilograms() {
		return kilograms;
	}

	public Weight subtract(Weight other) {
		Objects.requireNonNull(other, "other weight must not be null");
		BigDecimal result = kilograms.subtract(other.kilograms);
		if (result.signum() < 0) {
			throw new IllegalArgumentException("weight subtraction must not produce a negative result");
		}
		return new Weight(result);
	}

	@Override
	public int compareTo(Weight other) {
		Objects.requireNonNull(other, "other weight must not be null");
		return kilograms.compareTo(other.kilograms);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Weight weight)) {
			return false;
		}
		return kilograms.compareTo(weight.kilograms) == 0;
	}

	@Override
	public int hashCode() {
		return normalizedForHash().hashCode();
	}

	private BigDecimal normalizedForHash() {
		return kilograms.signum() == 0 ? BigDecimal.ZERO : kilograms.stripTrailingZeros();
	}
}
