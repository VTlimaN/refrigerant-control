package dev.sasser.refrigerantcontrol.domain;

import java.util.List;
import java.util.Objects;

public final class RefrigerantGas {

	private static final List<String> SUPPORTED_OPERATIONAL_NAMES = List.of(
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
		if (!SUPPORTED_OPERATIONAL_NAMES.contains(operationalName)) {
			throw new IllegalArgumentException("unsupported operational refrigerant name");
		}
	}

	public static RefrigerantGas of(String operationalName) {
		return new RefrigerantGas(operationalName);
	}

	public static List<String> supportedOperationalNames() {
		return SUPPORTED_OPERATIONAL_NAMES;
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
