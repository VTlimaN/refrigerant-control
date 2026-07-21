package dev.sasser.refrigerantcontrol.web.activity;

import java.math.BigDecimal;
import java.util.Objects;

import dev.sasser.refrigerantcontrol.application.CylinderNotFoundException;
import dev.sasser.refrigerantcontrol.application.InitialGrossWeightNotRegisteredException;
import dev.sasser.refrigerantcontrol.application.PendingUsageActivityAlreadyExistsException;
import dev.sasser.refrigerantcontrol.application.UsageActivityResult;
import dev.sasser.refrigerantcontrol.application.UsageActivityUseCases;
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
@RequestMapping("/activities")
public final class ActivityController {

	private static final String ACTIVITY_START_VIEW = "activity-start";
	private static final String BINDING_FAILURE_MESSAGE = "Não foi possível processar os dados informados.";

	private final UsageActivityUseCases usageActivityUseCases;
	private final BrazilianDecimalParser decimalParser = new BrazilianDecimalParser();

	public ActivityController(UsageActivityUseCases usageActivityUseCases) {
		this.usageActivityUseCases = Objects.requireNonNull(
				usageActivityUseCases,
				"usage activity use cases must not be null");
	}

	@ModelAttribute("activityStartForm")
	public ActivityStartForm activityStartForm() {
		return new ActivityStartForm();
	}

	@GetMapping("/start")
	public String showStartPage(
			@RequestParam(name = "seal", required = false) String sealNumber,
			@ModelAttribute("activityStartForm") ActivityStartForm form) {
		if (sealNumber != null) {
			form.setSealNumber(sealNumber);
		}
		return ACTIVITY_START_VIEW;
	}

	@PostMapping("/start")
	public String startActivity(
			@Valid @ModelAttribute("activityStartForm") ActivityStartForm form,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (hasBindingErrors(bindingResult)) {
			return ACTIVITY_START_VIEW;
		}

		BigDecimal departureGrossWeight;
		try {
			departureGrossWeight = decimalParser.parse(form.getDepartureGrossWeight());
		}
		catch (IllegalArgumentException exception) {
			bindingResult.rejectValue(
					"departureGrossWeight",
					"weight.malformed",
					"Informe um peso válido usando vírgula ou ponto como separador decimal.");
			return ACTIVITY_START_VIEW;
		}

		UsageActivityResult startedActivity;
		try {
			startedActivity = usageActivityUseCases.startUsageActivity(
					form.getSealNumber(),
					departureGrossWeight,
					form.getActivityLocation());
		}
		catch (CylinderNotFoundException exception) {
			bindingResult.rejectValue(
					"sealNumber",
					"cylinder.notFound",
					"O cilindro informado não existe.");
			return ACTIVITY_START_VIEW;
		}
		catch (InitialGrossWeightNotRegisteredException exception) {
			bindingResult.reject(
					"activity.initialWeightMissing",
					"Registre o peso bruto inicial deste cilindro antes de iniciar a atividade.");
			return ACTIVITY_START_VIEW;
		}
		catch (PendingUsageActivityAlreadyExistsException exception) {
			bindingResult.reject(
					"activity.pending",
					"Este cilindro já possui uma atividade aguardando o peso de retorno.");
			return ACTIVITY_START_VIEW;
		}
		catch (IllegalArgumentException exception) {
			bindingResult.rejectValue(
					"departureGrossWeight",
					"weight.negative",
					"O peso bruto de saída não pode ser negativo.");
			return ACTIVITY_START_VIEW;
		}

		redirectAttributes.addFlashAttribute(
				"successMessage",
				"Atividade iniciada com sucesso. O cilindro está aguardando o peso de retorno.");

		redirectAttributes.addFlashAttribute(
				"startedActivity",
				startedActivity);

		return "redirect:/activities/start";
	}

	private boolean hasBindingErrors(BindingResult bindingResult) {
		if (bindingResult.getFieldErrors().stream().anyMatch(FieldError::isBindingFailure)) {
			bindingResult.reject("form.binding", BINDING_FAILURE_MESSAGE);
		}
		return bindingResult.hasErrors();
	}
}
