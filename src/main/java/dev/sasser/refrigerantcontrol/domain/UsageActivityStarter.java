package dev.sasser.refrigerantcontrol.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class UsageActivityStarter {

	public UsageActivityStarter() {
	}

	public UsageActivity start(
			Cylinder cylinder,
			Weight departureGrossWeight,
			Instant startedAt,
			Collection<UsageActivity> relevantActivities) {
		Cylinder requiredCylinder = Objects.requireNonNull(cylinder, "cylinder must not be null");
		Weight requiredDepartureWeight = Objects.requireNonNull(
				departureGrossWeight,
				"departure gross weight must not be null");
		Instant requiredStartedAt = Objects.requireNonNull(startedAt, "started at must not be null");
		List<UsageActivity> activities = List.copyOf(
				Objects.requireNonNull(relevantActivities, "relevant activities must not be null"));

		if (requiredCylinder.initialGrossWeight().isEmpty()) {
			throw new IllegalStateException(
					"cylinder must have an initial gross weight before starting an activity");
		}

		boolean hasPendingActivity = activities.stream()
				.anyMatch(activity -> activity.cylinder().equals(requiredCylinder)
						&& activity.isAwaitingReturnWeight());
		if (hasPendingActivity) {
			throw new IllegalStateException("cylinder already has an activity awaiting return weight");
		}

		return UsageActivity.start(requiredCylinder, requiredDepartureWeight, requiredStartedAt);
	}
}
