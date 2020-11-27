package ru.lanit.at;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NodeTests {
	private String content;

	private final String body = "{\n" +
			"\t\"host\" : \"host\",\n" +
			"\t\"port\" : \"port\",\n" +
			"\t\"nodeName\" : \"nodeName\"\n" +
			"}";

	String expectedResultGetRequest = "[ {\n" +
			"  \"nodeName\" : \"nodeName\",\n" +
			"  \"address\" : \"http://host:port\",\n" +
			"  \"isFree\" : \"Yes\",\n" +
			"  \"idSession\" : \" \",\n" +
			"  \"timeout\" : \"60\"\n" +
			"} ]";

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void registerNodeAndGetInfo() throws Exception {
		MvcResult resultPostRequest = this.mockMvc.perform(post("/api/register/node").contentType(
				MediaType.APPLICATION_JSON)
				.content(body))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultPostRequest.getResponse().getContentAsString();
		Assertions.assertEquals("The node has been registered successfully. The name - \"nodeName\"", content);

		MvcResult resultGetRequest = this.mockMvc.perform(get("/status"))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultGetRequest.getResponse().getContentAsString();

		//Нормализация из LF в CRLF. При возникновении ошибок (например, при тестировании в Linux) - убрать.
		content = content.replaceAll("\\r\\n", "\n");
		content = content.replaceAll("\\r", "\n");
		Assertions.assertEquals(content, expectedResultGetRequest);
	}

	@Test
	public void deleteAllNodesAndGetResult() throws Exception {
		MvcResult resultPostRequest = this.mockMvc.perform(post("/api/register/node").contentType(
				MediaType.APPLICATION_JSON)
				.content(body))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultPostRequest.getResponse().getContentAsString();
		Assertions.assertEquals("The node has been registered successfully. The name - \"nodeName\"", content);

		MvcResult resultGetRequest = this.mockMvc.perform(get("/api/delete/nodes"))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultGetRequest.getResponse().getContentAsString();
		Assertions.assertEquals("Information has been deleted.", content);
	}

	@Test
	public void deleteNodeAndGetResult () throws Exception {
		MvcResult resultPostRequest = this.mockMvc.perform(post("/api/register/node").contentType(
				MediaType.APPLICATION_JSON)
				.content(body))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultPostRequest.getResponse().getContentAsString();
		Assertions.assertEquals("The node has been registered successfully. The name - \"nodeName\"", content);

		MvcResult resultGetRequest = this.mockMvc.perform(get("/api/delete/node/nodeName"))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultGetRequest.getResponse().getContentAsString();
		Assertions.assertEquals("Information about node nodeName has been deleted.", content);
	}

	@Test
	public void setTimeoutTest() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/set/timeout/120"))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = result.getResponse().getContentAsString();
		Assertions.assertEquals("Set timeout value 120", content);
	}
}
