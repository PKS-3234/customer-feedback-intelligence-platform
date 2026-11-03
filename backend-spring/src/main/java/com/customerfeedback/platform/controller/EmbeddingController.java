package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.BatchEmbeddingSummaryResponse;
import com.customerfeedback.platform.dto.SemanticSearchRequest;
import com.customerfeedback.platform.dto.SemanticSearchResult;
import com.customerfeedback.platform.dto.StoredEmbeddingResponse;
import com.customerfeedback.platform.service.AiEmbeddingClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Embeddings and Search", description = "Generate feedback embeddings and search feedback semantically.")
public class EmbeddingController {

    private final AiEmbeddingClient aiEmbeddingClient;

    public EmbeddingController(AiEmbeddingClient aiEmbeddingClient) {
        this.aiEmbeddingClient = aiEmbeddingClient;
    }

    @PostMapping("/embeddings/feedback/{feedbackId}")
    @Operation(summary = "Generate and persist a feedback embedding")
    public ResponseEntity<StoredEmbeddingResponse> embedFeedback(
            @PathVariable UUID feedbackId) {
        return ResponseEntity.ok(aiEmbeddingClient.embedFeedback(feedbackId));
    }

    @GetMapping("/embeddings/feedback/{feedbackId}")
    @Operation(summary = "Get current feedback embedding metadata")
    public ResponseEntity<StoredEmbeddingResponse> getFeedbackEmbedding(
            @PathVariable UUID feedbackId) {
        return ResponseEntity.ok(aiEmbeddingClient.getFeedbackEmbedding(feedbackId));
    }

    @PostMapping("/embeddings/batches/{batchId}")
    @Operation(summary = "Generate embeddings for analyzed feedback in a batch")
    public ResponseEntity<BatchEmbeddingSummaryResponse> embedBatch(
            @PathVariable UUID batchId,
            @RequestParam(required = false) Integer limit) {
        if (limit != null && limit < 1) {
            throw new IllegalArgumentException("Embedding limit must be greater than zero.");
        }
        return ResponseEntity.ok(aiEmbeddingClient.embedBatch(batchId, limit));
    }

    @PostMapping("/semantic-search")
    @Operation(summary = "Search stored feedback by semantic similarity")
    public ResponseEntity<List<SemanticSearchResult>> semanticSearch(
            @Valid @RequestBody SemanticSearchRequest request) {
        return ResponseEntity.ok(aiEmbeddingClient.semanticSearch(request));
    }
}
