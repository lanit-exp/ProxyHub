package ru.lanit.at;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.lanit.at.util.ResourceUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(value = {"test"})
class NodeTests {
	private String content;

	@Value("classpath:body.json")
	private Resource body;

	@Value("classpath:expected-request-body.json")
	private Resource expectedResultGetRequest;

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void registerNodeAndGetInfo() throws Exception {
		MvcResult resultPostRequest = this.mockMvc.perform(post("/node/register").contentType(
				MediaType.APPLICATION_JSON)
				.content(ResourceUtils.asString(body)))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultPostRequest.getResponse().getContentAsString();
		Assertions.assertEquals("The node has been registered successfully. The name - \"nodeName\"", content);

		MvcResult resultGetRequest = this.mockMvc.perform(get("/node/status"))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultGetRequest.getResponse().getContentAsString();
		log.info("Content1 {}", content);
		log.info("Content2 {}", ResourceUtils.asString(expectedResultGetRequest));

		//Нормализация из LF в CRLF. При возникновении ошибок (например, при тестировании в Linux) - убрать.
		content = content.replaceAll("\\r\\n", "\n");
		content = content.replaceAll("\\r", "\n");
		Assertions.assertEquals(content, ResourceUtils.asString(expectedResultGetRequest));
	}

	@Test
	public void deleteAllNodesAndGetResult() throws Exception {
		MvcResult resultPostRequest = this.mockMvc.perform(post("/node/register").contentType(
				MediaType.APPLICATION_JSON)
				.content(ResourceUtils.asString(body)))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultPostRequest.getResponse().getContentAsString();
		Assertions.assertEquals("The node has been registered successfully. The name - \"nodeName\"", content);

		MvcResult resultGetRequest = this.mockMvc.perform(get("/node/clear"))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultGetRequest.getResponse().getContentAsString();
		Assertions.assertEquals("Information has been deleted.", content);
	}

	@Test
	public void deleteNodeAndGetResult () throws Exception {
		MvcResult resultPostRequest = this.mockMvc.perform(post("/node/register").contentType(
				MediaType.APPLICATION_JSON)
				.content(ResourceUtils.asString(body)))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultPostRequest.getResponse().getContentAsString();
		Assertions.assertEquals("The node has been registered successfully. The name - \"nodeName\"", content);

		MvcResult resultGetRequest = this.mockMvc.perform(get("/node/delete/nodeName"))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultGetRequest.getResponse().getContentAsString();
		Assertions.assertEquals("Information about node nodeName has been deleted.", content);
	}

	@Test
	public void setTimeoutTest() throws Exception {
		MvcResult result = mockMvc.perform(get("/hub/timeout/set/120"))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = result.getResponse().getContentAsString();
		Assertions.assertEquals("Set timeout value 120", content);
	}
}
