package dev.sasser.refrigerantcontrol.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import dev.sasser.refrigerantcontrol.domain.ActivityStatus;
import dev.sasser.refrigerantcontrol.domain.UsageActivity;
import dev.sasser.refrigerantcontrol.domain.Weight;

public record UsageActivityResult(
		String sealNumber,
		BigDecimal departureGrossWeight,
		String activityLocation,
		Instant startedAt,
		ActivityStatus status,
		Optional<BigDecimal> returnGrossWeight,
		Optional<Instant> completedAt,
		Optional<BigDecimal> consumedQuantity,
		boolean zeroConsumptionConfirmed) {

	public UsageActivityResult {
		Objects.requireNonNull(sealNumber, "seal number must not be null");
		Objects.requireNonNull(departureGrossWeight, "departure gross weight must not be null");
		if (activityLocation == null || activityLocation.isBlank()) {
			throw new IllegalArgumentException("Activity location must not be blank");
		}
		Objects.requireNonNull(startedAt, "started at must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(returnGrossWeight, "return gross weight must not be null");
		Objects.requireNonNull(completedAt, "completed at must not be null");
		Objects.requireNonNull(consumedQuantity, "consumed quantity must not be null");
		boolean hasZeroConsumption = consumedQuantity
				.filter(consumption -> consumption.signum() == 0)
				.isPresent();
		if (zeroConsumptionConfirmed
				&& (status != ActivityStatus.COMPLETED || !hasZeroConsumption)) {
			throw new IllegalArgumentException("zero consumption confirmation requires completed zero consumption");
		}
		if (status == ActivityStatus.COMPLETED && hasZeroConsumption && !zeroConsumptionConfirmed) {
			throw new IllegalArgumentException("completed zero consumption must be confirmed");
		}
	}

	static UsageActivityResult from(UsageActivity activity) {
		UsageActivity requiredActivity = Objects.requireNonNull(activity, "usage activity must not be null");
		return new UsageActivityResult(
				requiredActivity.cylinder().sealNumber().value(),
				requiredActivity.departureGrossWeight().inKilograms(),
				requiredActivity.activityLocation(),
				requiredActivity.startedAt(),
				requiredActivity.status(),
				requiredActivity.returnGrossWeight().map(Weight::inKilograms),
				requiredActivity.completedAt(),
				requiredActivity.consumedQuantity().map(Weight::inKilograms),
				requiredActivity.zeroConsumptionConfirmed());
	}
}
