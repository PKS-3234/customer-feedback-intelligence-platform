package com.customerfeedback.platform.dto;

import java.util.List;

public record DashboardSemanticSummary(
        long totalEmbeddings,
        List<DashboardEmbeddingModel> models,
        Integer vectorDimension,
        String status,
        String statusMessage
) {}
