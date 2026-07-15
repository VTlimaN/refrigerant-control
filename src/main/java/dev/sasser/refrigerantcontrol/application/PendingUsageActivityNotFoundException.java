package dev.sasser.refrigerantcontrol.application;

import dev.sasser.refrigerantcontrol.domain.SealNumber;

public final class PendingUsageActivityNotFoundException extends RuntimeException {

	public PendingUsageActivityNotFoundException(SealNumber sealNumber) {
		super("Pending usage activity was not found for seal number: " + sealNumber.value());
	}
}
