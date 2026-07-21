package dev.sasser.refrigerantcontrol.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class UsageActivity {

	private final Cylinder cylinder;
	private final Weight departureGrossWeight;
	private final String activityLocation;
	private final Instant startedAt;
	private ActivityStatus status;
	private Weight returnGrossWeight;
	private Instant completedAt;
	private boolean zeroConsumptionConfirmed;

	private UsageActivity(
			Cylinder cylinder,
			Weight departureGrossWeight,
			String activityLocation,
			Instant startedAt) {
		this.cylinder = Objects.requireNonNull(cylinder, "cylinder must not be null");
		this.departureGrossWeight = Objects.requireNonNull(
				departureGrossWeight,
				"departure gross weight must not be null");
		if (activityLocation == null || activityLocation.isBlank()) {
			throw new IllegalArgumentException("activity location must not be blank");
		}
		this.activityLocation = activityLocation;
		this.startedAt = Objects.requireNonNull(startedAt, "started at must not be null");
		this.status = ActivityStatus.AWAITING_RETURN_WEIGHT;
	}

	static UsageActivity start(
			Cylinder cylinder,
			Weight departureGrossWeight,
			String activityLocation,
			Instant startedAt) {
		return new UsageActivity(cylinder, departureGrossWeight, activityLocation, startedAt);
	}

	public Cylinder cylinder() {
		return cylinder;
	}

	public Weight departureGrossWeight() {
		return departureGrossWeight;
	}

	public String activityLocation() {
		return activityLocation;
	}

	public Instant startedAt() {
		return startedAt;
	}

	public ActivityStatus status() {
		return status;
	}

	public Optional<Weight> returnGrossWeight() {
		return Optional.ofNullable(returnGrossWeight);
	}

	public Optional<Instant> completedAt() {
		return Optional.ofNullable(completedAt);
	}

	public Optional<Weight> consumedQuantity() {
		if (status != ActivityStatus.COMPLETED) {
			return Optional.empty();
		}
		return Optional.of(departureGrossWeight.subtract(returnGrossWeight));
	}

	public boolean zeroConsumptionConfirmed() {
		return zeroConsumptionConfirmed;
	}

	public boolean isAwaitingReturnWeight() {
		return status == ActivityStatus.AWAITING_RETURN_WEIGHT;
	}

	public void complete(
			Weight returnGrossWeight,
			Instant completedAt,
			boolean zeroConsumptionConfirmed) {
		Weight requiredReturnWeight = Objects.requireNonNull(
				returnGrossWeight,
				"return gross weight must not be null");
		Instant requiredCompletedAt = Objects.requireNonNull(completedAt, "completed at must not be null");

		if (status != ActivityStatus.AWAITING_RETURN_WEIGHT) {
			throw new IllegalStateException("activity is already completed");
		}
		int weightComparison = requiredReturnWeight.compareTo(departureGrossWeight);
		if (weightComparison > 0) {
			throw new IllegalArgumentException("return gross weight must not be greater than departure gross weight");
		}
		if (requiredCompletedAt.isBefore(startedAt)) {
			throw new IllegalArgumentException("completed at must not be before started at");
		}
		if (weightComparison == 0 && !zeroConsumptionConfirmed) {
			throw new IllegalStateException("zero consumption must be confirmed");
		}

		this.returnGrossWeight = requiredReturnWeight;
		this.completedAt = requiredCompletedAt;
		this.zeroConsumptionConfirmed = weightComparison == 0;
		this.status = ActivityStatus.COMPLETED;
	}
}
