package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.SemanticSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiEmbeddingClientTest {

    private MockRestServiceServer server;
    private AiEmbeddingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiEmbeddingClient(builder, "http://ai-service.test");
    }

    @Test
    void shouldTriggerBatchEmbeddingThroughFastApi() {
        UUID batchId = UUID.randomUUID();
        server.expect(requestTo("http://ai-service.test/api/v1/embeddings/batches/" + batchId + "?limit=4"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"batchId":"%s","processed":4,"successful":3,"failed":1,"durationSeconds":2.0}
                        """.formatted(batchId), MediaType.APPLICATION_JSON));

        var result = client.embedBatch(batchId, 4);

        assertEquals(4, result.processed());
        assertEquals(3, result.successful());
        server.verify();
    }

    @Test
    void shouldSendSemanticSearchQueryToFastApi() {
        server.expect(requestTo("http://ai-service.test/api/v1/semantic-search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"query":"duplicate card charge","limit":5}
                        """))
                .andRespond(withSuccess("""
                        [{
                          "feedbackId":"00000000-0000-0000-0000-000000000001",
                          "batchId":"00000000-0000-0000-0000-000000000002",
                          "feedbackText":"Card charged twice.",
                          "sentiment":"NEGATIVE",
                          "category":"Payments",
                          "theme":"Duplicate Debit",
                          "emotion":"Frustrated",
                          "priority":"HIGH",
                          "summary":"Duplicate debit.",
                          "similarity":0.93
                        }]
                        """, MediaType.APPLICATION_JSON));

        var results = client.semanticSearch(new SemanticSearchRequest("duplicate card charge", 5));

        assertEquals(1, results.size());
        assertEquals(0.93, results.getFirst().similarity());
        server.verify();
    }
}
