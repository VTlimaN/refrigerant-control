package dev.sasser.refrigerantcontrol.application;

import dev.sasser.refrigerantcontrol.domain.SealNumber;

public final class PendingUsageActivityAlreadyExistsException extends RuntimeException {

	public PendingUsageActivityAlreadyExistsException(SealNumber sealNumber) {
		super("Pending usage activity already exists for seal number: " + sealNumber.value());
	}
}
