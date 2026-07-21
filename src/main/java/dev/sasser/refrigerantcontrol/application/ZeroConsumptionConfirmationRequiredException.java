package dev.sasser.refrigerantcontrol.application;

import dev.sasser.refrigerantcontrol.domain.SealNumber;

public final class ZeroConsumptionConfirmationRequiredException extends RuntimeException {

	public ZeroConsumptionConfirmationRequiredException(SealNumber sealNumber) {
		super("Zero consumption confirmation is required for seal number: " + sealNumber.value());
	}
}
