package com.customerfeedback.platform.dto;

import com.customerfeedback.platform.model.enums.ProcessingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FeedbackResponse(
        UUID feedbackId,
        UUID batchId,
        String externalReference,
        String customerReference,
        String feedbackText,
        LocalDate feedbackDate,
        String channel,
        Integer rating,
        String product,
        String region,
        String customerSegment,
        String language,
        Instant receivedAt,
        ProcessingStatus processingStatus,
        String deduplicationHash
) {}
