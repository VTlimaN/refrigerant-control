package dev.sasser.refrigerantcontrol.web.cylinder;

import jakarta.validation.constraints.NotBlank;

public final class CylinderRegistrationForm {

	@NotBlank(message = "Informe o número do lacre.")
	private String sealNumber;

	@NotBlank(message = "Selecione o gás refrigerante.")
	private String operationalRefrigerantName;

	public CylinderRegistrationForm() {
	}

	public String getSealNumber() {
		return sealNumber;
	}

	public void setSealNumber(String sealNumber) {
		this.sealNumber = sealNumber;
	}

	public String getOperationalRefrigerantName() {
		return operationalRefrigerantName;
	}

	public void setOperationalRefrigerantName(String operationalRefrigerantName) {
		this.operationalRefrigerantName = operationalRefrigerantName;
	}
}
