package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.DashboardAgentStatus;
import com.customerfeedback.platform.dto.DashboardChartDatum;
import com.customerfeedback.platform.dto.DashboardEmbeddingModel;
import com.customerfeedback.platform.dto.DashboardMetricsResponse;
import com.customerfeedback.platform.dto.DashboardSemanticSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final List<DashboardAgentStatus> AGENTS = List.of(
            new DashboardAgentStatus(
                    "Supervisor Agent",
                    "Routes each question once to the relevant bounded analytics, retrieval, and data-quality agents.",
                    "Configured",
                    null
            ),
            new DashboardAgentStatus(
                    "Analytics Agent",
                    "Reads aggregate feedback, sentiment, category, and priority metrics.",
                    "Configured",
                    null
            ),
            new DashboardAgentStatus(
                    "Retrieval Agent",
                    "Retrieves semantically similar feedback using existing PGVector embeddings.",
                    "Configured",
                    null
            ),
            new DashboardAgentStatus(
                    "Data Quality Agent",
                    "Reports analysis and embedding coverage, failed records, and processing errors.",
                    "Configured",
                    null
            ),
            new DashboardAgentStatus(
                    "Reporting Agent",
                    "Produces one evidence-grounded answer from the selected agents' results.",
                    "Configured",
                    null
            )
    );

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardMetricsResponse getMetrics() {
        Map<String, Object> metrics = jdbcTemplate.queryForMap("""
                SELECT
                    (SELECT COUNT(*) FROM feedback) AS feedback_total,
                    (SELECT COUNT(DISTINCT feedback_id)
                       FROM feedback_analysis
                      WHERE is_current = true) AS analyzed_total,
                    (SELECT COUNT(DISTINCT embedding.feedback_id)
                       FROM feedback_embeddings embedding
                       JOIN feedback_analysis analysis
                         ON analysis.analysis_id = embedding.analysis_id
                        AND analysis.feedback_id = embedding.feedback_id
                        AND analysis.is_current = true
                      WHERE embedding.is_current = true) AS embedded_total,
                    (SELECT COUNT(DISTINCT feedback_id)
                       FROM feedback_analysis
                      WHERE is_current = true
                        AND UPPER(TRIM(COALESCE(sentiment, ''))) = 'POSITIVE') AS positive_total,
                    (SELECT COUNT(DISTINCT feedback_id)
                       FROM feedback_analysis
                      WHERE is_current = true
                        AND UPPER(TRIM(COALESCE(sentiment, ''))) = 'NEGATIVE') AS negative_total,
                    (SELECT COUNT(DISTINCT feedback_id)
                       FROM feedback_analysis
                      WHERE is_current = true
                        AND UPPER(TRIM(COALESCE(priority, ''))) = 'HIGH') AS high_priority_total,
                    (SELECT COUNT(*)
                       FROM ingestion_batches
                      WHERE status IN ('RECEIVED', 'VALIDATING', 'VALIDATED', 'IMPORTING')) AS current_batches,
                    (SELECT COUNT(*) FROM processing_errors) AS processing_errors,
                    (SELECT COUNT(*) FROM processing_errors WHERE resolved = false) AS unresolved_errors,
                    EXISTS (
                        SELECT 1 FROM pg_extension WHERE extname = 'vector'
                    ) AS vector_extension_available
                """);

        long feedbackTotal = number(metrics, "feedback_total");
        long analyzedTotal = number(metrics, "analyzed_total");
        long embeddedTotal = number(metrics, "embedded_total");
        long positiveTotal = number(metrics, "positive_total");
        long negativeTotal = number(metrics, "negative_total");
        List<DashboardEmbeddingModel> embeddingModels = getEmbeddingModels();
        boolean vectorAvailable = Boolean.TRUE.equals(metrics.get("vector_extension_available"));
        Integer vectorDimension = getVectorDimension(embeddingModels);
        String searchStatus = vectorAvailable ? "Ready" : "Unavailable";
        String statusMessage = !vectorAvailable
                ? "The PostgreSQL vector extension is not available."
                : embeddedTotal == 0
                        ? "PGVector is ready; there are no current embeddings to search."
                        : "PGVector is ready with current feedback embeddings.";

        return new DashboardMetricsResponse(
                feedbackTotal,
                analyzedTotal,
                embeddedTotal,
                percent(positiveTotal, analyzedTotal),
                percent(negativeTotal, analyzedTotal),
                number(metrics, "high_priority_total"),
                number(metrics, "current_batches"),
                number(metrics, "processing_errors"),
                number(metrics, "unresolved_errors"),
                distribution("feedback_analysis", "sentiment", true),
                distribution("feedback_analysis", "category", true),
                distribution("feedback_analysis", "theme", true),
                distribution("feedback_analysis", "priority", true),
                distribution("feedback", "channel", false),
                distribution("feedback", "product", false),
                new DashboardSemanticSummary(
                        embeddedTotal,
                        embeddingModels,
                        vectorDimension,
                        searchStatus,
                        statusMessage
                )
        );
    }

    public List<DashboardAgentStatus> getAgentStatuses() {
        return AGENTS;
    }

    private List<DashboardEmbeddingModel> getEmbeddingModels() {
        return jdbcTemplate.query("""
                SELECT COALESCE(embedding.embedding_model, 'Unknown') AS model,
                       embedding.embedding_dimension AS dimension,
                       COUNT(DISTINCT embedding.feedback_id) AS count
                  FROM feedback_embeddings embedding
                  JOIN feedback_analysis analysis
                    ON analysis.analysis_id = embedding.analysis_id
                   AND analysis.feedback_id = embedding.feedback_id
                   AND analysis.is_current = true
                 WHERE embedding.is_current = true
                 GROUP BY embedding.embedding_model, embedding.embedding_dimension
                 ORDER BY count DESC, model
                """,
                (result, rowNumber) -> new DashboardEmbeddingModel(
                        result.getString("model"),
                        (Integer) result.getObject("dimension"),
                        result.getLong("count")
                ));
    }

    private Integer getVectorDimension(List<DashboardEmbeddingModel> models) {
        if (models.isEmpty()) {
            return null;
        }
        List<Integer> dimensions = models.stream()
                .map(DashboardEmbeddingModel::dimension)
                .distinct()
                .toList();
        return dimensions.size() == 1 ? dimensions.getFirst() : null;
    }

    private List<DashboardChartDatum> distribution(
            String table,
            String column,
            boolean currentAnalysis) {
        if (!(table.equals("feedback_analysis") || table.equals("feedback"))
                || !(column.equals("sentiment") || column.equals("category")
                || column.equals("theme") || column.equals("priority")
                || column.equals("channel") || column.equals("product"))) {
            throw new IllegalArgumentException("Unsupported dashboard distribution.");
        }

        String sql = "SELECT COALESCE(NULLIF(BTRIM(" + column + "), ''), 'Unspecified') AS label, "
                + "COUNT(DISTINCT feedback_id) AS count FROM " + table
                + (currentAnalysis ? " WHERE is_current = true" : "")
                + " GROUP BY label ORDER BY count DESC, label";
        return jdbcTemplate.query(
                sql,
                (result, rowNumber) -> new DashboardChartDatum(
                        result.getString("label"),
                        result.getLong("count")
                )
        );
    }

    private static long number(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Dashboard query did not return numeric metric '" + key + "'.");
    }

    private static double percent(long count, long total) {
        return total == 0 ? 0.0 : Math.round((count * 1000.0) / total) / 10.0;
    }
}
