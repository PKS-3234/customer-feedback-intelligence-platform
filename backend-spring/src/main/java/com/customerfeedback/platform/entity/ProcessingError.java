package com.customerfeedback.platform.entity;

import com.customerfeedback.platform.model.enums.ProcessingStage;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processing_errors")
public class ProcessingError {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "error_id", updatable = false, nullable = false)
    private UUID errorId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "feedback_id")
    private UUID feedbackId;

    @Column(name = "csv_row_number")
    private Integer csvRowNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_stage", nullable = false, length = 50)
    private ProcessingStage processingStage;

    @Column(name = "error_code", nullable = false, length = 100)
    private String errorCode;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "original_row", columnDefinition = "jsonb")
    private String originalRow;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public ProcessingError() {
    }

    public ProcessingError(UUID batchId, UUID feedbackId, Integer csvRowNumber,
                           ProcessingStage processingStage, String errorCode,
                           String errorMessage, String originalRow) {
        this.batchId = batchId;
        this.feedbackId = feedbackId;
        this.csvRowNumber = csvRowNumber;
        this.processingStage = processingStage;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.originalRow = originalRow;
        this.createdAt = Instant.now();
        this.resolved = false;
    }

    // Getters and Setters
    public UUID getErrorId() {
        return errorId;
    }

    public void setErrorId(UUID errorId) {
        this.errorId = errorId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public UUID getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(UUID feedbackId) {
        this.feedbackId = feedbackId;
    }

    public Integer getCsvRowNumber() {
        return csvRowNumber;
    }

    public void setCsvRowNumber(Integer csvRowNumber) {
        this.csvRowNumber = csvRowNumber;
    }

    public ProcessingStage getProcessingStage() {
        return processingStage;
    }

    public void setProcessingStage(ProcessingStage processingStage) {
        this.processingStage = processingStage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getOriginalRow() {
        return originalRow;
    }

    public void setOriginalRow(String originalRow) {
        this.originalRow = originalRow;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
