package dev.sasser.refrigerantcontrol.application;

import dev.sasser.refrigerantcontrol.domain.SealNumber;

public final class CylinderNotFoundException extends RuntimeException {

	public CylinderNotFoundException(SealNumber sealNumber) {
		super("Cylinder was not found for seal number: " + sealNumber.value());
	}
}
