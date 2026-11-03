package com.customerfeedback.platform.dto;

public record DashboardEmbeddingModel(
        String model,
        Integer dimension,
        long count
) {}
