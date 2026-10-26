package com.customerfeedback.platform.dto;

import com.customerfeedback.platform.model.enums.ProcessingStage;

import java.time.Instant;
import java.util.UUID;

public record ProcessingErrorResponse(
        UUID errorId,
        UUID batchId,
        UUID feedbackId,
        Integer csvRowNumber,
        ProcessingStage processingStage,
        String errorCode,
        String errorMessage,
        String originalRow,
        Instant createdAt,
        boolean resolved,
        Instant resolvedAt
) {}
