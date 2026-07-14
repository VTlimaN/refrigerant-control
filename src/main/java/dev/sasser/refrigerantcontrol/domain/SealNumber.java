package dev.sasser.refrigerantcontrol.domain;

import java.util.Objects;

public final class SealNumber {

	private final String value;

	private SealNumber(String value) {
		this.value = Objects.requireNonNull(value, "seal number must not be null");
		if (value.isBlank()) {
			throw new IllegalArgumentException("seal number must not be blank");
		}
	}

	public static SealNumber of(String value) {
		return new SealNumber(value);
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SealNumber sealNumber)) {
			return false;
		}
		return value.equals(sealNumber.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
