package com.customerfeedback.platform.dto;

public record BatchEmbeddingSummaryResponse(
        String batchId,
        int processed,
        int successful,
        int failed,
        double durationSeconds
) {}
