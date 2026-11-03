package com.customerfeedback.platform.dto;

/**
 * DTO representing the summary result of a batch AI analysis run,
 * mirroring the JSON produced by FastAPI's {@code BatchAnalysisSummaryResponse}.
 */
public class BatchAnalysisSummaryResponse {

    private String batchId;
    private int processed;
    private int successful;
    private int failed;
    private double durationSeconds;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public BatchAnalysisSummaryResponse() {}

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public int getProcessed() {
        return processed;
    }

    public void setProcessed(int processed) {
        this.processed = processed;
    }

    public int getSuccessful() {
        return successful;
    }

    public void setSuccessful(int successful) {
        this.successful = successful;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
