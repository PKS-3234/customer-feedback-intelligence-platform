package com.customerfeedback.platform.exception;

public class FileValidationException extends RuntimeException {

    private final String errorCode;

    public FileValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
