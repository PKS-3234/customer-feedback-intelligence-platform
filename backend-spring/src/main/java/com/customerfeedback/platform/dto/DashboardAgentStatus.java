package com.customerfeedback.platform.dto;

import java.time.Instant;

public record DashboardAgentStatus(
        String agent,
        String purpose,
        String availability,
        Instant lastExecution
) {}
