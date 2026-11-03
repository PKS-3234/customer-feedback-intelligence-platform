package com.customerfeedback.platform.dto;

import java.util.List;

public record DashboardMetricsResponse(
        long totalFeedbackRecords,
        long totalAnalyzedRecords,
        long totalEmbeddedRecords,
        double positiveFeedbackPercent,
        double negativeFeedbackPercent,
        long highPriorityFeedbackCount,
        long currentBatches,
        long processingErrors,
        long unresolvedProcessingErrors,
        List<DashboardChartDatum> sentimentDistribution,
        List<DashboardChartDatum> categoryDistribution,
        List<DashboardChartDatum> themeDistribution,
        List<DashboardChartDatum> priorityDistribution,
        List<DashboardChartDatum> feedbackByChannel,
        List<DashboardChartDatum> feedbackByProduct,
        DashboardSemanticSummary semanticSearch
) {}
