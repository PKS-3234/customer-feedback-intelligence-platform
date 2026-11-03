package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.BatchAnalysisSummaryResponse;
import com.customerfeedback.platform.dto.FeedbackAnalysisResponse;
import com.customerfeedback.platform.entity.FeedbackAnalysis;
import com.customerfeedback.platform.repository.FeedbackAnalysisRepository;
import com.customerfeedback.platform.repository.IngestionBatchRepository;
import com.customerfeedback.platform.service.AiAnalysisClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
class AnalysisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiAnalysisClient aiAnalysisClient;

    @MockBean
    private FeedbackAnalysisRepository analysisRepository;

    @MockBean
    private IngestionBatchRepository batchRepository;

    @Test
    void shouldProxySingleFeedbackAnalysisToAiService() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        FeedbackAnalysisResponse response = new FeedbackAnalysisResponse();
        response.setFeedbackId(feedbackId);
        response.setSentiment("NEGATIVE");
        when(aiAnalysisClient.analyzeFeedback(feedbackId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/analysis/feedback/{feedbackId}", feedbackId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackId", is(feedbackId.toString())))
                .andExpect(jsonPath("$.sentiment", is("NEGATIVE")));

        verify(aiAnalysisClient).analyzeFeedback(feedbackId);
    }

    @Test
    void shouldProxyBatchAnalysisAndReturnSummary() throws Exception {
        UUID batchId = UUID.randomUUID();
        BatchAnalysisSummaryResponse response = new BatchAnalysisSummaryResponse();
        response.setBatchId(batchId.toString());
        response.setProcessed(5);
        response.setSuccessful(4);
        response.setFailed(1);
        response.setDurationSeconds(1.7);
        when(aiAnalysisClient.analyzeBatch(batchId, 5)).thenReturn(response);

        mockMvc.perform(post("/api/v1/analysis/batches/{batchId}", batchId)
                        .queryParam("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed", is(5)))
                .andExpect(jsonPath("$.successful", is(4)))
                .andExpect(jsonPath("$.failed", is(1)));

        verify(aiAnalysisClient).analyzeBatch(batchId, 5);
    }

    @Test
    void shouldRejectNonPositiveBatchLimit() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/batches/{batchId}", UUID.randomUUID())
                        .queryParam("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_INPUT")));
    }

    @Test
    void shouldRetrievePersistedCurrentAnalysisByFeedbackId() throws Exception {
        UUID feedbackId = UUID.randomUUID();
        when(analysisRepository.findByFeedbackIdAndIsCurrent(feedbackId, true))
                .thenReturn(Optional.of(analysis(feedbackId)));

        mockMvc.perform(get("/api/v1/analysis/feedback/{feedbackId}", feedbackId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackId", is(feedbackId.toString())))
                .andExpect(jsonPath("$.sentiment", is("POSITIVE")))
                .andExpect(jsonPath("$.summary", is("Helpful support.")))
                .andExpect(jsonPath("$.isCurrent", is(true)));
    }

    @Test
    void shouldRetrievePersistedCurrentAnalysesByBatchId() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID feedbackId = UUID.randomUUID();
        when(batchRepository.existsById(batchId)).thenReturn(true);
        when(analysisRepository.findCurrentByBatchId(batchId))
                .thenReturn(List.of(analysis(feedbackId)));

        mockMvc.perform(get("/api/v1/analysis/batches/{batchId}", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].feedbackId", is(feedbackId.toString())))
                .andExpect(jsonPath("$[0].priority", is("LOW")));
    }

    private FeedbackAnalysis analysis(UUID feedbackId) {
        FeedbackAnalysis analysis = new FeedbackAnalysis();
        analysis.setAnalysisId(UUID.randomUUID());
        analysis.setFeedbackId(feedbackId);
        analysis.setSentiment("POSITIVE");
        analysis.setCategory("Support");
        analysis.setTheme("Helpful Interaction");
        analysis.setEmotion("Satisfied");
        analysis.setPriority("LOW");
        analysis.setAssignedDepartment("Support Team");
        analysis.setShortSummary("Helpful support.");
        analysis.setConfidenceScore(new BigDecimal("0.95"));
        analysis.setAnalyzedAt(Instant.parse("2026-11-03T10:00:00Z"));
        analysis.setModelName("gemini-2.5-flash");
        analysis.setPromptVersion("v1.0");
        analysis.setIsCurrent(true);
        analysis.setCreatedAt(Instant.parse("2026-11-03T10:00:00Z"));
        return analysis;
    }
}
