package com.customerfeedback.platform.dto;

import java.time.Instant;
import java.util.UUID;

public record StoredEmbeddingResponse(
        UUID embeddingId,
        UUID feedbackId,
        String embeddingModel,
        int embeddingDimension,
        boolean isCurrent,
        Instant createdAt
) {}
