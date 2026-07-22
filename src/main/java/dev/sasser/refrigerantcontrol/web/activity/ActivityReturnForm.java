package dev.sasser.refrigerantcontrol.web.activity;

import jakarta.validation.constraints.NotBlank;

public final class ActivityReturnForm {

	@NotBlank(message = "Informe o número do lacre.")
	private String sealNumber;

	@NotBlank(message = "Informe o peso bruto de retorno.")
	private String returnGrossWeight;

	public ActivityReturnForm() {
	}

	public String getSealNumber() {
		return sealNumber;
	}

	public void setSealNumber(String sealNumber) {
		this.sealNumber = sealNumber;
	}

	public String getReturnGrossWeight() {
		return returnGrossWeight;
	}

	public void setReturnGrossWeight(String returnGrossWeight) {
		this.returnGrossWeight = returnGrossWeight;
	}
}
