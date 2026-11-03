package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.AgentQueryResponse;
import com.customerfeedback.platform.dto.SupportingEvidence;
import com.customerfeedback.platform.service.AiAgentClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiAgentClient aiAgentClient;

    @Test
    void shouldProxyQuestionAndReturnAgentsAndEvidence() throws Exception {
        when(aiAgentClient.query(any())).thenReturn(new AgentQueryResponse(
                "Customers report duplicate charges [feedback-1].",
                List.of("Supervisor Agent", "Retrieval Agent", "Reporting Agent"),
                List.of(new SupportingEvidence(
                        "feedback-1",
                        "feedback",
                        "Similar feedback",
                        "Card charged twice.",
                        Map.of("similarity", 0.94)
                ))
        ));

        mockMvc.perform(post("/api/v1/agents/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"Find duplicate charge reports","limit":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", is("Customers report duplicate charges [feedback-1].")))
                .andExpect(jsonPath("$.agentsUsed[1]", is("Retrieval Agent")))
                .andExpect(jsonPath("$.supportingEvidence[0].content", is("Card charged twice.")));

        verify(aiAgentClient).query(any());
    }

    @Test
    void shouldRejectInvalidAgentQuestion() throws Exception {
        mockMvc.perform(post("/api/v1/agents/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":" "}
                                """))
                .andExpect(status().isBadRequest());
    }
}
