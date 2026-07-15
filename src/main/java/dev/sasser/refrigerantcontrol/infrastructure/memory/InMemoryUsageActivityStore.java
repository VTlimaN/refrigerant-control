package dev.sasser.refrigerantcontrol.infrastructure.memory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.sasser.refrigerantcontrol.application.port.UsageActivityStore;
import dev.sasser.refrigerantcontrol.domain.ActivityStatus;
import dev.sasser.refrigerantcontrol.domain.Cylinder;
import dev.sasser.refrigerantcontrol.domain.RefrigerantGas;
import dev.sasser.refrigerantcontrol.domain.SealNumber;
import dev.sasser.refrigerantcontrol.domain.UsageActivity;
import dev.sasser.refrigerantcontrol.domain.UsageActivityStarter;
import dev.sasser.refrigerantcontrol.domain.Weight;

public final class InMemoryUsageActivityStore implements UsageActivityStore {

	private final Object lock = new Object();
	private final Map<String, List<UsageActivitySnapshot>> activitiesBySealNumber = new HashMap<>();

	@Override
	public UsageActivity startAtomically(
			SealNumber sealNumber,
			Function<Collection<UsageActivity>, UsageActivity> startOperation) {
		SealNumber requiredSealNumber = Objects.requireNonNull(sealNumber, "seal number must not be null");
		Function<Collection<UsageActivity>, UsageActivity> requiredStartOperation = Objects.requireNonNull(
				startOperation,
				"start operation must not be null");
		synchronized (lock) {
			List<UsageActivitySnapshot> existingSnapshots = activitiesBySealNumber.getOrDefault(
					requiredSealNumber.value(),
					List.of());
			List<UsageActivity> relevantActivities = existingSnapshots.stream()
					.map(UsageActivitySnapshot::toActivity)
					.toList();

			UsageActivity startedActivity = Objects.requireNonNull(
					requiredStartOperation.apply(relevantActivities),
					"start operation must not return null");
			if (!startedActivity.cylinder().sealNumber().equals(requiredSealNumber)) {
				throw new IllegalArgumentException("started activity must belong to the requested seal number");
			}

			UsageActivitySnapshot startedSnapshot = UsageActivitySnapshot.from(startedActivity);
			List<UsageActivitySnapshot> updatedSnapshots = new ArrayList<>(existingSnapshots);
			updatedSnapshots.add(startedSnapshot);
			activitiesBySealNumber.put(requiredSealNumber.value(), List.copyOf(updatedSnapshots));
			return startedSnapshot.toActivity();
		}
	}

	@Override
	public Optional<UsageActivity> completePendingAtomically(
			SealNumber sealNumber,
			Consumer<UsageActivity> completion) {
		SealNumber requiredSealNumber = Objects.requireNonNull(sealNumber, "seal number must not be null");
		Consumer<UsageActivity> requiredCompletion = Objects.requireNonNull(
				completion,
				"completion must not be null");
		synchronized (lock) {
			List<UsageActivitySnapshot> existingSnapshots = activitiesBySealNumber.getOrDefault(
					requiredSealNumber.value(),
					List.of());
			int pendingIndex = findPendingIndex(existingSnapshots);
			if (pendingIndex < 0) {
				return Optional.empty();
			}

			UsageActivity detachedActivity = existingSnapshots.get(pendingIndex).toActivity();
			requiredCompletion.accept(detachedActivity);
			UsageActivitySnapshot completedSnapshot = UsageActivitySnapshot.from(detachedActivity);
			List<UsageActivitySnapshot> updatedSnapshots = new ArrayList<>(existingSnapshots);
			updatedSnapshots.set(pendingIndex, completedSnapshot);
			activitiesBySealNumber.put(requiredSealNumber.value(), List.copyOf(updatedSnapshots));
			return Optional.of(completedSnapshot.toActivity());
		}
	}

	private static int findPendingIndex(List<UsageActivitySnapshot> snapshots) {
		int pendingIndex = -1;
		for (int index = 0; index < snapshots.size(); index++) {
			if (snapshots.get(index).status() == ActivityStatus.AWAITING_RETURN_WEIGHT) {
				if (pendingIndex >= 0) {
					throw new IllegalStateException("multiple pending usage activities exist for the seal number");
				}
				pendingIndex = index;
			}
		}
		return pendingIndex;
	}

	private record UsageActivitySnapshot(
			String sealNumber,
			String operationalRefrigerantName,
			BigDecimal initialGrossWeight,
			BigDecimal departureGrossWeight,
			Instant startedAt,
			ActivityStatus status,
			Optional<BigDecimal> returnGrossWeight,
			Optional<Instant> completedAt) {

		private UsageActivitySnapshot {
			Objects.requireNonNull(sealNumber, "seal number must not be null");
			Objects.requireNonNull(
					operationalRefrigerantName,
					"operational refrigerant name must not be null");
			Objects.requireNonNull(initialGrossWeight, "initial gross weight must not be null");
			Objects.requireNonNull(departureGrossWeight, "departure gross weight must not be null");
			Objects.requireNonNull(startedAt, "started at must not be null");
			Objects.requireNonNull(status, "status must not be null");
			Objects.requireNonNull(returnGrossWeight, "return gross weight must not be null");
			Objects.requireNonNull(completedAt, "completed at must not be null");
			if (status == ActivityStatus.AWAITING_RETURN_WEIGHT
					&& (returnGrossWeight.isPresent() || completedAt.isPresent())) {
				throw new IllegalArgumentException("pending activity snapshot must not contain completion values");
			}
			if (status == ActivityStatus.COMPLETED
					&& (returnGrossWeight.isEmpty() || completedAt.isEmpty())) {
				throw new IllegalArgumentException("completed activity snapshot must contain completion values");
			}
		}

		private static UsageActivitySnapshot from(UsageActivity activity) {
			Weight initialGrossWeight = activity.cylinder().initialGrossWeight()
					.orElseThrow(() -> new IllegalStateException("activity cylinder must have an initial gross weight"));
			return new UsageActivitySnapshot(
					activity.cylinder().sealNumber().value(),
					activity.cylinder().refrigerantGas().operationalName(),
					initialGrossWeight.inKilograms(),
					activity.departureGrossWeight().inKilograms(),
					activity.startedAt(),
					activity.status(),
					activity.returnGrossWeight().map(Weight::inKilograms),
					activity.completedAt());
		}

		private UsageActivity toActivity() {
			Cylinder cylinder = Cylinder.register(
					SealNumber.of(sealNumber),
					RefrigerantGas.of(operationalRefrigerantName));
			cylinder.registerInitialGrossWeight(Weight.of(initialGrossWeight));
			UsageActivity activity = new UsageActivityStarter().start(
					cylinder,
					Weight.of(departureGrossWeight),
					startedAt,
					List.of());
			if (status == ActivityStatus.COMPLETED) {
				activity.complete(Weight.of(returnGrossWeight.orElseThrow()), completedAt.orElseThrow());
			}
			return activity;
		}
	}
}
