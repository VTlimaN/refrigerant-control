package dev.sasser.refrigerantcontrol.web.activity;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

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
class ActivityReturnControllerTest {

	private static final String SUCCESS_MESSAGE =
			"Atividade concluída com sucesso. O consumo foi calculado.";
	private static final String NO_PENDING_MESSAGE =
			"Este cilindro não possui uma atividade aguardando o peso de retorno.";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CylinderUseCases cylinderUseCases;

	@Autowired
	private UsageActivityUseCases usageActivityUseCases;

	@Autowired
	private UsageActivityStore usageActivityStore;

	@Test
	void shouldRenderCompleteActivityReturnPageWithEmptyForm() throws Exception {
		MvcResult page = mockMvc.perform(get("/activities/return"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(view().name("activity-return"))
				.andExpect(model().attributeExists("activityReturnForm"))
				.andExpect(model().attribute(
						"activityReturnForm",
						hasProperty("sealNumber", nullValue())))
				.andExpect(model().attribute(
						"activityReturnForm",
						hasProperty("returnGrossWeight", nullValue())))
				.andExpect(content().string(containsString(
						"<title>Registrar retorno | Controle de Gases Refrigerantes</title>")))
				.andExpect(content().string(containsString("Operação de atividades")))
				.andExpect(content().string(containsString("<h1>Registrar retorno</h1>")))
				.andExpect(content().string(containsString(
						"Informe o cilindro e o peso bruto de retorno para concluir a atividade pendente.")))
				.andExpect(content().string(containsString(
						"Os dados cadastrados nesta etapa ficam apenas na memória e serão perdidos quando a aplicação for reiniciada.")))
				.andExpect(content().string(containsString("href=\"/\"")))
				.andExpect(content().string(containsString("Voltar ao início")))
				.andExpect(content().string(containsString("href=\"/cylinders\"")))
				.andExpect(content().string(containsString("Cadastrar cilindro")))
				.andExpect(content().string(containsString("href=\"/activities/start\"")))
				.andExpect(content().string(containsString("Iniciar atividade")))
				.andExpect(content().string(containsString(
						"<form method=\"post\" action=\"/activities/return\">")))
				.andExpect(content().string(containsString("for=\"return-seal-number\">Número do lacre")))
				.andExpect(content().string(containsString("id=\"return-seal-number\"")))
				.andExpect(content().string(containsString("autocomplete=\"off\"")))
				.andExpect(content().string(containsString(
						"for=\"return-gross-weight\">Peso bruto de retorno (kg)")))
				.andExpect(content().string(containsString("id=\"return-gross-weight\"")))
				.andExpect(content().string(containsString("inputmode=\"decimal\"")))
				.andExpect(content().string(containsString("placeholder=\"15,140\"")))
				.andExpect(content().string(containsString("<span aria-hidden=\"true\">kg</span>")))
				.andExpect(content().string(containsString("<button type=\"submit\">Concluir atividade</button>")))
				.andExpect(content().string(not(containsString("name=\"confirmZeroConsumption\""))))
				.andExpect(content().string(not(containsString("name=\"departureGrossWeight\""))))
				.andExpect(content().string(not(containsString("name=\"activityLocation\""))))
				.andExpect(content().string(not(containsString("name=\"operationalRefrigerantName\""))))
				.andExpect(content().string(not(containsString("name=\"status\""))))
				.andExpect(content().string(not(containsString("type=\"date\""))))
				.andExpect(content().string(not(containsString("type=\"time\""))))
				.andExpect(content().string(not(containsString("type=\"datetime-local\""))))
				.andReturn();

		RenderedHtml html = RenderedHtml.parse(page);
		RenderedElement sealInput = html.uniqueElementWithId("return-seal-number");
		assertFalse(sealInput.hasAttribute("aria-describedby"));
		assertFalse("true".equals(sealInput.attribute("aria-invalid")));
		assertEquals(0, html.elementsWithId("sealNumber-error").size());

		RenderedElement returnWeightInput = html.uniqueElementWithId("return-gross-weight");
		assertEquals("return-gross-weight-hint", returnWeightInput.attribute("aria-describedby"));
		assertFalse("true".equals(returnWeightInput.attribute("aria-invalid")));
		assertEquals(1, html.elementsWithId("return-gross-weight-hint").size());
		assertEquals(0, html.elementsWithId("returnGrossWeight-error").size());
		html.assertEveryAriaDescriptionResolves();
		html.assertIdsAreUnique();
	}

	@Test
	void shouldPrefillExactOrdinarySealFromQueryWithoutLookup() throws Exception {
		String sealNumber = "UNREGISTERED-Return.Á-001";

		mockMvc.perform(get("/activities/return").queryParam("seal", sealNumber))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attribute(
						"activityReturnForm",
						hasProperty("sealNumber", equalTo(sealNumber))));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"  ABC-123/+  ",
			"Lacre São José #7",
			"<script>alert('query')</script>"
	})
	void shouldPrefillExactFreeFormSealFromDecodedQuery(String sealNumber) throws Exception {
		mockMvc.perform(get("/activities/return").queryParam("seal", sealNumber))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attribute(
						"activityReturnForm",
						hasProperty("sealNumber", equalTo(sealNumber))));
	}

	@Test
	void shouldCompleteNonzeroActivityPreserveStateAndApplyPostRedirectGet() throws Exception {
		String sealNumber = "  RETURN SUCCESS / + 002  ";
		String activityLocation = "  Área técnica, Bloco A!  ";
		startPendingActivity(sealNumber, "R407C", new BigDecimal("15.140"), activityLocation);

		MvcResult completion = mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "12,10"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/activities/return"))
				.andExpect(flash().attribute("successMessage", SUCCESS_MESSAGE))
				.andExpect(flash().attribute("completedActivity", instanceOf(UsageActivityResult.class)))
				.andReturn();
		String redirectUrl = completion.getResponse().getRedirectedUrl();
		MockHttpSession session = (MockHttpSession) completion.getRequest().getSession(false);
		assertNotNull(session);

		UsageActivity storedActivity = onlyActivityFor(sealNumber);
		assertCompletedNonzero(storedActivity, sealNumber, activityLocation, "R407C");

		mockMvc.perform(get(redirectUrl).session(session))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attribute("successMessage", SUCCESS_MESSAGE))
				.andExpect(model().attribute("completedActivity", instanceOf(UsageActivityResult.class)))
				.andExpect(content().string(containsString("Atividade concluída")))
				.andExpect(content().string(containsString(sealNumber)))
				.andExpect(content().string(containsString(activityLocation)))
				.andExpect(content().string(containsString("Peso bruto de saída (kg):")))
				.andExpect(content().string(containsString("15,140")))
				.andExpect(content().string(containsString("Peso bruto de retorno (kg):")))
				.andExpect(content().string(containsString("12,10")))
				.andExpect(content().string(containsString("Consumo (kg):")))
				.andExpect(content().string(containsString("3,040")))
				.andExpect(content().string(containsString("Situação:</strong> Atividade concluída")))
				.andExpect(content().string(not(containsString("COMPLETED"))))
				.andExpect(content().string(not(containsString("Optional["))))
				.andExpect(content().string(not(containsString("Optional.empty"))));
		assertEquals(1, activitiesFor(sealNumber).size());

		mockMvc.perform(get(redirectUrl).session(session))
				.andExpect(status().isOk())
				.andExpect(model().attributeDoesNotExist("successMessage", "completedActivity"))
				.andExpect(content().string(not(containsString(
						"<h2 id=\"completed-activity-title\">Atividade concluída</h2>"))));

		MvcResult duplicate = mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "12,10"))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attributeHasErrors("activityReturnForm"))
				.andExpect(content().string(containsString(NO_PENDING_MESSAGE)))
				.andExpect(content().string(not(containsString("PendingUsageActivityNotFoundException"))))
				.andReturn();
		assertGlobalError(duplicate, "activity.pendingNotFound", NO_PENDING_MESSAGE);
		assertEquals(1, activitiesFor(sealNumber).size());
		assertCompletedNonzero(onlyActivityFor(sealNumber), sealNumber, activityLocation, "R407C");
	}

	@Test
	void shouldRequireExplicitZeroConsumptionConfirmationAndCompleteWithPrg() throws Exception {
		String sealNumber = "ZERO CONFIRM / + 003";
		String activityLocation = "  Oficina zero, Área B.  ";
		startPendingActivity(sealNumber, "R32", new BigDecimal("15.140"), activityLocation);

		assertZeroConfirmationRequired(sealNumber, "15,14", null);
		assertPending(onlyActivityFor(sealNumber), sealNumber, activityLocation);

		for (String invalidConfirmation : List.of("", "TRUE", "1")) {
			assertZeroConfirmationRequired(sealNumber, "15,14", invalidConfirmation);
			assertPending(onlyActivityFor(sealNumber), sealNumber, activityLocation);
		}

		MvcResult confirmation = mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "15,14")
					.param("confirmZeroConsumption", "true"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/activities/return"))
				.andExpect(flash().attribute("successMessage", SUCCESS_MESSAGE))
				.andExpect(flash().attribute("completedActivity", instanceOf(UsageActivityResult.class)))
				.andReturn();
		MockHttpSession session = (MockHttpSession) confirmation.getRequest().getSession(false);
		assertNotNull(session);

		UsageActivity completed = onlyActivityFor(sealNumber);
		assertEquals(ActivityStatus.COMPLETED, completed.status());
		assertEquals(new BigDecimal("15.140"), completed.departureGrossWeight().inKilograms());
		assertEquals(3, completed.departureGrossWeight().inKilograms().scale());
		assertEquals(new BigDecimal("15.14"), completed.returnGrossWeight().orElseThrow().inKilograms());
		assertEquals(2, completed.returnGrossWeight().orElseThrow().inKilograms().scale());
		assertEquals(new BigDecimal("0.000"), completed.consumedQuantity().orElseThrow().inKilograms());
		assertEquals(3, completed.consumedQuantity().orElseThrow().inKilograms().scale());
		assertTrue(completed.zeroConsumptionConfirmed());
		assertTrue(completed.completedAt().isPresent());
		assertEquals(1, activitiesFor(sealNumber).size());

		String redirectUrl = confirmation.getResponse().getRedirectedUrl();
		mockMvc.perform(get(redirectUrl).session(session))
				.andExpect(status().isOk())
				.andExpect(model().attribute("successMessage", SUCCESS_MESSAGE))
				.andExpect(model().attribute("completedActivity", instanceOf(UsageActivityResult.class)))
				.andExpect(content().string(containsString("15,140")))
				.andExpect(content().string(containsString("15,14")))
				.andExpect(content().string(containsString("0,000")))
				.andExpect(content().string(not(containsString("COMPLETED"))));

		mockMvc.perform(get(redirectUrl).session(session))
				.andExpect(status().isOk())
				.andExpect(model().attributeDoesNotExist("successMessage", "completedActivity"))
				.andExpect(content().string(not(containsString(
						"<h2 id=\"completed-activity-title\">Atividade concluída</h2>"))));
		assertEquals(1, activitiesFor(sealNumber).size());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	void shouldRejectBlankSealAndPreserveSubmittedValues(String sealNumber) throws Exception {
		String suffix = sealNumber.isEmpty() ? "EMPTY-017" : "SPACES-018";
		String pendingSealNumber = "RETURN-BLANK-SEAL-" + suffix;
		String location = "Sala de máquinas";
		startPendingActivity(pendingSealNumber, "R134A", new BigDecimal("15.140"), location);

		MvcResult failure = mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "12,100"))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attributeHasFieldErrors("activityReturnForm", "sealNumber"))
				.andExpect(model().attribute(
						"activityReturnForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(model().attribute(
						"activityReturnForm",
						hasProperty("returnGrossWeight", equalTo("12,100"))))
				.andExpect(content().string(containsString("Informe o número do lacre.")))
				.andReturn();
		assertFieldErrorAria(
				failure,
				"return-seal-number",
				"sealNumber-error",
				"sealNumber-error",
				"Informe o número do lacre.");
		assertPending(onlyActivityFor(pendingSealNumber), pendingSealNumber, location);

		mockMvc.perform(post("/activities/return")
					.param("sealNumber", pendingSealNumber)
					.param("returnGrossWeight", "12,100"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/activities/return"));
		assertEquals(ActivityStatus.COMPLETED, onlyActivityFor(pendingSealNumber).status());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	void shouldRejectBlankReturnWeightAndPreservePendingActivity(String returnGrossWeight) throws Exception {
		String suffix = returnGrossWeight.isEmpty() ? "EMPTY-004" : "SPACES-005";
		String sealNumber = "RETURN-BLANK-WEIGHT-" + suffix;
		String location = "Sala de máquinas";
		startPendingActivity(sealNumber, "R134A", new BigDecimal("15.140"), location);

		MvcResult failure = mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", returnGrossWeight))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attributeHasFieldErrors("activityReturnForm", "returnGrossWeight"))
				.andExpect(model().attribute(
						"activityReturnForm",
						hasProperty("returnGrossWeight", equalTo(returnGrossWeight))))
				.andExpect(content().string(containsString("Informe o peso bruto de retorno.")))
				.andReturn();
		assertFieldErrorAria(
				failure,
				"return-gross-weight",
				"return-gross-weight-hint returnGrossWeight-error",
				"returnGrossWeight-error",
				"Informe o peso bruto de retorno.");
		assertPending(onlyActivityFor(sealNumber), sealNumber, location);

		mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "12,100"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/activities/return"));
		assertEquals(ActivityStatus.COMPLETED, onlyActivityFor(sealNumber).status());
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"letters|RETURN-MALFORMED-LETTERS-006",
			"1e3|RETURN-MALFORMED-SCIENTIFIC-007",
			"1.234,56|RETURN-MALFORMED-MIXED-DOT-COMMA-008",
			"1,234.56|RETURN-MALFORMED-MIXED-COMMA-DOT-009",
			"12,1,0|RETURN-MALFORMED-MULTIPLE-COMMA-010",
			"12.1.0|RETURN-MALFORMED-MULTIPLE-DOT-011",
			"+12,100|RETURN-MALFORMED-PLUS-012"
	})
	void shouldRejectMalformedReturnWeightAndAllowValidRetry(String input, String sealNumber) throws Exception {
		String location = "  Local preservado, Área C.  ";
		startPendingActivity(sealNumber, "R404", new BigDecimal("15.140"), location);

		MvcResult failure = mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", input))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attributeHasFieldErrors("activityReturnForm", "returnGrossWeight"))
				.andExpect(model().attribute(
						"activityReturnForm",
						hasProperty("returnGrossWeight", equalTo(input))))
				.andExpect(content().string(containsString(
						"Informe um peso válido usando vírgula ou ponto como separador decimal.")))
				.andReturn();
		assertFieldError(failure, "returnGrossWeight", "weight.malformed");
		assertPending(onlyActivityFor(sealNumber), sealNumber, location);

		mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "12,100"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/activities/return"));
		assertEquals(ActivityStatus.COMPLETED, onlyActivityFor(sealNumber).status());
	}

	@Test
	void shouldRejectNegativeReturnWeightAndAllowValidRetry() throws Exception {
		String sealNumber = "RETURN-NEGATIVE-RETRY-013";
		String location = "  Sala técnica, nível -1.  ";
		startPendingActivity(sealNumber, "R410A", new BigDecimal("15.140"), location);

		MvcResult failure = mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "-12,100"))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attributeHasFieldErrors("activityReturnForm", "returnGrossWeight"))
				.andExpect(content().string(containsString(
						"O peso bruto de retorno não pode ser negativo.")))
				.andReturn();
		assertFieldError(failure, "returnGrossWeight", "weight.negative");
		assertPending(onlyActivityFor(sealNumber), sealNumber, location);

		mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "12,100"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/activities/return"));
		assertEquals(ActivityStatus.COMPLETED, onlyActivityFor(sealNumber).status());
	}

	@Test
	void shouldRejectMissingCylinderWithFixedFieldError() throws Exception {
		String sealNumber = "RETURN-MISSING-CYLINDER-014";

		MvcResult result = mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "12,100"))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attributeHasFieldErrors("activityReturnForm", "sealNumber"))
				.andExpect(content().string(containsString("O cilindro informado não existe.")))
				.andExpect(content().string(not(containsString("CylinderNotFoundException"))))
				.andExpect(content().string(not(containsString("Cylinder was not found"))))
				.andReturn();
		assertFieldError(result, "sealNumber", "cylinder.notFound");
	}

	@Test
	void shouldRejectCylinderWithoutPendingActivityWithFixedGlobalError() throws Exception {
		String sealNumber = "RETURN-NO-PENDING-015";
		registerCylinderWithInitialWeight(sealNumber, "R-22");

		MvcResult result = mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "12,100"))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attributeHasErrors("activityReturnForm"))
				.andExpect(content().string(containsString(NO_PENDING_MESSAGE)))
				.andExpect(content().string(not(containsString("PendingUsageActivityNotFoundException"))))
				.andExpect(content().string(not(containsString("Pending usage activity was not found"))))
				.andReturn();
		assertGlobalError(result, "activity.pendingNotFound", NO_PENDING_MESSAGE);
		assertTrue(activitiesFor(sealNumber).isEmpty());
	}

	@Test
	void shouldRejectReturnGreaterThanDepartureAndAllowValidRetry() throws Exception {
		String sealNumber = "RETURN-GREATER-RETRY-016";
		String location = "  Depósito, setor D.  ";
		startPendingActivity(sealNumber, "141B", new BigDecimal("15.140"), location);

		MvcResult failure = mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "15,141"))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attributeHasFieldErrors("activityReturnForm", "returnGrossWeight"))
				.andExpect(content().string(containsString(
						"O peso bruto de retorno não pode ser maior que o peso bruto de saída.")))
				.andExpect(content().string(not(containsString(
						"ReturnGrossWeightGreaterThanDepartureException"))))
				.andReturn();
		assertFieldError(failure, "returnGrossWeight", "weight.greaterThanDeparture");
		assertPending(onlyActivityFor(sealNumber), sealNumber, location);

		mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "12,100"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/activities/return"));
		assertEquals(ActivityStatus.COMPLETED, onlyActivityFor(sealNumber).status());
	}

	@Test
	void shouldPreserveAndEscapeHtmlSignificantSealAndStoredLocation() throws Exception {
		String sealNumber = "<script>alert('seal')</script>";
		String activityLocation = "  <img src=x onerror=alert('location')> Área & oficina  ";
		startPendingActivity(sealNumber, "R32", new BigDecimal("15.140"), activityLocation);

		MvcResult completion = mockMvc.perform(post("/activities/return")
					.param("sealNumber", sealNumber)
					.param("returnGrossWeight", "12,100"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/activities/return"))
				.andReturn();
		MockHttpSession session = (MockHttpSession) completion.getRequest().getSession(false);
		assertNotNull(session);

		UsageActivity storedActivity = onlyActivityFor(sealNumber);
		assertEquals(sealNumber, storedActivity.cylinder().sealNumber().value());
		assertEquals(activityLocation, storedActivity.activityLocation());

		mockMvc.perform(get(completion.getResponse().getRedirectedUrl()).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(
						"&lt;script&gt;alert(&#39;seal&#39;)&lt;/script&gt;")))
				.andExpect(content().string(containsString(
						"&lt;img src=x onerror=alert(&#39;location&#39;)&gt;")))
				.andExpect(content().string(containsString("Área &amp; oficina")))
				.andExpect(content().string(containsString("12,100")))
				.andExpect(content().string(containsString("3,040")))
				.andExpect(content().string(not(containsString("<script>"))))
				.andExpect(content().string(not(containsString("<img"))))
				.andExpect(content().string(not(containsString("th:utext"))));
	}

	private void assertZeroConfirmationRequired(
			String sealNumber,
			String returnGrossWeight,
			String confirmationRequest) throws Exception {
		var request = post("/activities/return")
				.param("sealNumber", sealNumber)
				.param("returnGrossWeight", returnGrossWeight);
		if (confirmationRequest != null) {
			request.param("confirmZeroConsumption", confirmationRequest);
		}

		MvcResult warning = mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(view().name("activity-return"))
				.andExpect(model().attribute("zeroConsumptionConfirmationRequired", true))
				.andExpect(model().attribute(
						"activityReturnForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(model().attribute(
						"activityReturnForm",
						hasProperty("returnGrossWeight", equalTo(returnGrossWeight))))
				.andExpect(content().string(containsString("O consumo calculado é zero. Deseja continuar?")))
				.andExpect(content().string(containsString(
						"name=\"confirmZeroConsumption\" value=\"true\"")))
				.andExpect(content().string(containsString("Confirmar consumo zero e concluir")))
				.andReturn();
		RenderedHtml.parse(warning).assertSafeZeroConfirmationForm();
	}

	private static void assertFieldErrorAria(
			MvcResult result,
			String inputId,
			String expectedDescription,
			String errorId,
			String expectedMessage) throws IOException {
		RenderedHtml html = RenderedHtml.parse(result);
		RenderedElement input = html.uniqueElementWithId(inputId);
		assertEquals(expectedDescription, input.attribute("aria-describedby"));
		assertEquals("true", input.attribute("aria-invalid"));
		RenderedElement error = html.uniqueElementWithId(errorId);
		assertEquals(expectedMessage, error.normalizedText());
		html.assertEveryAriaDescriptionResolves();
		html.assertIdsAreUnique();
	}

	private void registerCylinderWithInitialWeight(String sealNumber, String operationalRefrigerantName) {
		cylinderUseCases.registerCylinder(sealNumber, operationalRefrigerantName);
		cylinderUseCases.registerInitialGrossWeight(sealNumber, new BigDecimal("18.500"));
	}

	private void startPendingActivity(
			String sealNumber,
			String operationalRefrigerantName,
			BigDecimal departureGrossWeight,
			String activityLocation) {
		registerCylinderWithInitialWeight(sealNumber, operationalRefrigerantName);
		usageActivityUseCases.startUsageActivity(sealNumber, departureGrossWeight, activityLocation);
	}

	private UsageActivity onlyActivityFor(String sealNumber) {
		List<UsageActivity> activities = activitiesFor(sealNumber);
		assertEquals(1, activities.size());
		return activities.getFirst();
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

	private void assertPending(UsageActivity activity, String sealNumber, String activityLocation) {
		assertEquals(sealNumber, activity.cylinder().sealNumber().value());
		assertEquals(activityLocation, activity.activityLocation());
		assertEquals(ActivityStatus.AWAITING_RETURN_WEIGHT, activity.status());
		assertTrue(activity.returnGrossWeight().isEmpty());
		assertTrue(activity.completedAt().isEmpty());
		assertTrue(activity.consumedQuantity().isEmpty());
		assertFalse(activity.zeroConsumptionConfirmed());
	}

	private void assertCompletedNonzero(
			UsageActivity activity,
			String sealNumber,
			String activityLocation,
			String operationalRefrigerantName) {
		assertEquals(sealNumber, activity.cylinder().sealNumber().value());
		assertEquals(operationalRefrigerantName, activity.cylinder().refrigerantGas().operationalName());
		assertEquals(activityLocation, activity.activityLocation());
		assertEquals(ActivityStatus.COMPLETED, activity.status());
		assertEquals(new BigDecimal("15.140"), activity.departureGrossWeight().inKilograms());
		assertEquals(3, activity.departureGrossWeight().inKilograms().scale());
		assertEquals(new BigDecimal("12.10"), activity.returnGrossWeight().orElseThrow().inKilograms());
		assertEquals(2, activity.returnGrossWeight().orElseThrow().inKilograms().scale());
		assertEquals(new BigDecimal("3.040"), activity.consumedQuantity().orElseThrow().inKilograms());
		assertEquals(3, activity.consumedQuantity().orElseThrow().inKilograms().scale());
		assertTrue(activity.completedAt().isPresent());
		assertFalse(activity.zeroConsumptionConfirmed());
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
				BindingResult.MODEL_KEY_PREFIX + "activityReturnForm");
	}

	private static final class RenderedHtml extends HTMLEditorKit.ParserCallback {

		private static final String RETURN_FORM_ACTION = "/activities/return";

		private final List<RenderedElement> elements = new ArrayList<>();
		private final Deque<RenderedElement> openElements = new ArrayDeque<>();

		private static RenderedHtml parse(MvcResult result) throws IOException {
			RenderedHtml html = new RenderedHtml();
			new ParserDelegator().parse(
					new StringReader(result.getResponse().getContentAsString()),
					html,
					true);
			return html;
		}

		@Override
		public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
			RenderedElement element = addElement(tag, attributes);
			openElements.push(element);
		}

		@Override
		public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
			if (tag.toString().equals("button")) {
				if (attributes.isDefined(HTML.Attribute.ENDTAG)) {
					closeElement(tag);
				}
				else {
					openElements.push(addElement(tag, attributes));
				}
				return;
			}
			addElement(tag, attributes);
		}

		@Override
		public void handleEndTag(HTML.Tag tag, int position) {
			closeElement(tag);
		}

		private void closeElement(HTML.Tag tag) {
			while (!openElements.isEmpty()) {
				RenderedElement element = openElements.pop();
				if (element.tag().equals(tag.toString())) {
					return;
				}
			}
		}

		@Override
		public void handleText(char[] data, int position) {
			openElements.forEach(element -> element.appendText(data));
		}

		private RenderedElement addElement(HTML.Tag tag, MutableAttributeSet sourceAttributes) {
			Map<String, String> attributes = attributesOf(sourceAttributes);
			String parentFormAction = tag == HTML.Tag.FORM
					? attributes.get("action")
					: openElements.stream()
							.filter(element -> element.tag().equals(HTML.Tag.FORM.toString()))
							.map(element -> element.attribute("action"))
							.findFirst()
							.orElse(null);
			RenderedElement element = new RenderedElement(tag.toString(), attributes, parentFormAction);
			elements.add(element);
			return element;
		}

		private static Map<String, String> attributesOf(MutableAttributeSet sourceAttributes) {
			Map<String, String> attributes = new LinkedHashMap<>();
			Enumeration<?> names = sourceAttributes.getAttributeNames();
			while (names.hasMoreElements()) {
				Object name = names.nextElement();
				Object value = sourceAttributes.getAttribute(name);
				attributes.put(name.toString(), value == null ? "" : value.toString());
			}
			return Map.copyOf(attributes);
		}

		private List<RenderedElement> elementsWithId(String id) {
			return elements.stream()
					.filter(element -> id.equals(element.attribute("id")))
					.toList();
		}

		private RenderedElement uniqueElementWithId(String id) {
			List<RenderedElement> matches = elementsWithId(id);
			assertEquals(1, matches.size(), () -> "expected one rendered element with id=" + id);
			return matches.getFirst();
		}

		private void assertEveryAriaDescriptionResolves() {
			for (RenderedElement element : elements) {
				String description = element.attribute("aria-describedby");
				if (description == null) {
					continue;
				}
				assertFalse(description.isBlank());
				String[] ids = description.trim().split("\\s+");
				assertEquals(ids.length, Set.of(ids).size());
				for (String id : ids) {
					assertFalse(id.isBlank());
					assertEquals(1, elementsWithId(id).size(),
							() -> "unresolved or duplicate aria-describedby id=" + id);
				}
			}
		}

		private void assertIdsAreUnique() {
			Map<String, Integer> idCounts = new LinkedHashMap<>();
			for (RenderedElement element : elements) {
				String id = element.attribute("id");
				if (id != null) {
					idCounts.merge(id, 1, Integer::sum);
				}
			}
			idCounts.forEach((id, count) -> assertEquals(1, count, () -> "duplicate rendered id=" + id));
		}

		private void assertSafeZeroConfirmationForm() {
			List<RenderedElement> forms = elements.stream()
					.filter(element -> element.tag().equals(HTML.Tag.FORM.toString()))
					.toList();
			assertEquals(1, forms.size());
			assertEquals(RETURN_FORM_ACTION, forms.getFirst().attribute("action"));
			assertEquals("post", forms.getFirst().attribute("method"));

			List<RenderedElement> submitButtons = elements.stream()
					.filter(element -> element.tag().equals("button"))
					.filter(element -> RETURN_FORM_ACTION.equals(element.parentFormAction()))
					.filter(element -> "submit".equals(element.attribute("type")))
					.toList();
			assertEquals(2, submitButtons.size());

			RenderedElement ordinaryButton = submitButtons.getFirst();
			assertEquals("Concluir atividade", ordinaryButton.normalizedText());
			assertFalse(ordinaryButton.hasAttribute("name"));
			assertFalse(ordinaryButton.hasAttribute("value"));

			RenderedElement confirmationButton = submitButtons.get(1);
			assertEquals("confirmZeroConsumption", confirmationButton.attribute("name"));
			assertEquals("true", confirmationButton.attribute("value"));
			assertEquals("Confirmar consumo zero e concluir", confirmationButton.normalizedText());

			long hiddenConfirmations = elements.stream()
					.filter(element -> element.tag().equals(HTML.Tag.INPUT.toString()))
					.filter(element -> "hidden".equals(element.attribute("type")))
					.filter(element -> "confirmZeroConsumption".equals(element.attribute("name")))
					.count();
			assertEquals(0, hiddenConfirmations);
			assertEveryAriaDescriptionResolves();
			assertIdsAreUnique();
		}
	}

	private static final class RenderedElement {

		private final String tag;
		private final Map<String, String> attributes;
		private final String parentFormAction;
		private final StringBuilder text = new StringBuilder();

		private RenderedElement(String tag, Map<String, String> attributes, String parentFormAction) {
			this.tag = tag;
			this.attributes = attributes;
			this.parentFormAction = parentFormAction;
		}

		private String tag() {
			return tag;
		}

		private String attribute(String name) {
			return attributes.get(name);
		}

		private boolean hasAttribute(String name) {
			return attributes.containsKey(name);
		}

		private String parentFormAction() {
			return parentFormAction;
		}

		private void appendText(char[] data) {
			text.append(' ').append(data);
		}

		private String normalizedText() {
			return text.toString().replaceAll("\\s+", " ").trim();
		}
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
