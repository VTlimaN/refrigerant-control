package dev.sasser.refrigerantcontrol.web.activity;

import jakarta.validation.constraints.NotBlank;

public final class ActivityStartForm {

	@NotBlank(message = "Informe o número do lacre.")
	private String sealNumber;

	@NotBlank(message = "Informe o peso bruto de saída.")
	private String departureGrossWeight;

	@NotBlank(message = "Informe o local da atividade.")
	private String activityLocation;

	public ActivityStartForm() {
	}

	public String getSealNumber() {
		return sealNumber;
	}

	public void setSealNumber(String sealNumber) {
		this.sealNumber = sealNumber;
	}

	public String getDepartureGrossWeight() {
		return departureGrossWeight;
	}

	public void setDepartureGrossWeight(String departureGrossWeight) {
		this.departureGrossWeight = departureGrossWeight;
	}

	public String getActivityLocation() {
		return activityLocation;
	}

	public void setActivityLocation(String activityLocation) {
		this.activityLocation = activityLocation;
	}
}
