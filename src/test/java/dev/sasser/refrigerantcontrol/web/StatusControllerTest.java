package dev.sasser.refrigerantcontrol.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatusController.class)
class StatusControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldReturnLocalApplicationStatus() throws Exception {
		mockMvc.perform(get("/status"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.*", hasSize(3)))
				.andExpect(jsonPath("$.application").value("Controle de Gases Refrigerantes"))
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.environment").value("local"));
	}
}
