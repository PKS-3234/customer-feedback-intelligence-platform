package com.customerfeedback.platform.exception;

import java.util.UUID;

public class DuplicateFileException extends RuntimeException {

    private final UUID existingBatchId;

    public DuplicateFileException(String message, UUID existingBatchId) {
        super(message);
        this.existingBatchId = existingBatchId;
    }

    public UUID getExistingBatchId() {
        return existingBatchId;
    }
}
