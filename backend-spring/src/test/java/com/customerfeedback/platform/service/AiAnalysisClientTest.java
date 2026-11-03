package com.customerfeedback.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiAnalysisClientTest {

    private MockRestServiceServer server;
    private AiAnalysisClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiAnalysisClient(builder, "http://ai-service.test");
    }

    @Test
    void shouldCallFastApiForSingleFeedbackAnalysis() {
        UUID feedbackId = UUID.randomUUID();
        server.expect(requestTo("http://ai-service.test/api/v1/analyze/" + feedbackId))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "analysisId":"00000000-0000-0000-0000-000000000001",
                          "feedbackId":"%s",
                          "sentiment":"NEGATIVE",
                          "category":"Payments",
                          "theme":"Refund Delay",
                          "emotion":"Frustrated",
                          "priority":"HIGH",
                          "assignedDepartment":"Payments Team",
                          "summary":"Refund is delayed.",
                          "confidence":0.92,
                          "analyzedAt":"2026-11-03T10:00:00Z",
                          "modelName":"gemini-2.5-flash",
                          "promptVersion":"v1.0",
                          "isCurrent":true,
                          "createdAt":"2026-11-03T10:00:00Z"
                        }
                        """.formatted(feedbackId), MediaType.APPLICATION_JSON));

        assertEquals(feedbackId, client.analyzeFeedback(feedbackId).getFeedbackId());
        server.verify();
    }

    @Test
    void shouldPassOptionalBatchLimitToFastApi() {
        UUID batchId = UUID.randomUUID();
        server.expect(requestTo("http://ai-service.test/api/v1/analyze/batch/" + batchId + "?limit=4"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"batchId":"%s","processed":4,"successful":3,"failed":1,"durationSeconds":2.5}
                        """.formatted(batchId), MediaType.APPLICATION_JSON));

        var result = client.analyzeBatch(batchId, 4);

        assertEquals(batchId.toString(), result.getBatchId());
        assertEquals(4, result.getProcessed());
        server.verify();
    }
}
