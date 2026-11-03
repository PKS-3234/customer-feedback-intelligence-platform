package com.customerfeedback.platform.exception;

import com.customerfeedback.platform.dto.ApiErrorResponse;
import com.customerfeedback.platform.service.AiServiceException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleAiServiceFailure(
            AiServiceException ex, HttpServletRequest request) {
        HttpStatus status = ex.getUpstreamStatus() == HttpStatus.NOT_FOUND.value()
                ? HttpStatus.NOT_FOUND
                : ex.getUpstreamStatus() == HttpStatus.BAD_REQUEST.value()
                        ? HttpStatus.BAD_REQUEST
                        : HttpStatus.BAD_GATEWAY;
        String correlationId = UUID.randomUUID().toString();
        log.error("[{}] AI service request failed with upstream status {}",
                correlationId, ex.getUpstreamStatus(), ex);

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                "AI_SERVICE_FAILURE",
                "The AI analysis service could not complete the request.",
                request.getRequestURI(),
                correlationId,
                null
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Resource not found: {}", correlationId, ex.getMessage());

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI(),
                correlationId,
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleFileValidation(FileValidationException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] File validation error ({}): {}", correlationId, ex.getErrorCode(), ex.getMessage());

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                correlationId,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DuplicateFileException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateFile(DuplicateFileException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Duplicate file upload attempt: {}", correlationId, ex.getMessage());

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "DUPLICATE_FILE",
                ex.getMessage(),
                request.getRequestURI(),
                correlationId,
                Map.of("existingBatchId", ex.getExistingBatchId() != null ? ex.getExistingBatchId().toString() : "")
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] File size exceeded limit: {}", correlationId, ex.getMessage());

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "FILE_TOO_LARGE",
                "Uploaded file exceeds the maximum allowed size limit.",
                request.getRequestURI(),
                correlationId,
                null
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Validation failed for one or more request fields.",
                request.getRequestURI(),
                correlationId,
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] Bad request: {}", correlationId, ex.getMessage());

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_INPUT",
                ex.getMessage(),
                request.getRequestURI(),
                correlationId,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[{}] Internal server error on path {}: {}", correlationId, request.getRequestURI(), ex.getMessage(), ex);

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected server error occurred. Please contact system administrator with correlation ID.",
                request.getRequestURI(),
                correlationId,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
