package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.DashboardAgentStatus;
import com.customerfeedback.platform.dto.DashboardMetricsResponse;
import com.customerfeedback.platform.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Executive Dashboard", description = "Read-only platform metrics and agent configuration for the dashboard.")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Get executive dashboard metrics and chart data")
    public ResponseEntity<DashboardMetricsResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getMetrics());
    }

    @GetMapping("/agents")
    @Operation(summary = "Get configured agent status and purpose")
    public ResponseEntity<List<DashboardAgentStatus>> getAgentStatuses() {
        return ResponseEntity.ok(dashboardService.getAgentStatuses());
    }
}
