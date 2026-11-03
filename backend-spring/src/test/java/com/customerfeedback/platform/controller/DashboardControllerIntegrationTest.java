package com.customerfeedback.platform.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnDatabaseBackedDashboardMetricsAndChartData() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFeedbackRecords").isNumber())
                .andExpect(jsonPath("$.totalAnalyzedRecords").isNumber())
                .andExpect(jsonPath("$.totalEmbeddedRecords").isNumber())
                .andExpect(jsonPath("$.positiveFeedbackPercent").isNumber())
                .andExpect(jsonPath("$.negativeFeedbackPercent").isNumber())
                .andExpect(jsonPath("$.highPriorityFeedbackCount").isNumber())
                .andExpect(jsonPath("$.currentBatches").isNumber())
                .andExpect(jsonPath("$.processingErrors").isNumber())
                .andExpect(jsonPath("$.unresolvedProcessingErrors").isNumber())
                .andExpect(jsonPath("$.sentimentDistribution").isArray())
                .andExpect(jsonPath("$.categoryDistribution").isArray())
                .andExpect(jsonPath("$.themeDistribution").isArray())
                .andExpect(jsonPath("$.priorityDistribution").isArray())
                .andExpect(jsonPath("$.feedbackByChannel").isArray())
                .andExpect(jsonPath("$.feedbackByProduct").isArray())
                .andExpect(jsonPath("$.semanticSearch.totalEmbeddings").isNumber())
                .andExpect(jsonPath("$.semanticSearch.models").isArray())
                .andExpect(jsonPath("$.semanticSearch.status", notNullValue()));
    }

    @Test
    void shouldReturnAllConfiguredAgentStatusesWithoutInventingExecutionTimes() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/agents").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].agent", is("Supervisor Agent")))
                .andExpect(jsonPath("$[0].availability", is("Configured")))
                .andExpect(jsonPath("$[0].lastExecution").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[1].agent", is("Analytics Agent")))
                .andExpect(jsonPath("$[2].agent", is("Retrieval Agent")))
                .andExpect(jsonPath("$[3].agent", is("Data Quality Agent")))
                .andExpect(jsonPath("$[4].agent", is("Reporting Agent")));
    }
}
