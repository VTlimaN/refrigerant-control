package dev.sasser.refrigerantcontrol.infrastructure.memory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import dev.sasser.refrigerantcontrol.application.port.CylinderStore;
import dev.sasser.refrigerantcontrol.domain.Cylinder;
import dev.sasser.refrigerantcontrol.domain.RefrigerantGas;
import dev.sasser.refrigerantcontrol.domain.SealNumber;
import dev.sasser.refrigerantcontrol.domain.Weight;

public final class InMemoryCylinderStore implements CylinderStore {

	private final Object lock = new Object();
	private final Map<String, CylinderSnapshot> cylindersBySealNumber = new HashMap<>();

	@Override
	public Optional<Cylinder> findBySealNumber(SealNumber sealNumber) {
		SealNumber requiredSealNumber = Objects.requireNonNull(sealNumber, "seal number must not be null");
		synchronized (lock) {
			CylinderSnapshot snapshot = cylindersBySealNumber.get(requiredSealNumber.value());
			return snapshot == null ? Optional.empty() : Optional.of(snapshot.toCylinder());
		}
	}

	@Override
	public boolean addIfAbsent(Cylinder cylinder) {
		Cylinder requiredCylinder = Objects.requireNonNull(cylinder, "cylinder must not be null");
		synchronized (lock) {
			String sealNumber = requiredCylinder.sealNumber().value();
			if (cylindersBySealNumber.containsKey(sealNumber)) {
				return false;
			}
			cylindersBySealNumber.put(sealNumber, CylinderSnapshot.from(requiredCylinder));
			return true;
		}
	}

	@Override
	public Optional<Cylinder> updateAtomically(SealNumber sealNumber, Consumer<Cylinder> change) {
		SealNumber requiredSealNumber = Objects.requireNonNull(sealNumber, "seal number must not be null");
		Consumer<Cylinder> requiredChange = Objects.requireNonNull(change, "change must not be null");
		synchronized (lock) {
			CylinderSnapshot existingSnapshot = cylindersBySealNumber.get(requiredSealNumber.value());
			if (existingSnapshot == null) {
				return Optional.empty();
			}

			Cylinder detachedCylinder = existingSnapshot.toCylinder();
			requiredChange.accept(detachedCylinder);
			CylinderSnapshot updatedSnapshot = CylinderSnapshot.from(detachedCylinder);
			cylindersBySealNumber.put(requiredSealNumber.value(), updatedSnapshot);
			return Optional.of(updatedSnapshot.toCylinder());
		}
	}

	private record CylinderSnapshot(
			String sealNumber,
			String operationalRefrigerantName,
			Optional<BigDecimal> initialGrossWeight) {

		private CylinderSnapshot {
			Objects.requireNonNull(sealNumber, "seal number must not be null");
			Objects.requireNonNull(
					operationalRefrigerantName,
					"operational refrigerant name must not be null");
			Objects.requireNonNull(initialGrossWeight, "initial gross weight must not be null");
		}

		private static CylinderSnapshot from(Cylinder cylinder) {
			return new CylinderSnapshot(
					cylinder.sealNumber().value(),
					cylinder.refrigerantGas().operationalName(),
					cylinder.initialGrossWeight().map(Weight::inKilograms));
		}

		private Cylinder toCylinder() {
			Cylinder cylinder = Cylinder.register(
					SealNumber.of(sealNumber),
					RefrigerantGas.of(operationalRefrigerantName));
			initialGrossWeight.ifPresent(weight -> cylinder.registerInitialGrossWeight(Weight.of(weight)));
			return cylinder;
		}
	}
}
