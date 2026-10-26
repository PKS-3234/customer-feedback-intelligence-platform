package com.customerfeedback.platform.repository;

import com.customerfeedback.platform.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID>, JpaSpecificationExecutor<Feedback> {

    boolean existsByDeduplicationHash(String deduplicationHash);

    Optional<Feedback> findByDeduplicationHash(String deduplicationHash);

    Page<Feedback> findAllByBatchId(UUID batchId, Pageable pageable);

    long countByBatchId(UUID batchId);
}
