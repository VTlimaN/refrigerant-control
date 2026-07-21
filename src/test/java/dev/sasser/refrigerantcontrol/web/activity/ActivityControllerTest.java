package dev.sasser.refrigerantcontrol.web.activity;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import dev.sasser.refrigerantcontrol.application.CylinderUseCases;
import dev.sasser.refrigerantcontrol.application.UsageActivityResult;
import dev.sasser.refrigerantcontrol.application.UsageActivityUseCases;
import dev.sasser.refrigerantcontrol.application.port.UsageActivityStore;
import dev.sasser.refrigerantcontrol.domain.ActivityStatus;
import dev.sasser.refrigerantcontrol.domain.SealNumber;
import dev.sasser.refrigerantcontrol.domain.UsageActivity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.BindingResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ActivityControllerTest {

	private static final String SUCCESS_MESSAGE =
			"Atividade iniciada com sucesso. O cilindro está aguardando o peso de retorno.";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CylinderUseCases cylinderUseCases;

	@Autowired
	private UsageActivityUseCases usageActivityUseCases;

	@Autowired
	private UsageActivityStore usageActivityStore;

	@Test
	void shouldRenderCompleteActivityStartPageWithEmptyForm() throws Exception {
		mockMvc.perform(get("/activities/start"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(view().name("activity-start"))
				.andExpect(model().attributeExists("activityStartForm"))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("sealNumber", nullValue())))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("departureGrossWeight", nullValue())))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("activityLocation", nullValue())))
				.andExpect(content().string(containsString("<title>Iniciar atividade | Controle de Gases Refrigerantes</title>")))
				.andExpect(content().string(containsString("Operação de atividades")))
				.andExpect(content().string(containsString("<h1>Iniciar atividade</h1>")))
				.andExpect(content().string(containsString(
						"Informe o cilindro, o peso bruto de saída e o local para iniciar uma atividade.")))
				.andExpect(content().string(containsString(
						"Os dados cadastrados nesta etapa ficam apenas na memória e serão perdidos quando a aplicação for reiniciada.")))
				.andExpect(content().string(containsString("href=\"/\"")))
				.andExpect(content().string(containsString("Voltar ao início")))
				.andExpect(content().string(containsString("href=\"/cylinders\"")))
				.andExpect(content().string(containsString("Cadastrar cilindro")))
				.andExpect(content().string(containsString("<form method=\"post\" action=\"/activities/start\">")))
				.andExpect(content().string(containsString("for=\"activity-seal-number\">Número do lacre")))
				.andExpect(content().string(containsString("id=\"activity-seal-number\"")))
				.andExpect(content().string(containsString("autocomplete=\"off\"")))
				.andExpect(content().string(containsString(
						"for=\"departure-gross-weight\">Peso bruto de saída (kg)")))
				.andExpect(content().string(containsString("id=\"departure-gross-weight\"")))
				.andExpect(content().string(containsString("inputmode=\"decimal\"")))
				.andExpect(content().string(containsString("placeholder=\"15,140\"")))
				.andExpect(content().string(containsString("<span aria-hidden=\"true\">kg</span>")))
				.andExpect(content().string(containsString("for=\"activity-location\">Local da atividade")))
				.andExpect(content().string(containsString("id=\"activity-location\"")))
				.andExpect(content().string(not(containsString("name=\"operationalRefrigerantName\""))))
				.andExpect(content().string(not(containsString("name=\"returnGrossWeight\""))))
				.andExpect(content().string(not(containsString("name=\"consumedQuantity\""))))
				.andExpect(content().string(not(containsString("name=\"status\""))))
				.andExpect(content().string(not(containsString("type=\"date\""))))
				.andExpect(content().string(not(containsString("type=\"time\""))))
				.andExpect(content().string(not(containsString("type=\"datetime-local\""))))
				.andExpect(content().string(not(containsString("/activities/return"))))
				.andExpect(content().string(not(containsString("/activities/complete"))))
				.andExpect(content().string(not(containsString("/activities/history"))))
				.andExpect(content().string(not(containsString("/activities/list"))));
	}

	@Test
	void shouldPrefillExactSealFromQueryWithoutLookup() throws Exception {
		String sealNumber = "UNREGISTERED-Seal.Á-001";

		mockMvc.perform(get("/activities/start").queryParam("seal", sealNumber))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-start"))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("sealNumber", equalTo(sealNumber))));
	}

	@Test
	void shouldPrefillSealContainingSpacesSlashAndPlusExactly() throws Exception {
		String sealNumber = "  WEB SPECIAL / + Á 002  ";

		mockMvc.perform(get("/activities/start").queryParam("seal", sealNumber))
				.andExpect(status().isOk())
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("sealNumber", equalTo(sealNumber))));
	}

	@Test
	void shouldStartActivityPreserveStateAndApplyPostRedirectGet() throws Exception {
		String sealNumber = "  ACTIVITY SUCCESS / + 003  ";
		String activityLocation = "  Área técnica, Bloco A!  ";
		registerCylinderWithInitialWeight(sealNumber, "R407C");

		MvcResult start = mockMvc.perform(post("/activities/start")
					.param("sealNumber", sealNumber)
					.param("departureGrossWeight", "15,140")
					.param("activityLocation", activityLocation))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/activities/start"))
				.andExpect(flash().attribute("successMessage", SUCCESS_MESSAGE))
				.andExpect(flash().attribute("startedActivity", instanceOf(UsageActivityResult.class)))
				.andReturn();
		String redirectUrl = start.getResponse().getRedirectedUrl();
		MockHttpSession session = (MockHttpSession) start.getRequest().getSession(false);
		assertNotNull(session);

		List<UsageActivity> activities = activitiesFor(sealNumber);
		assertEquals(1, activities.size());
		UsageActivity storedActivity = activities.getFirst();
		assertEquals(sealNumber, storedActivity.cylinder().sealNumber().value());
		assertEquals("R407C", storedActivity.cylinder().refrigerantGas().operationalName());
		assertEquals(activityLocation, storedActivity.activityLocation());
		assertEquals(new BigDecimal("15.140"), storedActivity.departureGrossWeight().inKilograms());
		assertEquals(3, storedActivity.departureGrossWeight().inKilograms().scale());
		assertNotNull(storedActivity.startedAt());
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, storedActivity.status());
		assertTrue(storedActivity.returnGrossWeight().isEmpty());
		assertTrue(storedActivity.completedAt().isEmpty());
		assertTrue(storedActivity.consumedQuantity().isEmpty());

		mockMvc.perform(get(redirectUrl).session(session))
				.andExpect(status().isOk())
				.andExpect(model().attribute("successMessage", SUCCESS_MESSAGE))
				.andExpect(model().attribute("startedActivity", instanceOf(UsageActivityResult.class)))
				.andExpect(content().string(containsString("Atividade iniciada")))
				.andExpect(content().string(containsString(sealNumber)))
				.andExpect(content().string(containsString(activityLocation)))
				.andExpect(content().string(containsString("Número do lacre:")))
				.andExpect(content().string(containsString("Local da atividade:")))
				.andExpect(content().string(containsString("Situação:")))
				.andExpect(content().string(containsString("Aguardando peso de retorno")))
				.andExpect(content().string(not(containsString("AWAITING_RETURN_WEIGHT"))));
		assertEquals(1, activitiesFor(sealNumber).size());

		mockMvc.perform(get(redirectUrl).session(session))
				.andExpect(status().isOk())
				.andExpect(model().attributeDoesNotExist("successMessage", "startedActivity"))
				.andExpect(content().string(not(containsString("<h2 id=\"started-activity-title\">Atividade iniciada</h2>"))));

		assertEquals(1, activitiesFor(sealNumber).size());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	void shouldRejectBlankSealAndPreserveSubmittedValues(String sealNumber) throws Exception {
		String inspectionSeal = sealNumber.isEmpty()
				? "ACTIVITY-BLANK-SEAL-EMPTY-004"
				: "ACTIVITY-BLANK-SEAL-SPACES-005";

		mockMvc.perform(post("/activities/start")
					.param("sealNumber", sealNumber)
					.param("departureGrossWeight", "15,140")
					.param("activityLocation", "Casa de máquinas"))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-start"))
				.andExpect(model().attributeHasFieldErrors("activityStartForm", "sealNumber"))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("departureGrossWeight", equalTo("15,140"))))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("activityLocation", equalTo("Casa de máquinas"))))
				.andExpect(content().string(containsString("Informe o número do lacre.")));

		assertTrue(activitiesFor(inspectionSeal).isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	void shouldRejectBlankWeightAndPreserveSubmittedValues(String departureGrossWeight) throws Exception {
		String suffix = departureGrossWeight.isEmpty() ? "EMPTY-006" : "SPACES-007";
		String sealNumber = "ACTIVITY-BLANK-WEIGHT-" + suffix;
		registerCylinderWithInitialWeight(sealNumber, "R32");

		mockMvc.perform(post("/activities/start")
					.param("sealNumber", sealNumber)
					.param("departureGrossWeight", departureGrossWeight)
					.param("activityLocation", "Oficina técnica"))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-start"))
				.andExpect(model().attributeHasFieldErrors("activityStartForm", "departureGrossWeight"))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("departureGrossWeight", equalTo(departureGrossWeight))))
				.andExpect(content().string(containsString("Informe o peso bruto de saída.")));

		assertTrue(activitiesFor(sealNumber).isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	void shouldRejectBlankLocationAndPreserveSubmittedValues(String activityLocation) throws Exception {
		String suffix = activityLocation.isEmpty() ? "EMPTY-008" : "SPACES-009";
		String sealNumber = "ACTIVITY-BLANK-LOCATION-" + suffix;
		registerCylinderWithInitialWeight(sealNumber, "R134A");

		mockMvc.perform(post("/activities/start")
					.param("sealNumber", sealNumber)
					.param("departureGrossWeight", "15,140")
					.param("activityLocation", activityLocation))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-start"))
				.andExpect(model().attributeHasFieldErrors("activityStartForm", "activityLocation"))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("activityLocation", equalTo(activityLocation))))
				.andExpect(content().string(containsString("Informe o local da atividade.")));

		assertTrue(activitiesFor(sealNumber).isEmpty());
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"letters|ACTIVITY-MALFORMED-LETTERS-010",
			"1e3|ACTIVITY-MALFORMED-SCIENTIFIC-011",
			"1.234,56|ACTIVITY-MALFORMED-MIXED-DOT-COMMA-012",
			"1,234.56|ACTIVITY-MALFORMED-MIXED-COMMA-DOT-013",
			"15,1,4|ACTIVITY-MALFORMED-MULTIPLE-COMMA-014",
			"15.1.4|ACTIVITY-MALFORMED-MULTIPLE-DOT-015",
			"+15,140|ACTIVITY-MALFORMED-PLUS-016"
	})
	void shouldRejectMalformedWeightWithoutStartingActivity(String input, String sealNumber) throws Exception {
		String activityLocation = "  Local preservado, Área B.  ";
		registerCylinderWithInitialWeight(sealNumber, "R404");

		MvcResult result = mockMvc.perform(post("/activities/start")
					.param("sealNumber", sealNumber)
					.param("departureGrossWeight", input)
					.param("activityLocation", activityLocation))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-start"))
				.andExpect(model().attributeHasFieldErrors("activityStartForm", "departureGrossWeight"))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("departureGrossWeight", equalTo(input))))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("activityLocation", equalTo(activityLocation))))
				.andExpect(content().string(containsString(
						"Informe um peso válido usando vírgula ou ponto como separador decimal.")))
				.andReturn();

		assertFieldError(result, "departureGrossWeight", "weight.malformed");
		assertTrue(activitiesFor(sealNumber).isEmpty());
	}

	@Test
	void shouldRejectNegativeWeightAndAllowValidRetry() throws Exception {
		String sealNumber = "ACTIVITY-NEGATIVE-RETRY-017";
		String activityLocation = "  Sala técnica, nível -1.  ";
		registerCylinderWithInitialWeight(sealNumber, "R410A");

		MvcResult failure = mockMvc.perform(post("/activities/start")
					.param("sealNumber", sealNumber)
					.param("departureGrossWeight", "-15,140")
					.param("activityLocation", activityLocation))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-start"))
				.andExpect(model().attributeHasFieldErrors("activityStartForm", "departureGrossWeight"))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("departureGrossWeight", equalTo("-15,140"))))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("activityLocation", equalTo(activityLocation))))
				.andExpect(content().string(containsString("O peso bruto de saída não pode ser negativo.")))
				.andReturn();

		assertFieldError(failure, "departureGrossWeight", "weight.negative");
		assertTrue(activitiesFor(sealNumber).isEmpty());

		mockMvc.perform(post("/activities/start")
					.param("sealNumber", sealNumber)
					.param("departureGrossWeight", "15,140")
					.param("activityLocation", activityLocation))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/activities/start"))
				.andExpect(flash().attribute("successMessage", SUCCESS_MESSAGE));

		assertEquals(1, activitiesFor(sealNumber).size());
	}

	@Test
	void shouldRejectMissingCylinderWithFixedFieldError() throws Exception {
		String sealNumber = "ACTIVITY-MISSING-CYLINDER-018";
		String activityLocation = "  Laboratório, ala C.  ";

		MvcResult result = mockMvc.perform(post("/activities/start")
					.param("sealNumber", sealNumber)
					.param("departureGrossWeight", "15,140")
					.param("activityLocation", activityLocation))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-start"))
				.andExpect(model().attributeHasFieldErrors("activityStartForm", "sealNumber"))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("departureGrossWeight", equalTo("15,140"))))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("activityLocation", equalTo(activityLocation))))
				.andExpect(content().string(containsString("O cilindro informado não existe.")))
				.andExpect(content().string(not(containsString("CylinderNotFoundException"))))
				.andExpect(content().string(not(containsString("Cylinder was not found"))))
				.andReturn();

		assertFieldError(result, "sealNumber", "cylinder.notFound");
		assertTrue(activitiesFor(sealNumber).isEmpty());
	}

	@Test
	void shouldRejectCylinderWithoutInitialWeightWithFixedGlobalError() throws Exception {
		String sealNumber = "ACTIVITY-NO-INITIAL-WEIGHT-019";
		String activityLocation = "  Depósito, setor D.  ";
		cylinderUseCases.registerCylinder(sealNumber, "R-22");

		MvcResult result = mockMvc.perform(post("/activities/start")
					.param("sealNumber", sealNumber)
					.param("departureGrossWeight", "15,140")
					.param("activityLocation", activityLocation))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-start"))
				.andExpect(model().attributeHasErrors("activityStartForm"))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("departureGrossWeight", equalTo("15,140"))))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("activityLocation", equalTo(activityLocation))))
				.andExpect(content().string(containsString(
						"Registre o peso bruto inicial deste cilindro antes de iniciar a atividade.")))
				.andExpect(content().string(not(containsString("InitialGrossWeightNotRegisteredException"))))
				.andExpect(content().string(not(containsString("Initial gross weight is not registered"))))
				.andReturn();

		assertGlobalError(
				result,
				"activity.initialWeightMissing",
				"Registre o peso bruto inicial deste cilindro antes de iniciar a atividade.");
		assertTrue(activitiesFor(sealNumber).isEmpty());
	}

	@Test
	void shouldRejectSecondPendingActivityWithoutPartialState() throws Exception {
		String sealNumber = "ACTIVITY-PENDING-020";
		String originalLocation = "Primeira área";
		String attemptedLocation = "  Segunda área, bloco E.  ";
		registerCylinderWithInitialWeight(sealNumber, "141B");
		usageActivityUseCases.startUsageActivity(sealNumber, new BigDecimal("15.140"), originalLocation);

		MvcResult result = mockMvc.perform(post("/activities/start")
					.param("sealNumber", sealNumber)
					.param("departureGrossWeight", "14,900")
					.param("activityLocation", attemptedLocation))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-start"))
				.andExpect(model().attributeHasErrors("activityStartForm"))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("departureGrossWeight", equalTo("14,900"))))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("activityLocation", equalTo(attemptedLocation))))
				.andExpect(content().string(containsString(
						"Este cilindro já possui uma atividade aguardando o peso de retorno.")))
				.andExpect(content().string(not(containsString("PendingUsageActivityAlreadyExistsException"))))
				.andExpect(content().string(not(containsString("Pending usage activity already exists"))))
				.andReturn();

		assertGlobalError(
				result,
				"activity.pending",
				"Este cilindro já possui uma atividade aguardando o peso de retorno.");
		List<UsageActivity> activities = activitiesFor(sealNumber);
		assertEquals(1, activities.size());
		assertEquals(originalLocation, activities.getFirst().activityLocation());
		assertEquals(new BigDecimal("15.140"), activities.getFirst().departureGrossWeight().inKilograms());
	}

	@Test
	void shouldPreserveAndEscapeHtmlSignificantSealAndLocation() throws Exception {
		String sealNumber = "<script>alert('seal')</script>";
		String activityLocation = "  <img src=x onerror=alert('location')> Área & oficina  ";
		registerCylinderWithInitialWeight(sealNumber, "R32");

		MvcResult start = mockMvc.perform(post("/activities/start")
					.param("sealNumber", sealNumber)
					.param("departureGrossWeight", "15,140")
					.param("activityLocation", activityLocation))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/activities/start"))
				.andReturn();

		UsageActivity storedActivity = activitiesFor(sealNumber).getFirst();
		assertEquals(sealNumber, storedActivity.cylinder().sealNumber().value());
		assertEquals(activityLocation, storedActivity.activityLocation());

		mockMvc.perform(get("/activities/start").flashAttrs(start.getFlashMap()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("&lt;script&gt;alert(&#39;seal&#39;)&lt;/script&gt;")))
				.andExpect(content().string(containsString("&lt;img src=x onerror=alert(&#39;location&#39;)&gt;")))
				.andExpect(content().string(containsString("Área &amp; oficina")))
				.andExpect(content().string(not(containsString("<script>"))))
				.andExpect(content().string(not(containsString("<img"))))
				.andExpect(content().string(not(containsString("th:utext"))));
	}

	private void registerCylinderWithInitialWeight(String sealNumber, String operationalRefrigerantName) {
		cylinderUseCases.registerCylinder(sealNumber, operationalRefrigerantName);
		cylinderUseCases.registerInitialGrossWeight(sealNumber, new BigDecimal("18.500"));
	}

	private List<UsageActivity> activitiesFor(String sealNumber) {
		try {
			usageActivityStore.startAtomically(
					SealNumber.of(sealNumber),
					activities -> {
						throw new ActivityInspection(activities);
					});
		}
		catch (ActivityInspection inspection) {
			return inspection.activities();
		}
		throw new AssertionError("activity inspection callback did not run");
	}

	private void assertFieldError(MvcResult result, String field, String code) {
		BindingResult bindingResult = bindingResult(result);
		assertNotNull(bindingResult.getFieldError(field));
		assertEquals(code, bindingResult.getFieldError(field).getCode());
	}

	private void assertGlobalError(MvcResult result, String code, String message) {
		BindingResult bindingResult = bindingResult(result);
		assertEquals(1, bindingResult.getGlobalErrorCount());
		assertNotNull(bindingResult.getGlobalError());
		assertEquals(code, bindingResult.getGlobalError().getCode());
		assertEquals(message, bindingResult.getGlobalError().getDefaultMessage());
	}

	private BindingResult bindingResult(MvcResult result) {
		return (BindingResult) result.getModelAndView().getModel().get(
				BindingResult.MODEL_KEY_PREFIX + "activityStartForm");
	}

	private static final class ActivityInspection extends RuntimeException {

		private final List<UsageActivity> activities;

		private ActivityInspection(Collection<UsageActivity> activities) {
			super(null, null, false, false);
			this.activities = List.copyOf(activities);
		}

		private List<UsageActivity> activities() {
			return activities;
		}
	}
}
