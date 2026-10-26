package com.customerfeedback.platform.dto;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String correlationId,
        Map<String, String> fieldErrors
) {}
