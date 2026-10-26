package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.FeedbackResponse;
import com.customerfeedback.platform.dto.PagedResponse;
import com.customerfeedback.platform.model.enums.ProcessingStatus;
import com.customerfeedback.platform.service.FeedbackQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feedback")
@Tag(name = "Feedback", description = "Endpoints for inspecting ingested raw feedback records and verification.")
public class FeedbackController {

    private final FeedbackQueryService queryService;

    public FeedbackController(FeedbackQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "List raw feedback", description = "Retrieves paginated feedback records with filtering by batch, channel, product, rating, and date.")
    public ResponseEntity<PagedResponse<FeedbackResponse>> listFeedback(
            @RequestParam(value = "batchId", required = false) UUID batchId,
            @RequestParam(value = "channel", required = false) String channel,
            @RequestParam(value = "product", required = false) String product,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(value = "processingStatus", required = false) ProcessingStatus processingStatus,
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 20, sort = "feedbackDate", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(queryService.queryFeedback(
                batchId, channel, product, rating, dateFrom, dateTo, processingStatus, keyword, pageable));
    }

    @GetMapping("/{feedbackId}")
    @Operation(summary = "Get feedback by ID", description = "Retrieves a single raw feedback record and its ingestion metadata.")
    @ApiResponse(responseCode = "200", description = "Feedback found")
    @ApiResponse(responseCode = "404", description = "Feedback not found")
    public ResponseEntity<FeedbackResponse> getFeedback(
            @PathVariable("feedbackId") UUID feedbackId) {
        return ResponseEntity.ok(queryService.getFeedbackById(feedbackId));
    }
}
