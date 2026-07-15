package dev.sasser.refrigerantcontrol.application;

import dev.sasser.refrigerantcontrol.domain.SealNumber;

public final class InitialGrossWeightNotRegisteredException extends RuntimeException {

	public InitialGrossWeightNotRegisteredException(SealNumber sealNumber) {
		super("Initial gross weight is not registered for seal number: " + sealNumber.value());
	}
}
