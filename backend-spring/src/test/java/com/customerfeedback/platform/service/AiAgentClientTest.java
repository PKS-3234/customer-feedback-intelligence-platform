package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.AgentQueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiAgentClientTest {

    private MockRestServiceServer server;
    private AiAgentClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiAgentClient(builder, "http://ai-service.test");
    }

    @Test
    void shouldProxyAgentQueryAndReturnEvidence() {
        server.expect(requestTo("http://ai-service.test/api/v1/agents/query"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"query":"What issues are reported?","limit":4}
                        """))
                .andRespond(withSuccess("""
                        {
                          "answer":"Customers report duplicate charges [feedback-1].",
                          "agentsUsed":["Supervisor Agent","Retrieval Agent","Reporting Agent"],
                          "supportingEvidence":[{
                            "evidenceId":"feedback-1",
                            "source":"feedback",
                            "title":"Similar feedback",
                            "content":"Card charged twice.",
                            "metadata":{"similarity":0.94}
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.query(new AgentQueryRequest("What issues are reported?", 4));

        assertEquals(3, response.agentsUsed().size());
        assertEquals("Card charged twice.", response.supportingEvidence().getFirst().content());
        server.verify();
    }

    @Test
    void shouldSendDefaultLimitWhenOmitted() {
        server.expect(requestTo("http://ai-service.test/api/v1/agents/query"))
                .andExpect(content().json("""
                        {"query":"Summarize customer feedback","limit":5}
                        """))
                .andRespond(withSuccess("""
                        {"answer":"Summary.","agentsUsed":[],"supportingEvidence":[]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("Summary.", client.query(new AgentQueryRequest("Summarize customer feedback", null)).answer());
        server.verify();
    }
}
