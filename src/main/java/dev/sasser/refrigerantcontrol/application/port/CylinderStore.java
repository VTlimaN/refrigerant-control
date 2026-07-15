package dev.sasser.refrigerantcontrol.application.port;

import java.util.Optional;
import java.util.function.Consumer;

import dev.sasser.refrigerantcontrol.domain.Cylinder;
import dev.sasser.refrigerantcontrol.domain.SealNumber;

public interface CylinderStore {

	Optional<Cylinder> findBySealNumber(SealNumber sealNumber);

	boolean addIfAbsent(Cylinder cylinder);

	/**
	 * Applies synchronous domain work once to a detached cylinder and saves it only
	 * when the callback completes normally. The callback must not perform I/O,
	 * asynchronous work, or nested store calls.
	 */
	Optional<Cylinder> updateAtomically(SealNumber sealNumber, Consumer<Cylinder> change);
}
