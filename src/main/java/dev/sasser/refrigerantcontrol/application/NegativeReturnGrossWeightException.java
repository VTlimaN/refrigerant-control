package dev.sasser.refrigerantcontrol.application;

import dev.sasser.refrigerantcontrol.domain.SealNumber;

public final class NegativeReturnGrossWeightException extends RuntimeException {

	public NegativeReturnGrossWeightException(SealNumber sealNumber) {
		super("Return gross weight must not be negative for seal number: " + sealNumber.value());
	}
}
