package dev.sasser.refrigerantcontrol.domain;

import java.util.Objects;
import java.util.Set;

public final class RefrigerantGas {

	private static final Set<String> CONFIRMED_OPERATIONAL_NAMES = Set.of(
			"R410A",
			"R32",
			"R-22",
			"R407C",
			"R134A",
			"R404",
			"141B");

	private final String operationalName;

	private RefrigerantGas(String operationalName) {
		this.operationalName = Objects.requireNonNull(operationalName, "operational name must not be null");
		if (!CONFIRMED_OPERATIONAL_NAMES.contains(operationalName)) {
			throw new IllegalArgumentException("unsupported operational refrigerant name");
		}
	}

	public static RefrigerantGas of(String operationalName) {
		return new RefrigerantGas(operationalName);
	}

	public String operationalName() {
		return operationalName;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RefrigerantGas refrigerantGas)) {
			return false;
		}
		return operationalName.equals(refrigerantGas.operationalName);
	}

	@Override
	public int hashCode() {
		return operationalName.hashCode();
	}
}
