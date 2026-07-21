package dev.sasser.refrigerantcontrol.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;

import dev.sasser.refrigerantcontrol.application.port.CylinderStore;
import dev.sasser.refrigerantcontrol.application.port.UsageActivityStore;
import dev.sasser.refrigerantcontrol.domain.Cylinder;
import dev.sasser.refrigerantcontrol.domain.SealNumber;
import dev.sasser.refrigerantcontrol.domain.UsageActivity;
import dev.sasser.refrigerantcontrol.domain.UsageActivityStarter;
import dev.sasser.refrigerantcontrol.domain.Weight;

public final class UsageActivityUseCases {

	private final CylinderStore cylinderStore;
	private final UsageActivityStore usageActivityStore;
	private final Clock clock;
	private final UsageActivityStarter usageActivityStarter;

	public UsageActivityUseCases(
			CylinderStore cylinderStore,
			UsageActivityStore usageActivityStore,
			Clock clock) {
		this.cylinderStore = Objects.requireNonNull(cylinderStore, "cylinder store must not be null");
		this.usageActivityStore = Objects.requireNonNull(
				usageActivityStore,
				"usage activity store must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.usageActivityStarter = new UsageActivityStarter();
	}

	public UsageActivityResult startUsageActivity(
			String sealNumber,
			BigDecimal departureGrossWeight,
			String activityLocation) {
		SealNumber seal = SealNumber.of(sealNumber);
		Weight departureWeight = Weight.of(departureGrossWeight);
		Cylinder cylinder = cylinderStore.findBySealNumber(seal)
				.orElseThrow(() -> new CylinderNotFoundException(seal));
		if (cylinder.initialGrossWeight().isEmpty()) {
			throw new InitialGrossWeightNotRegisteredException(seal);
		}

		UsageActivity activity = usageActivityStore.startAtomically(
				seal,
				relevantActivities -> {
					if (relevantActivities.stream().anyMatch(UsageActivity::isAwaitingReturnWeight)) {
						throw new PendingUsageActivityAlreadyExistsException(seal);
					}
					return usageActivityStarter.start(
							cylinder,
							departureWeight,
							activityLocation,
							clock.instant(),
							relevantActivities);
				});

		return UsageActivityResult.from(activity);
	}

	public UsageActivityResult completePendingUsageActivity(
			String sealNumber,
			BigDecimal returnGrossWeight,
			boolean zeroConsumptionConfirmed) {
		SealNumber seal = SealNumber.of(sealNumber);
		Weight returnWeight;
		try {
			returnWeight = Weight.of(returnGrossWeight);
		}
		catch (IllegalArgumentException exception) {
			throw new NegativeReturnGrossWeightException(seal);
		}
		cylinderStore.findBySealNumber(seal)
				.orElseThrow(() -> new CylinderNotFoundException(seal));

		UsageActivity completedActivity = usageActivityStore.completePendingAtomically(
				seal,
				activity -> {
					int weightComparison = returnWeight.compareTo(activity.departureGrossWeight());
					if (weightComparison > 0) {
						throw new ReturnGrossWeightGreaterThanDepartureException(seal);
					}
					if (weightComparison == 0 && !zeroConsumptionConfirmed) {
						throw new ZeroConsumptionConfirmationRequiredException(seal);
					}
					activity.complete(returnWeight, clock.instant(), zeroConsumptionConfirmed);
				})
				.orElseThrow(() -> new PendingUsageActivityNotFoundException(seal));

		return UsageActivityResult.from(completedActivity);
	}
}
