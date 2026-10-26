package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.FeedbackResponse;
import com.customerfeedback.platform.dto.PagedResponse;
import com.customerfeedback.platform.entity.Feedback;
import com.customerfeedback.platform.exception.ResourceNotFoundException;
import com.customerfeedback.platform.model.enums.ProcessingStatus;
import com.customerfeedback.platform.repository.FeedbackRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FeedbackQueryService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackQueryService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * Queries feedback records with dynamic filters and pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<FeedbackResponse> queryFeedback(
            UUID batchId,
            String channel,
            String product,
            Integer rating,
            LocalDate dateFrom,
            LocalDate dateTo,
            ProcessingStatus processingStatus,
            String keyword,
            Pageable pageable) {

        Specification<Feedback> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (batchId != null) {
                predicates.add(cb.equal(root.get("batchId"), batchId));
            }
            if (channel != null && !channel.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("channel")), channel.trim().toLowerCase()));
            }
            if (product != null && !product.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("product")), product.trim().toLowerCase()));
            }
            if (rating != null) {
                predicates.add(cb.equal(root.get("rating"), rating));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("feedbackDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("feedbackDate"), dateTo));
            }
            if (processingStatus != null) {
                predicates.add(cb.equal(root.get("processingStatus"), processingStatus));
            }
            if (keyword != null && !keyword.isBlank()) {
                String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("feedbackText")), searchPattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Feedback> page = feedbackRepository.findAll(spec, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    /**
     * Retrieves single feedback record by UUID.
     */
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackById(UUID feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with ID: " + feedbackId));
        return toResponse(feedback);
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getFeedbackId(),
                feedback.getBatchId(),
                feedback.getExternalReference(),
                feedback.getCustomerReference(),
                feedback.getFeedbackText(),
                feedback.getFeedbackDate(),
                feedback.getChannel(),
                feedback.getRating(),
                feedback.getProduct(),
                feedback.getRegion(),
                feedback.getCustomerSegment(),
                feedback.getLanguage(),
                feedback.getReceivedAt(),
                feedback.getProcessingStatus(),
                feedback.getDeduplicationHash()
        );
    }
}
