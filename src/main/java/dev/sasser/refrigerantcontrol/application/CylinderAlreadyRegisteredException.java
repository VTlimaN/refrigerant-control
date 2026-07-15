package dev.sasser.refrigerantcontrol.application;

import dev.sasser.refrigerantcontrol.domain.SealNumber;

public final class CylinderAlreadyRegisteredException extends RuntimeException {

	public CylinderAlreadyRegisteredException(SealNumber sealNumber) {
		super("Cylinder is already registered for seal number: " + sealNumber.value());
	}
}
