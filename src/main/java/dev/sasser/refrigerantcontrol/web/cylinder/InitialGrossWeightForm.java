package dev.sasser.refrigerantcontrol.web.cylinder;

import jakarta.validation.constraints.NotBlank;

public final class InitialGrossWeightForm {

	@NotBlank(message = "Informe o número do lacre.")
	private String sealNumber;

	@NotBlank(message = "Informe o peso bruto inicial.")
	private String initialGrossWeight;

	public InitialGrossWeightForm() {
	}

	public String getSealNumber() {
		return sealNumber;
	}

	public void setSealNumber(String sealNumber) {
		this.sealNumber = sealNumber;
	}

	public String getInitialGrossWeight() {
		return initialGrossWeight;
	}

	public void setInitialGrossWeight(String initialGrossWeight) {
		this.initialGrossWeight = initialGrossWeight;
	}
}
