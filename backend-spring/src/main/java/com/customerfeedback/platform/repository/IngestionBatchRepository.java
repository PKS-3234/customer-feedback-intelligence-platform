package com.customerfeedback.platform.repository;

import com.customerfeedback.platform.entity.IngestionBatch;
import com.customerfeedback.platform.model.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IngestionBatchRepository extends JpaRepository<IngestionBatch, UUID> {

    Optional<IngestionBatch> findFirstByFileHashOrderByUploadedAtDesc(String fileHash);

    Page<IngestionBatch> findAllByStatus(BatchStatus status, Pageable pageable);

    Page<IngestionBatch> findAllByOrderByUploadedAtDesc(Pageable pageable);
}
