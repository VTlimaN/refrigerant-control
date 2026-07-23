package dev.sasser.refrigerantcontrol.web.activity;

import java.util.Objects;

import dev.sasser.refrigerantcontrol.application.UsageActivityUseCases;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/activities")
public final class PendingActivityController {

	private final UsageActivityUseCases usageActivityUseCases;

	public PendingActivityController(UsageActivityUseCases usageActivityUseCases) {
		this.usageActivityUseCases = Objects.requireNonNull(
				usageActivityUseCases,
				"usage activity use cases must not be null");
	}

	@GetMapping("/pending")
	public String showPendingActivities(Model model) {
		model.addAttribute(
				"pendingActivities",
				usageActivityUseCases.listPendingUsageActivities());
		return "activity-pending";
	}
}
