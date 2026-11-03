package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.AgentQueryRequest;
import com.customerfeedback.platform.dto.AgentQueryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class AiAgentClient {

    private final RestClient restClient;

    public AiAgentClient(
            RestClient.Builder builder,
            @Value("${app.ai-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public AgentQueryResponse query(AgentQueryRequest request) {
        try {
            return restClient.post()
                    .uri("/api/v1/agents/query")
                    .body(Map.of(
                            "query", request.query(),
                            "limit", request.limit() == null ? 5 : request.limit()
                    ))
                    .retrieve()
                    .body(AgentQueryResponse.class);
        } catch (RestClientResponseException ex) {
            throw new AiServiceException(ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new AiServiceException(502, ex);
        }
    }
}
