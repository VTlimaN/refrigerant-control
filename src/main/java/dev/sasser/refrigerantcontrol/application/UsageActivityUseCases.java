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

	public UsageActivityResult startUsageActivity(String sealNumber, BigDecimal departureGrossWeight) {
		SealNumber seal = SealNumber.of(sealNumber);
		Weight departureWeight = Weight.of(departureGrossWeight);
		Cylinder cylinder = cylinderStore.findBySealNumber(seal)
				.orElseThrow(() -> new CylinderNotFoundException(seal));

		UsageActivity activity = usageActivityStore.startAtomically(
				seal,
				relevantActivities -> usageActivityStarter.start(
						cylinder,
						departureWeight,
						clock.instant(),
						relevantActivities));

		return UsageActivityResult.from(activity);
	}

	public UsageActivityResult completePendingUsageActivity(String sealNumber, BigDecimal returnGrossWeight) {
		SealNumber seal = SealNumber.of(sealNumber);
		Weight returnWeight = Weight.of(returnGrossWeight);
		cylinderStore.findBySealNumber(seal)
				.orElseThrow(() -> new CylinderNotFoundException(seal));

		UsageActivity completedActivity = usageActivityStore.completePendingAtomically(
				seal,
				activity -> activity.complete(returnWeight, clock.instant()))
				.orElseThrow(() -> new PendingUsageActivityNotFoundException(seal));

		return UsageActivityResult.from(completedActivity);
	}
}
