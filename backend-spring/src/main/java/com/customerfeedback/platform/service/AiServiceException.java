package com.customerfeedback.platform.service;

public class AiServiceException extends RuntimeException {

    private final int upstreamStatus;

    public AiServiceException(int upstreamStatus, Throwable cause) {
        super("AI analysis service request failed.", cause);
        this.upstreamStatus = upstreamStatus;
    }

    public int getUpstreamStatus() {
        return upstreamStatus;
    }
}
