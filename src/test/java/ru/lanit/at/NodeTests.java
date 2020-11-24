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
			"\t\"applicationName\" : \"applicationName\"\n" +
			"}";

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
		Assertions.assertEquals("The node has been registered successfully. The name - \"applicationName\"", content);

		MvcResult resultGetRequest = this.mockMvc.perform(get("/api/get/nodes"))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultGetRequest.getResponse().getContentAsString();
		String expectedResultGetRequest = "[{\"address\":\"http://host:port\",\"isFree\":\"Yes\",\"idSession\":\" \",\"applicationName\":\"applicationName\"}]";
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
		Assertions.assertEquals("The node has been registered successfully. The name - \"applicationName\"", content);

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
		Assertions.assertEquals("The node has been registered successfully. The name - \"applicationName\"", content);

		MvcResult resultGetRequest = this.mockMvc.perform(get("/api/delete/node/applicationName"))
				.andDo(print())
				.andExpect(status().isOk())
				.andReturn();

		content = resultGetRequest.getResponse().getContentAsString();
		Assertions.assertEquals("Information about node applicationName has been deleted.", content);
	}
}
