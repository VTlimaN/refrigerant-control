package dev.sasser.refrigerantcontrol.application;

import dev.sasser.refrigerantcontrol.domain.SealNumber;

public final class ReturnGrossWeightGreaterThanDepartureException extends RuntimeException {

	public ReturnGrossWeightGreaterThanDepartureException(SealNumber sealNumber) {
		super("Return gross weight is greater than departure gross weight for seal number: "
				+ sealNumber.value());
	}
}
