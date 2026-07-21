package dev.sasser.refrigerantcontrol.web.cylinder;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import dev.sasser.refrigerantcontrol.application.CylinderUseCases;
import dev.sasser.refrigerantcontrol.application.port.CylinderStore;
import dev.sasser.refrigerantcontrol.domain.Cylinder;
import dev.sasser.refrigerantcontrol.domain.RefrigerantGas;
import dev.sasser.refrigerantcontrol.domain.SealNumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CylinderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CylinderUseCases cylinderUseCases;

	@Autowired
	private CylinderStore cylinderStore;

	@Test
	void shouldRenderCompleteCylinderRegistrationPage() throws Exception {
		mockMvc.perform(get("/cylinders"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(view().name("cylinders"))
				.andExpect(model().attributeExists("cylinderRegistrationForm", "initialGrossWeightForm"))
				.andExpect(model().attribute("operationalRefrigerantNames", RefrigerantGas.supportedOperationalNames()))
				.andExpect(content().string(containsString("<form method=\"post\" action=\"/cylinders\">")))
				.andExpect(content().string(containsString("<form method=\"post\" action=\"/cylinders/initial-weight\">")))
				.andExpect(content().string(containsString("href=\"/\"")))
				.andExpect(content().string(containsString("href=\"/activities/start\"")))
				.andExpect(content().string(containsString("Iniciar atividade")))
				.andExpect(content().string(containsString("for=\"cylinder-seal-number\">Número do lacre")))
				.andExpect(content().string(containsString("for=\"operational-refrigerant-name\">Gás refrigerante")))
				.andExpect(content().string(containsString("for=\"initial-gross-weight\">Peso bruto inicial")))
				.andExpect(content().string(containsString("inputmode=\"decimal\"")))
				.andExpect(content().string(containsString(
						"<label for=\"initial-gross-weight\">Peso bruto inicial (kg)</label>")))
				.andExpect(content().string(containsString(
						"Os dados cadastrados nesta etapa ficam apenas na memória e serão perdidos quando a aplicação for reiniciada.")))
				.andExpect(content().string(not(containsString("name=\"departureGrossWeight\""))))
				.andExpect(content().string(not(containsString("name=\"returnGrossWeight\""))))
				.andExpect(content().string(not(containsString("action=\"/activities/start\""))));
	}

	@Test
	void shouldRegisterCylinderAndPreserveExactOperationalGasName() throws Exception {
		String sealNumber = "WEB-REGISTRATION-001";

		mockMvc.perform(post("/cylinders")
					.param("sealNumber", sealNumber)
					.param("operationalRefrigerantName", "R-22"))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute(
						"successMessage",
						"Cilindro cadastrado com sucesso. O lacre foi preenchido abaixo para registrar o peso inicial."));

		Cylinder storedCylinder = findCylinder(sealNumber);
		assertEquals(sealNumber, storedCylinder.sealNumber().value());
		assertEquals("R-22", storedCylinder.refrigerantGas().operationalName());
		assertTrue(storedCylinder.initialGrossWeight().isEmpty());
	}

	@Test
	void shouldEncodeAndRestoreExactSealDuringRegistrationRedirect() throws Exception {
		String sealNumber = "WEB SPECIAL / + 002";

		MvcResult registration = mockMvc.perform(post("/cylinders")
					.param("sealNumber", sealNumber)
					.param("operationalRefrigerantName", "R410A"))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute(
						"successMessage",
						"Cilindro cadastrado com sucesso. O lacre foi preenchido abaixo para registrar o peso inicial."))
				.andReturn();

		String location = registration.getResponse().getRedirectedUrl();
		URI redirectUri = URI.create(location);
		String decodedSeal = URLDecoder.decode(
				redirectUri.getRawQuery().substring("seal=".length()),
				StandardCharsets.UTF_8);

		assertEquals("/cylinders", redirectUri.getPath());
		assertEquals(sealNumber, decodedSeal);
		assertFalse(location.startsWith("//"));

		mockMvc.perform(get("/cylinders")
					.queryParam("seal", decodedSeal)
					.flashAttrs(registration.getFlashMap()))
				.andExpect(status().isOk())
				.andExpect(model().attribute(
						"initialGrossWeightForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(model().attribute(
						"successMessage",
						"Cilindro cadastrado com sucesso. O lacre foi preenchido abaixo para registrar o peso inicial."));

		mockMvc.perform(get("/cylinders").queryParam("seal", decodedSeal))
				.andExpect(status().isOk())
				.andExpect(model().attribute(
						"initialGrossWeightForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(model().attributeDoesNotExist("successMessage"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	void shouldRejectBlankCylinderSealAndPreserveSubmittedValue(String sealNumber) throws Exception {
		mockMvc.perform(post("/cylinders")
					.param("sealNumber", sealNumber)
					.param("operationalRefrigerantName", "R410A"))
				.andExpect(status().isOk())
				.andExpect(view().name("cylinders"))
				.andExpect(model().attributeHasFieldErrors("cylinderRegistrationForm", "sealNumber"))
				.andExpect(model().attribute(
						"cylinderRegistrationForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(content().string(containsString(
						"<option value=\"R410A\" selected=\"selected\">R410A</option>")))
				.andExpect(content().string(containsString("Informe o número do lacre.")));
	}

	@Test
	void shouldRejectBlankRefrigerantGasAndPreserveSeal() throws Exception {
		String sealNumber = "WEB-BLANK-GAS-003";

		mockMvc.perform(post("/cylinders")
					.param("sealNumber", sealNumber)
					.param("operationalRefrigerantName", ""))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("cylinderRegistrationForm", "operationalRefrigerantName"))
				.andExpect(model().attribute(
						"cylinderRegistrationForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(content().string(containsString("Selecione o gás refrigerante.")));
	}

	@Test
	void shouldRejectTamperedGasWithoutAddingItAsAnOption() throws Exception {
		mockMvc.perform(post("/cylinders")
					.param("sealNumber", "WEB-TAMPERED-GAS-004")
					.param("operationalRefrigerantName", "R507"))
				.andExpect(status().isOk())
				.andExpect(view().name("cylinders"))
				.andExpect(model().attributeHasFieldErrors("cylinderRegistrationForm", "operationalRefrigerantName"))
				.andExpect(model().attribute("operationalRefrigerantNames", RefrigerantGas.supportedOperationalNames()))
				.andExpect(content().string(not(containsString("<option value=\"R507\""))))
				.andExpect(content().string(containsString("Selecione um gás refrigerante válido.")))
				.andExpect(content().string(not(containsString("unsupported operational refrigerant name"))));
	}

	@Test
	void shouldRejectDuplicateSealWithoutExposingTechnicalException() throws Exception {
		String sealNumber = "WEB-DUPLICATE-005";
		cylinderUseCases.registerCylinder(sealNumber, "R32");

		mockMvc.perform(post("/cylinders")
					.param("sealNumber", sealNumber)
					.param("operationalRefrigerantName", "R32"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("cylinderRegistrationForm", "sealNumber"))
				.andExpect(content().string(containsString("Já existe um cilindro com este número de lacre.")))
				.andExpect(content().string(not(containsString("CylinderAlreadyRegisteredException"))))
				.andExpect(content().string(not(containsString("Cylinder is already registered"))));
	}

	@Test
	void shouldRegisterCommaDecimalInitialWeightAndPreserveScale() throws Exception {
		String sealNumber = "WEB-INITIAL-WEIGHT-006";
		cylinderUseCases.registerCylinder(sealNumber, "R407C");

		MvcResult registration = mockMvc.perform(post("/cylinders/initial-weight")
					.param("sealNumber", sealNumber)
					.param("initialGrossWeight", "15,140"))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("successMessage", "Peso bruto inicial registrado com sucesso."))
				.andReturn();

		URI redirectUri = URI.create(registration.getResponse().getRedirectedUrl());
		assertEquals("/activities/start", redirectUri.getPath());
		assertEquals(sealNumber, decodeSealQuery(redirectUri));

		BigDecimal storedWeight = findCylinder(sealNumber).initialGrossWeight().orElseThrow().inKilograms();
		assertEquals(new BigDecimal("15.140"), storedWeight);
		assertEquals(3, storedWeight.scale());
	}

	@Test
	void shouldSafelyRedirectSpecialSealToActivityStartAndConsumeSuccessFlashOnce() throws Exception {
		String sealNumber = "WEB INITIAL / + 010";
		cylinderUseCases.registerCylinder(sealNumber, "R410A");

		MvcResult registration = mockMvc.perform(post("/cylinders/initial-weight")
					.param("sealNumber", sealNumber)
					.param("initialGrossWeight", "15,140"))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attribute("successMessage", "Peso bruto inicial registrado com sucesso."))
				.andReturn();

		String location = registration.getResponse().getRedirectedUrl();
		URI redirectUri = URI.create(location);
		String decodedSeal = decodeSealQuery(redirectUri);
		MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);

		assertEquals("/activities/start", redirectUri.getPath());
		assertEquals(sealNumber, decodedSeal);
		assertFalse(location.startsWith("//"));
		assertNotNull(session);

		mockMvc.perform(get(redirectUri)
					.session(session)
					.with(request -> {
						request.setParameter("seal", decodedSeal);
						return request;
					}))
				.andExpect(status().isOk())
				.andExpect(view().name("activity-start"))
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(model().attribute(
						"successMessage",
						"Peso bruto inicial registrado com sucesso."));

		BigDecimal weightAfterFirstGet = findCylinder(sealNumber).initialGrossWeight().orElseThrow().inKilograms();
		assertEquals(new BigDecimal("15.140"), weightAfterFirstGet);
		assertEquals(3, weightAfterFirstGet.scale());

		mockMvc.perform(get(redirectUri)
					.session(session)
					.with(request -> {
						request.setParameter("seal", decodedSeal);
						return request;
					}))
				.andExpect(status().isOk())
				.andExpect(model().attribute(
						"activityStartForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(model().attributeDoesNotExist("successMessage"));

		BigDecimal storedWeight = findCylinder(sealNumber).initialGrossWeight().orElseThrow().inKilograms();
		assertEquals(new BigDecimal("15.140"), storedWeight);
		assertEquals(3, storedWeight.scale());
	}

	@Test
	void shouldRejectBlankInitialWeightFieldsAndPreserveSubmittedValues() throws Exception {
		mockMvc.perform(post("/cylinders/initial-weight")
					.param("sealNumber", "")
					.param("initialGrossWeight", ""))
				.andExpect(status().isOk())
				.andExpect(view().name("cylinders"))
				.andExpect(model().attributeHasFieldErrors(
						"initialGrossWeightForm",
						"sealNumber",
						"initialGrossWeight"))
				.andExpect(model().attribute(
						"initialGrossWeightForm",
						hasProperty("initialGrossWeight", equalTo(""))))
				.andExpect(content().string(containsString("Informe o número do lacre.")))
				.andExpect(content().string(containsString("Informe o peso bruto inicial.")));
	}

	@ParameterizedTest
	@ValueSource(strings = {"letters", "1e3", "15,1,4", "15.1.4", "1.234,56", "1,234.56"})
	void shouldRejectMalformedInitialWeightWithoutCallingUseCase(String input) throws Exception {
		mockMvc.perform(post("/cylinders/initial-weight")
					.param("sealNumber", "WEB-MALFORMED-WEIGHT")
					.param("initialGrossWeight", input))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("initialGrossWeightForm", "initialGrossWeight"))
				.andExpect(model().attribute(
						"initialGrossWeightForm",
						hasProperty("initialGrossWeight", equalTo(input))))
				.andExpect(content().string(containsString(
						"Informe um peso válido usando vírgula ou ponto como separador decimal.")))
				.andExpect(content().string(not(containsString("O cilindro informado não existe."))));
	}

	@Test
	void shouldDelegateNegativeWeightValidationToDomainWithoutSaving() throws Exception {
		String sealNumber = "WEB-NEGATIVE-WEIGHT-007";
		cylinderUseCases.registerCylinder(sealNumber, "R134A");

		mockMvc.perform(post("/cylinders/initial-weight")
					.param("sealNumber", sealNumber)
					.param("initialGrossWeight", "-15,140"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("initialGrossWeightForm", "initialGrossWeight"))
				.andExpect(content().string(containsString("O peso não pode ser negativo.")));

		assertTrue(findCylinder(sealNumber).initialGrossWeight().isEmpty());
	}

	@Test
	void shouldRejectInitialWeightForMissingCylinder() throws Exception {
		mockMvc.perform(post("/cylinders/initial-weight")
					.param("sealNumber", "WEB-MISSING-CYLINDER-008")
					.param("initialGrossWeight", "15,140"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasFieldErrors("initialGrossWeightForm", "sealNumber"))
				.andExpect(content().string(containsString("O cilindro informado não existe.")))
				.andExpect(content().string(not(containsString("CylinderNotFoundException"))));
	}

	@Test
	void shouldRejectSecondInitialWeightAndPreserveFirstStoredValue() throws Exception {
		String sealNumber = "WEB-SECOND-WEIGHT-009";
		cylinderUseCases.registerCylinder(sealNumber, "R404");
		cylinderUseCases.registerInitialGrossWeight(sealNumber, new BigDecimal("15.140"));

		mockMvc.perform(post("/cylinders/initial-weight")
					.param("sealNumber", sealNumber)
					.param("initialGrossWeight", "14,500"))
				.andExpect(status().isOk())
				.andExpect(model().attributeHasErrors("initialGrossWeightForm"))
				.andExpect(content().string(containsString(
						"O peso bruto inicial deste cilindro já foi registrado.")))
				.andExpect(content().string(not(containsString("IllegalStateException"))));

		BigDecimal storedWeight = findCylinder(sealNumber).initialGrossWeight().orElseThrow().inKilograms();
		assertEquals(new BigDecimal("15.140"), storedWeight);
	}

	@Test
	void shouldEscapeHtmlSignificantSealWhenRenderingValidationFailure() throws Exception {
		String sealNumber = "<script>alert('unsafe')</script>";

		mockMvc.perform(post("/cylinders")
					.param("sealNumber", sealNumber)
					.param("operationalRefrigerantName", ""))
				.andExpect(status().isOk())
				.andExpect(model().attribute(
						"cylinderRegistrationForm",
						hasProperty("sealNumber", equalTo(sealNumber))))
				.andExpect(content().string(not(containsString(sealNumber))))
				.andExpect(content().string(containsString("&lt;script&gt;alert(&#39;unsafe&#39;)&lt;/script&gt;")))
				.andExpect(content().string(not(containsString("<script>"))));
	}

	private Cylinder findCylinder(String sealNumber) {
		return cylinderStore.findBySealNumber(SealNumber.of(sealNumber)).orElseThrow();
	}

	private String decodeSealQuery(URI redirectUri) {
		return URLDecoder.decode(
				redirectUri.getRawQuery().substring("seal=".length()),
				StandardCharsets.UTF_8);
	}
}
