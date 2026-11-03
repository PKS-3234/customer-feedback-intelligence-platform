package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.BatchAnalysisSummaryResponse;
import com.customerfeedback.platform.dto.FeedbackAnalysisResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

/**
 * HTTP client for the internal FastAPI analysis endpoints.
 */
@Component
public class AiAnalysisClient {

    private final RestClient restClient;

    public AiAnalysisClient(
            RestClient.Builder builder,
            @Value("${app.ai-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public FeedbackAnalysisResponse analyzeFeedback(UUID feedbackId) {
        try {
            return restClient.post()
                    .uri("/api/v1/analyze/{feedbackId}", feedbackId)
                    .retrieve()
                    .body(FeedbackAnalysisResponse.class);
        } catch (RestClientResponseException ex) {
            throw new AiServiceException(ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new AiServiceException(502, ex);
        }
    }

    public BatchAnalysisSummaryResponse analyzeBatch(UUID batchId, Integer limit) {
        try {
            return restClient.post()
                    .uri(uriBuilder -> {
                        var uri = uriBuilder.path("/api/v1/analyze/batch/{batchId}");
                        if (limit != null) {
                            uri.queryParam("limit", limit);
                        }
                        return uri.build(batchId);
                    })
                    .retrieve()
                    .body(BatchAnalysisSummaryResponse.class);
        } catch (RestClientResponseException ex) {
            throw new AiServiceException(ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new AiServiceException(502, ex);
        }
    }
}
