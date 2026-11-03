package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.BatchAnalysisSummaryResponse;
import com.customerfeedback.platform.dto.FeedbackAnalysisResponse;
import com.customerfeedback.platform.entity.FeedbackAnalysis;
import com.customerfeedback.platform.exception.ResourceNotFoundException;
import com.customerfeedback.platform.repository.FeedbackAnalysisRepository;
import com.customerfeedback.platform.repository.IngestionBatchRepository;
import com.customerfeedback.platform.service.AiAnalysisClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analysis")
@Tag(name = "Analysis", description = "Trigger AI feedback analysis and retrieve persisted results.")
public class AnalysisController {

    private final AiAnalysisClient aiAnalysisClient;
    private final FeedbackAnalysisRepository analysisRepository;
    private final IngestionBatchRepository batchRepository;

    public AnalysisController(
            AiAnalysisClient aiAnalysisClient,
            FeedbackAnalysisRepository analysisRepository,
            IngestionBatchRepository batchRepository) {
        this.aiAnalysisClient = aiAnalysisClient;
        this.analysisRepository = analysisRepository;
        this.batchRepository = batchRepository;
    }

    @PostMapping("/feedback/{feedbackId}")
    @Operation(summary = "Analyze one feedback record")
    public ResponseEntity<FeedbackAnalysisResponse> analyzeFeedback(
            @PathVariable UUID feedbackId) {
        return ResponseEntity.ok(aiAnalysisClient.analyzeFeedback(feedbackId));
    }

    @PostMapping("/batches/{batchId}")
    @Operation(summary = "Analyze pending feedback in a batch")
    public ResponseEntity<BatchAnalysisSummaryResponse> analyzeBatch(
            @PathVariable UUID batchId,
            @RequestParam(required = false) Integer limit) {
        if (limit != null && limit < 1) {
            throw new IllegalArgumentException("Analysis limit must be greater than zero.");
        }
        return ResponseEntity.ok(aiAnalysisClient.analyzeBatch(batchId, limit));
    }

    @GetMapping("/feedback/{feedbackId}")
    @Operation(summary = "Get current analysis for one feedback record")
    public ResponseEntity<FeedbackAnalysisResponse> getFeedbackAnalysis(
            @PathVariable UUID feedbackId) {
        FeedbackAnalysis analysis = analysisRepository
                .findByFeedbackIdAndIsCurrent(feedbackId, true)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No current analysis found for feedback ID: " + feedbackId));
        return ResponseEntity.ok(toResponse(analysis));
    }

    @GetMapping("/batches/{batchId}")
    @Operation(summary = "List current analysis results for a batch")
    public ResponseEntity<List<FeedbackAnalysisResponse>> getBatchAnalysis(
            @PathVariable UUID batchId) {
        if (!batchRepository.existsById(batchId)) {
            throw new ResourceNotFoundException("Batch not found with ID: " + batchId);
        }
        List<FeedbackAnalysisResponse> results = analysisRepository
                .findCurrentByBatchId(batchId)
                .stream()
                .map(AnalysisController::toResponse)
                .toList();
        return ResponseEntity.ok(results);
    }

    private static FeedbackAnalysisResponse toResponse(FeedbackAnalysis analysis) {
        FeedbackAnalysisResponse response = new FeedbackAnalysisResponse();
        response.setAnalysisId(analysis.getAnalysisId());
        response.setFeedbackId(analysis.getFeedbackId());
        response.setSentiment(analysis.getSentiment());
        response.setCategory(analysis.getCategory());
        response.setTheme(analysis.getTheme());
        response.setEmotion(analysis.getEmotion());
        response.setPriority(analysis.getPriority());
        response.setAssignedDepartment(analysis.getAssignedDepartment());
        response.setSummary(analysis.getShortSummary());
        response.setConfidence(analysis.getConfidenceScore());
        response.setAnalyzedAt(analysis.getAnalyzedAt());
        response.setModelName(analysis.getModelName());
        response.setPromptVersion(analysis.getPromptVersion());
        response.setIsCurrent(analysis.isIsCurrent());
        response.setCreatedAt(analysis.getCreatedAt());
        return response;
    }
}
