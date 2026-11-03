package com.customerfeedback.platform.dto;

import java.util.UUID;

public record SemanticSearchResult(
        UUID feedbackId,
        UUID batchId,
        String feedbackText,
        String sentiment,
        String category,
        String theme,
        String emotion,
        String priority,
        String summary,
        double similarity
) {}
