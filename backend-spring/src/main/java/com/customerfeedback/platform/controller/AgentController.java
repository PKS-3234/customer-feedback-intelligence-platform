package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.AgentQueryRequest;
import com.customerfeedback.platform.dto.AgentQueryResponse;
import com.customerfeedback.platform.service.AiAgentClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents")
@Tag(name = "Agents", description = "Route feedback questions through the bounded AI agent workflow.")
public class AgentController {

    private final AiAgentClient aiAgentClient;

    public AgentController(AiAgentClient aiAgentClient) {
        this.aiAgentClient = aiAgentClient;
    }

    @PostMapping("/query")
    @Operation(summary = "Query the feedback intelligence agents")
    public ResponseEntity<AgentQueryResponse> query(
            @Valid @RequestBody AgentQueryRequest request) {
        return ResponseEntity.ok(aiAgentClient.query(request));
    }
}
