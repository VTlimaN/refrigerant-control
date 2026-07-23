package dev.sasser.refrigerantcontrol.web.activity;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dev.sasser.refrigerantcontrol.application.CylinderUseCases;
import dev.sasser.refrigerantcontrol.application.UsageActivityResult;
import dev.sasser.refrigerantcontrol.application.UsageActivityUseCases;
import dev.sasser.refrigerantcontrol.domain.ActivityStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PendingActivityControllerTest {

	private static final Pattern RETURN_LINK =
			Pattern.compile("href=\"([^\"]*/activities/return\\?seal=[^\"]*)\"");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CylinderUseCases cylinderUseCases;

	@Autowired
	private UsageActivityUseCases usageActivityUseCases;

	@Test
	void shouldRenderAccessibleEmptyPendingActivitiesPage() throws Exception {
		MvcResult result = mockMvc.perform(get("/activities/pending"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(view().name("activity-pending"))
				.andExpect(model().attributeExists("pendingActivities"))
				.andExpect(content().string(containsString(
						"<title>Atividades pendentes | Controle de Gases Refrigerantes</title>")))
				.andExpect(content().string(containsString(
						"Consulte os cilindros que ainda aguardam o peso bruto de retorno.")))
				.andExpect(content().string(containsString(
						"Nenhuma atividade está aguardando o peso bruto de retorno.")))
				.andExpect(content().string(containsString("href=\"/\"")))
				.andExpect(content().string(containsString(
						"href=\"/activities/start\"")))
				.andReturn();

		Collection<?> pendingActivities = modelCollection(result);
		assertTrue(pendingActivities.isEmpty());
		assertThrows(UnsupportedOperationException.class, pendingActivities::clear);

		String html = result.getResponse().getContentAsString();
		assertEquals(1, occurrences(html, "<h1>"));
		assertTrue(html.contains("<h1>Atividades pendentes</h1>"));
		assertTrue(html.contains("<section class=\"form-card\">"));
		assertFalse(html.contains("Registrar retorno do cilindro"));
		assertFalse(html.contains("/activities/return?seal="));
		assertFalse(html.contains("aria-describedby"));
		assertTrue(usageActivityUseCases.listPendingUsageActivities().isEmpty());
	}

	@Test
	void shouldRenderAllAndOnlyPendingActivitiesWithExactValuesWithoutDependingOnOrder() throws Exception {
		String firstSeal = "  PENDENTE São Paulo / A&B  ";
		String secondSeal = "PENDENTE-002";
		String completedSeal = "CONCLUÍDA-003";
		String firstLocation = "  Oficina Norte, Área A!  ";
		String secondLocation = "Sala técnica / Bloco B";
		startPendingActivity(firstSeal, new BigDecimal("15.140"), firstLocation);
		startPendingActivity(secondSeal, new BigDecimal("9.50"), secondLocation);
		startPendingActivity(completedSeal, new BigDecimal("13.500"), "Área concluída");
		usageActivityUseCases.completePendingUsageActivity(
				completedSeal,
				new BigDecimal("12.000"),
				false);

		MvcResult result = mockMvc.perform(get("/activities/pending"))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-pending"))
				.andReturn();

		Collection<UsageActivityResult> pendingActivities = activityResults(result);
		assertEquals(Set.of(firstSeal, secondSeal), sealNumbers(pendingActivities));
		assertPendingResult(
				resultForSeal(pendingActivities, firstSeal),
				firstLocation,
				new BigDecimal("15.140"),
				3);
		assertPendingResult(
				resultForSeal(pendingActivities, secondSeal),
				secondLocation,
				new BigDecimal("9.50"),
				2);

		String html = result.getResponse().getContentAsString();
		assertTrue(html.contains("PENDENTE São Paulo / A&amp;B"));
		assertTrue(html.contains(secondSeal));
		assertTrue(html.contains(firstLocation));
		assertTrue(html.contains(secondLocation));
		assertTrue(html.contains("15,140"));
		assertTrue(html.contains("9,50"));
		assertFalse(html.contains(completedSeal));
		assertEquals(1, occurrences(html, "<ul class=\"form-grid activity-list\">"));
		assertEquals(2, occurrences(html, "<li class=\"form-card\">"));
		assertEquals(2, occurrences(html, "<article>"));
		assertEquals(2, occurrences(html, "/activities/return?seal="));
	}

	@Test
	void shouldEncodeReturnLinkAndPrefillExistingReturnFormWithExactSeal() throws Exception {
		String sealNumber = "  Lacre São José / A&B + #7";
		startPendingActivity(sealNumber, new BigDecimal("15.140"), "Sala de máquinas");

		MvcResult pendingPage = mockMvc.perform(get("/activities/pending"))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-pending"))
				.andReturn();

		String html = pendingPage.getResponse().getContentAsString();
		String returnUri = uniqueReturnUri(html);
		String upperCaseUri = returnUri.toUpperCase(Locale.ROOT);
		assertTrue(returnUri.startsWith("/activities/return?seal="));
		assertFalse(returnUri.contains(" "));
		assertFalse(returnUri.startsWith("//"));
		assertFalse(upperCaseUri.startsWith("JAVASCRIPT:"));
		assertTrue(upperCaseUri.contains("%26"));
		assertTrue(upperCaseUri.contains("%2B"));
		assertTrue(upperCaseUri.contains("%23"));
		assertTrue(html.contains("A&amp;B"));
		assertEquals(sealNumber, decodedSealParameter(returnUri));

		mockMvc.perform(get(URI.create(returnUri)))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attribute(
						"activityReturnForm",
						hasProperty("sealNumber", equalTo(sealNumber))));

		Collection<UsageActivityResult> pendingAfterFollow =
				usageActivityUseCases.listPendingUsageActivities();
		assertEquals(Set.of(sealNumber), sealNumbers(pendingAfterFollow));
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT,
				resultForSeal(pendingAfterFollow, sealNumber).status());
	}

	@Test
	void shouldEscapeDynamicContentAndRemainReadOnlyAcrossRepeatedRequests() throws Exception {
		String sealNumber = "<script>alert('seal')</script>";
		String activityLocation = "  <img src=x onerror=alert('location')> Área & oficina  ";
		startPendingActivity(sealNumber, new BigDecimal("11.230"), activityLocation);
		Collection<UsageActivityResult> before =
				usageActivityUseCases.listPendingUsageActivities();

		MvcResult firstPage = mockMvc.perform(get("/activities/pending"))
				.andExpect(status().isOk())
				.andReturn();
		MvcResult secondPage = mockMvc.perform(get("/activities/pending"))
				.andExpect(status().isOk())
				.andReturn();

		String firstHtml = firstPage.getResponse().getContentAsString();
		String secondHtml = secondPage.getResponse().getContentAsString();
		assertEquals(firstHtml, secondHtml);
		for (String html : List.of(firstHtml, secondHtml)) {
			assertTrue(html.contains("&lt;script&gt;alert(&#39;seal&#39;)&lt;/script&gt;"));
			assertTrue(html.contains(
					"&lt;img src=x onerror=alert(&#39;location&#39;)&gt; Área &amp; oficina"));
			assertFalse(html.contains("<script>alert('seal')</script>"));
			assertFalse(html.contains("<img src=x onerror=alert('location')>"));
			assertFalse(html.contains("th:utext"));
			assertTrue(uniqueReturnUri(html).startsWith("/activities/return?seal="));
		}

		Collection<UsageActivityResult> after =
				usageActivityUseCases.listPendingUsageActivities();
		assertEquals(sealNumbers(before), sealNumbers(after));
		assertPendingResult(
				resultForSeal(after, sealNumber),
				activityLocation,
				new BigDecimal("11.230"),
				3);
	}

	private void startPendingActivity(
			String sealNumber,
			BigDecimal departureGrossWeight,
			String activityLocation) {
		cylinderUseCases.registerCylinder(sealNumber, "R134A");
		cylinderUseCases.registerInitialGrossWeight(sealNumber, new BigDecimal("18.500"));
		usageActivityUseCases.startUsageActivity(
				sealNumber,
				departureGrossWeight,
				activityLocation);
	}

	private Collection<?> modelCollection(MvcResult result) {
		Object attribute = result.getModelAndView().getModel().get("pendingActivities");
		assertTrue(attribute instanceof Collection<?>);
		return (Collection<?>) attribute;
	}

	private Collection<UsageActivityResult> activityResults(MvcResult result) {
		return modelCollection(result).stream()
				.map(UsageActivityResult.class::cast)
				.toList();
	}

	private Set<String> sealNumbers(Collection<UsageActivityResult> activities) {
		return activities.stream()
				.map(UsageActivityResult::sealNumber)
				.collect(Collectors.toSet());
	}

	private UsageActivityResult resultForSeal(
			Collection<UsageActivityResult> activities,
			String sealNumber) {
		return activities.stream()
				.filter(activity -> activity.sealNumber().equals(sealNumber))
				.findFirst()
				.orElseThrow();
	}

	private void assertPendingResult(
			UsageActivityResult activity,
			String activityLocation,
			BigDecimal departureGrossWeight,
			int scale) {
		assertEquals(activityLocation, activity.activityLocation());
		assertEquals(departureGrossWeight, activity.departureGrossWeight());
		assertEquals(scale, activity.departureGrossWeight().scale());
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, activity.status());
		assertTrue(activity.returnGrossWeight().isEmpty());
		assertTrue(activity.completedAt().isEmpty());
		assertTrue(activity.consumedQuantity().isEmpty());
		assertFalse(activity.zeroConsumptionConfirmed());
	}

	private String uniqueReturnUri(String html) {
		Matcher matcher = RETURN_LINK.matcher(html);
		assertTrue(matcher.find());
		String returnUri = matcher.group(1).replace("&amp;", "&");
		assertFalse(matcher.find());
		return returnUri;
	}

	private String decodedSealParameter(String returnUri) {
		String rawQuery = URI.create(returnUri).getRawQuery();
		assertTrue(rawQuery.startsWith("seal="));
		return URLDecoder.decode(rawQuery.substring("seal=".length()), StandardCharsets.UTF_8);
	}

	private int occurrences(String text, String fragment) {
		int count = 0;
		int nextIndex = 0;
		while ((nextIndex = text.indexOf(fragment, nextIndex)) >= 0) {
			count++;
			nextIndex += fragment.length();
		}
		return count;
	}
}
