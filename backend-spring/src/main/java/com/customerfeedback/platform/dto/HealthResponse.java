package com.customerfeedback.platform.dto;

import java.time.Instant;

public record HealthResponse(
        String status,
        String service,
        String version,
        Instant timestamp
) {}
