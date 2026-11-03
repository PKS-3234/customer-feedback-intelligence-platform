package com.customerfeedback.platform.dto;

import java.util.List;

public record AgentQueryResponse(
        String answer,
        List<String> agentsUsed,
        List<SupportingEvidence> supportingEvidence
) {}
