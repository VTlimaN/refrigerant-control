package dev.sasser.refrigerantcontrol.web.activity;

import java.math.BigDecimal;
import java.util.Objects;

import dev.sasser.refrigerantcontrol.application.CylinderNotFoundException;
import dev.sasser.refrigerantcontrol.application.NegativeReturnGrossWeightException;
import dev.sasser.refrigerantcontrol.application.PendingUsageActivityNotFoundException;
import dev.sasser.refrigerantcontrol.application.ReturnGrossWeightGreaterThanDepartureException;
import dev.sasser.refrigerantcontrol.application.UsageActivityResult;
import dev.sasser.refrigerantcontrol.application.UsageActivityUseCases;
import dev.sasser.refrigerantcontrol.application.ZeroConsumptionConfirmationRequiredException;
import dev.sasser.refrigerantcontrol.web.support.BrazilianDecimalParser;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
public final class ActivityReturnController {

	private static final String ACTIVITY_RETURN_VIEW = "activity-return";
	private static final String BINDING_FAILURE_MESSAGE = "Não foi possível processar os dados informados.";

	private final UsageActivityUseCases usageActivityUseCases;
	private final BrazilianDecimalParser decimalParser = new BrazilianDecimalParser();

	public ActivityReturnController(UsageActivityUseCases usageActivityUseCases) {
		this.usageActivityUseCases = Objects.requireNonNull(
				usageActivityUseCases,
				"usage activity use cases must not be null");
	}

	@ModelAttribute("activityReturnForm")
	public ActivityReturnForm activityReturnForm() {
		return new ActivityReturnForm();
	}

	@GetMapping("/return")
	public String showReturnPage(
			@RequestParam(name = "seal", required = false) String sealNumber,
			@ModelAttribute("activityReturnForm") ActivityReturnForm form) {
		if (sealNumber != null) {
			form.setSealNumber(sealNumber);
		}
		return ACTIVITY_RETURN_VIEW;
	}

	@PostMapping("/return")
	public String completeActivity(
			@Valid @ModelAttribute("activityReturnForm") ActivityReturnForm form,
			BindingResult bindingResult,
			@RequestParam(name = "confirmZeroConsumption", required = false) String confirmationRequest,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (hasBindingErrors(bindingResult)) {
			return ACTIVITY_RETURN_VIEW;
		}

		BigDecimal returnGrossWeight;
		try {
			returnGrossWeight = decimalParser.parse(form.getReturnGrossWeight());
		}
		catch (IllegalArgumentException exception) {
			bindingResult.rejectValue(
					"returnGrossWeight",
					"weight.malformed",
					"Informe um peso válido usando vírgula ou ponto como separador decimal.");
			return ACTIVITY_RETURN_VIEW;
		}

		boolean zeroConsumptionConfirmed = "true".equals(confirmationRequest);
		UsageActivityResult completedActivity;
		try {
			completedActivity = usageActivityUseCases.completePendingUsageActivity(
					form.getSealNumber(),
					returnGrossWeight,
					zeroConsumptionConfirmed);
		}
		catch (NegativeReturnGrossWeightException exception) {
			bindingResult.rejectValue(
					"returnGrossWeight",
					"weight.negative",
					"O peso bruto de retorno não pode ser negativo.");
			return ACTIVITY_RETURN_VIEW;
		}
		catch (CylinderNotFoundException exception) {
			bindingResult.rejectValue(
					"sealNumber",
					"cylinder.notFound",
					"O cilindro informado não existe.");
			return ACTIVITY_RETURN_VIEW;
		}
		catch (PendingUsageActivityNotFoundException exception) {
			bindingResult.reject(
					"activity.pendingNotFound",
					"Este cilindro não possui uma atividade aguardando o peso de retorno.");
			return ACTIVITY_RETURN_VIEW;
		}
		catch (ReturnGrossWeightGreaterThanDepartureException exception) {
			bindingResult.rejectValue(
					"returnGrossWeight",
					"weight.greaterThanDeparture",
					"O peso bruto de retorno não pode ser maior que o peso bruto de saída.");
			return ACTIVITY_RETURN_VIEW;
		}
		catch (ZeroConsumptionConfirmationRequiredException exception) {
			model.addAttribute("zeroConsumptionConfirmationRequired", true);
			return ACTIVITY_RETURN_VIEW;
		}

		redirectAttributes.addFlashAttribute(
				"successMessage",
				"Atividade concluída com sucesso. O consumo foi calculado.");
		redirectAttributes.addFlashAttribute("completedActivity", completedActivity);
		return "redirect:/activities/return";
	}

	private boolean hasBindingErrors(BindingResult bindingResult) {
		if (bindingResult.getFieldErrors().stream().anyMatch(FieldError::isBindingFailure)) {
			bindingResult.reject("form.binding", BINDING_FAILURE_MESSAGE);
		}
		return bindingResult.hasErrors();
	}
}
