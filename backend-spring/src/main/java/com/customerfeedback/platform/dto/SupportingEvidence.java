package com.customerfeedback.platform.dto;

import java.util.Map;

public record SupportingEvidence(
        String evidenceId,
        String source,
        String title,
        String content,
        Map<String, Object> metadata
) {}
