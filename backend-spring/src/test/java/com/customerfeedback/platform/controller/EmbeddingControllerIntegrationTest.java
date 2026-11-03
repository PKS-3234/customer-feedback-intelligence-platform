package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.BatchEmbeddingSummaryResponse;
import com.customerfeedback.platform.dto.SemanticSearchResult;
import com.customerfeedback.platform.dto.StoredEmbeddingResponse;
import com.customerfeedback.platform.service.AiEmbeddingClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmbeddingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiEmbeddingClient aiEmbeddingClient;

    @Test
    void shouldProxySingleEmbeddingRequest() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        when(aiEmbeddingClient.embedFeedback(feedbackId)).thenReturn(embedding(feedbackId));

        mockMvc.perform(post("/api/v1/embeddings/feedback/{feedbackId}", feedbackId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackId", is(feedbackId.toString())))
                .andExpect(jsonPath("$.embeddingDimension", is(768)));

        verify(aiEmbeddingClient).embedFeedback(feedbackId);
    }

    @Test
    void shouldRetrieveEmbeddingMetadata() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        when(aiEmbeddingClient.getFeedbackEmbedding(feedbackId)).thenReturn(embedding(feedbackId));

        mockMvc.perform(get("/api/v1/embeddings/feedback/{feedbackId}", feedbackId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embeddingModel", is("gemini-embedding-001")));
    }

    @Test
    void shouldTriggerBatchEmbedding() throws Exception {
        UUID batchId = UUID.randomUUID();
        when(aiEmbeddingClient.embedBatch(batchId, 2))
                .thenReturn(new BatchEmbeddingSummaryResponse(batchId.toString(), 2, 2, 0, 1.5));

        mockMvc.perform(post("/api/v1/embeddings/batches/{batchId}", batchId)
                        .queryParam("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successful", is(2)));
    }

    @Test
    void shouldRejectInvalidSemanticSearchRequest() throws Exception {
        mockMvc.perform(post("/api/v1/semantic-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":" ","limit":51}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnSemanticSearchResults() throws Exception {
        when(aiEmbeddingClient.semanticSearch(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new SemanticSearchResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "A duplicate debit occurred.",
                        "NEGATIVE",
                        "Payments",
                        "Duplicate Debit",
                        "Frustrated",
                        "HIGH",
                        "Duplicate charge.",
                        0.94
                )));

        mockMvc.perform(post("/api/v1/semantic-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"duplicate payment charge","limit":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category", is("Payments")))
                .andExpect(jsonPath("$[0].similarity", is(0.94)));
    }

    private StoredEmbeddingResponse embedding(UUID feedbackId) {
        return new StoredEmbeddingResponse(
                UUID.randomUUID(),
                feedbackId,
                "gemini-embedding-001",
                768,
                true,
                Instant.parse("2026-11-03T10:00:00Z")
        );
    }
}
