package dev.sasser.refrigerantcontrol.web.cylinder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import dev.sasser.refrigerantcontrol.application.CylinderAlreadyRegisteredException;
import dev.sasser.refrigerantcontrol.application.CylinderNotFoundException;
import dev.sasser.refrigerantcontrol.application.CylinderResult;
import dev.sasser.refrigerantcontrol.application.CylinderUseCases;
import dev.sasser.refrigerantcontrol.domain.RefrigerantGas;
import dev.sasser.refrigerantcontrol.web.support.BrazilianDecimalParser;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cylinders")
public final class CylinderController {

	private static final String CYLINDERS_VIEW = "cylinders";
	private static final String REGISTRATION_SUCCESS_MESSAGE =
			"Cilindro cadastrado com sucesso. O lacre foi preenchido abaixo para registrar o peso inicial.";
	private static final String INITIAL_WEIGHT_SUCCESS_MESSAGE = "Peso bruto inicial registrado com sucesso.";
	private static final String BINDING_FAILURE_MESSAGE = "Não foi possível processar os dados informados.";

	private final CylinderUseCases cylinderUseCases;
	private final BrazilianDecimalParser decimalParser = new BrazilianDecimalParser();

	public CylinderController(CylinderUseCases cylinderUseCases) {
		this.cylinderUseCases = Objects.requireNonNull(cylinderUseCases, "cylinder use cases must not be null");
	}

	@ModelAttribute("cylinderRegistrationForm")
	public CylinderRegistrationForm cylinderRegistrationForm() {
		return new CylinderRegistrationForm();
	}

	@ModelAttribute("initialGrossWeightForm")
	public InitialGrossWeightForm initialGrossWeightForm() {
		return new InitialGrossWeightForm();
	}

	@ModelAttribute("operationalRefrigerantNames")
	public List<String> operationalRefrigerantNames() {
		return RefrigerantGas.supportedOperationalNames();
	}

	@GetMapping
	public String showRegistrationPage(
			@RequestParam(name = "seal", required = false) String sealNumber,
			@ModelAttribute("initialGrossWeightForm") InitialGrossWeightForm initialGrossWeightForm) {
		if (sealNumber != null) {
			initialGrossWeightForm.setSealNumber(sealNumber);
		}
		return CYLINDERS_VIEW;
	}

	@PostMapping
	public String registerCylinder(
			@Valid @ModelAttribute("cylinderRegistrationForm") CylinderRegistrationForm form,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (hasBindingErrors(bindingResult)) {
			return CYLINDERS_VIEW;
		}

		CylinderResult result;
		try {
			result = cylinderUseCases.registerCylinder(
					form.getSealNumber(),
					form.getOperationalRefrigerantName());
		}
		catch (CylinderAlreadyRegisteredException exception) {
			bindingResult.rejectValue(
					"sealNumber",
					"cylinder.duplicate",
					"Já existe um cilindro com este número de lacre.");
			return CYLINDERS_VIEW;
		}
		catch (IllegalArgumentException exception) {
			bindingResult.rejectValue(
					"operationalRefrigerantName",
					"refrigerant.unsupported",
					"Selecione um gás refrigerante válido.");
			return CYLINDERS_VIEW;
		}

		redirectAttributes.addFlashAttribute("successMessage", REGISTRATION_SUCCESS_MESSAGE);
		redirectAttributes.addAttribute("seal", result.sealNumber());
		return "redirect:/cylinders";
	}

	@PostMapping("/initial-weight")
	public String registerInitialGrossWeight(
			@Valid @ModelAttribute("initialGrossWeightForm") InitialGrossWeightForm form,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (hasBindingErrors(bindingResult)) {
			return CYLINDERS_VIEW;
		}

		BigDecimal initialGrossWeight;
		try {
			initialGrossWeight = decimalParser.parse(form.getInitialGrossWeight());
		}
		catch (IllegalArgumentException exception) {
			bindingResult.rejectValue(
					"initialGrossWeight",
					"weight.malformed",
					"Informe um peso válido usando vírgula ou ponto como separador decimal.");
			return CYLINDERS_VIEW;
		}

		CylinderResult result;
		try {
			result = cylinderUseCases.registerInitialGrossWeight(form.getSealNumber(), initialGrossWeight);
		}
		catch (CylinderNotFoundException exception) {
			bindingResult.rejectValue(
					"sealNumber",
					"cylinder.notFound",
					"O cilindro informado não existe.");
			return CYLINDERS_VIEW;
		}
		catch (IllegalArgumentException exception) {
			bindingResult.rejectValue(
					"initialGrossWeight",
					"weight.negative",
					"O peso não pode ser negativo.");
			return CYLINDERS_VIEW;
		}
		catch (IllegalStateException exception) {
			bindingResult.reject(
					"initialGrossWeight.alreadyRegistered",
					"O peso bruto inicial deste cilindro já foi registrado.");
			return CYLINDERS_VIEW;
		}

		redirectAttributes.addFlashAttribute("successMessage", INITIAL_WEIGHT_SUCCESS_MESSAGE);
		redirectAttributes.addAttribute("seal", result.sealNumber());
		return "redirect:/activities/start";
	}

	private boolean hasBindingErrors(BindingResult bindingResult) {
		if (bindingResult.getFieldErrors().stream().anyMatch(FieldError::isBindingFailure)) {
			bindingResult.reject("form.binding", BINDING_FAILURE_MESSAGE);
		}
		return bindingResult.hasErrors();
	}
}
