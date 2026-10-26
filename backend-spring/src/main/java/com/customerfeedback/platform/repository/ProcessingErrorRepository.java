package com.customerfeedback.platform.repository;

import com.customerfeedback.platform.entity.ProcessingError;
import com.customerfeedback.platform.model.enums.ProcessingStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessingErrorRepository extends JpaRepository<ProcessingError, UUID> {

    Page<ProcessingError> findAllByBatchIdOrderByCsvRowNumberAsc(UUID batchId, Pageable pageable);

    Page<ProcessingError> findAllByBatchIdAndProcessingStageOrderByCsvRowNumberAsc(
            UUID batchId, ProcessingStage processingStage, Pageable pageable);

    Page<ProcessingError> findAllByBatchIdAndErrorCodeOrderByCsvRowNumberAsc(
            UUID batchId, String errorCode, Pageable pageable);

    Page<ProcessingError> findAllByBatchIdAndProcessingStageAndErrorCodeOrderByCsvRowNumberAsc(
            UUID batchId, ProcessingStage processingStage, String errorCode, Pageable pageable);

    long countByBatchId(UUID batchId);
}
