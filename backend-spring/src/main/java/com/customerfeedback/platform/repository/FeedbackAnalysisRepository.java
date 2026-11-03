package com.customerfeedback.platform.repository;

import com.customerfeedback.platform.entity.FeedbackAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link FeedbackAnalysis}.
 * <p>
 * Versioning note: Only one record per feedbackId should have {@code isCurrent=true}.
 * Older records remain with {@code isCurrent=false} for audit purposes.
 * </p>
 */
public interface FeedbackAnalysisRepository extends JpaRepository<FeedbackAnalysis, UUID> {

    /**
     * Returns the active (most-recent) analysis for a given feedback record.
     */
    Optional<FeedbackAnalysis> findByFeedbackIdAndIsCurrent(UUID feedbackId, boolean isCurrent);

    /**
     * Returns current analysis results for all feedback in a batch.
     */
    @Query(value = """
            SELECT analysis.*
            FROM feedback_analysis analysis
            JOIN feedback ON feedback.feedback_id = analysis.feedback_id
            WHERE feedback.batch_id = :batchId
              AND analysis.is_current = true
            ORDER BY analysis.analyzed_at DESC
            """, nativeQuery = true)
    List<FeedbackAnalysis> findCurrentByBatchId(@Param("batchId") UUID batchId);

    /**
     * Returns all analysis versions for a feedback record, newest first.
     */
    List<FeedbackAnalysis> findByFeedbackIdOrderByCreatedAtDesc(UUID feedbackId);
}
