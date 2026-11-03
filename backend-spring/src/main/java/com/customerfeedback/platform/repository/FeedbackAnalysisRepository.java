package com.customerfeedback.platform.repository;

import com.customerfeedback.platform.entity.FeedbackAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * Returns all analysis versions for a feedback record, newest first.
     */
    List<FeedbackAnalysis> findByFeedbackIdOrderByCreatedAtDesc(UUID feedbackId);
}
