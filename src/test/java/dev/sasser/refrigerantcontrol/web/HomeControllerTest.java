package dev.sasser.refrigerantcontrol.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldRenderHomePage() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(view().name("home"))
				.andExpect(content().string(containsString("Controle de Gases Refrigerantes")))
				.andExpect(content().string(containsString("Esta página confirma a verificação inicial do ambiente de desenvolvimento.")))
				.andExpect(content().string(containsString("A aplicação está em execução.")))
				.andExpect(content().string(containsString("href=\"/cylinders\"")))
				.andExpect(content().string(containsString("href=\"/activities/start\"")))
				.andExpect(content().string(containsString("Iniciar atividade")))
				.andExpect(content().string(containsString("href=\"/activities/return\"")))
				.andExpect(content().string(containsString("Registrar retorno")))
				.andExpect(content().string(containsString("href=\"/status\"")));
	}
}
