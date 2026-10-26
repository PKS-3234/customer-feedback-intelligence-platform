package com.customerfeedback.platform.entity;

import com.customerfeedback.platform.model.enums.ProcessingStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feedback_id", updatable = false, nullable = false)
    private UUID feedbackId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "external_reference", nullable = false, length = 100)
    private String externalReference;

    @Column(name = "customer_reference", length = 100)
    private String customerReference;

    @Column(name = "feedback_text", nullable = false, columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "feedback_date", nullable = false)
    private LocalDate feedbackDate;

    @Column(name = "channel", nullable = false, length = 50)
    private String channel;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "product", length = 100)
    private String product;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "customer_segment", length = 100)
    private String customerSegment;

    @Column(name = "language", length = 20)
    private String language;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 50)
    private ProcessingStatus processingStatus = ProcessingStatus.AI_PENDING;

    @Column(name = "deduplication_hash", nullable = false, length = 64)
    private String deduplicationHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Feedback() {
    }

    public Feedback(UUID batchId, String externalReference, String customerReference,
                    String feedbackText, LocalDate feedbackDate, String channel,
                    Integer rating, String product, String region, String customerSegment,
                    String language, String deduplicationHash) {
        this.batchId = batchId;
        this.externalReference = externalReference;
        this.customerReference = customerReference;
        this.feedbackText = feedbackText;
        this.feedbackDate = feedbackDate;
        this.channel = channel;
        this.rating = rating;
        this.product = product;
        this.region = region;
        this.customerSegment = customerSegment;
        this.language = language;
        this.deduplicationHash = deduplicationHash;
        this.receivedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.processingStatus = ProcessingStatus.AI_PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(UUID feedbackId) {
        this.feedbackId = feedbackId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public void setCustomerReference(String customerReference) {
        this.customerReference = customerReference;
    }

    public String getFeedbackText() {
        return feedbackText;
    }

    public void setFeedbackText(String feedbackText) {
        this.feedbackText = feedbackText;
    }

    public LocalDate getFeedbackDate() {
        return feedbackDate;
    }

    public void setFeedbackDate(LocalDate feedbackDate) {
        this.feedbackDate = feedbackDate;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCustomerSegment() {
        return customerSegment;
    }

    public void setCustomerSegment(String customerSegment) {
        this.customerSegment = customerSegment;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getDeduplicationHash() {
        return deduplicationHash;
    }

    public void setDeduplicationHash(String deduplicationHash) {
        this.deduplicationHash = deduplicationHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
