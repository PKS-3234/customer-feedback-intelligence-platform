package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.BatchEmbeddingSummaryResponse;
import com.customerfeedback.platform.dto.SemanticSearchRequest;
import com.customerfeedback.platform.dto.SemanticSearchResult;
import com.customerfeedback.platform.dto.StoredEmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AiEmbeddingClient {

    private final RestClient restClient;

    public AiEmbeddingClient(
            RestClient.Builder builder,
            @Value("${app.ai-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public StoredEmbeddingResponse embedFeedback(UUID feedbackId) {
        return execute(() -> restClient.post()
                .uri("/api/v1/embeddings/feedback/{feedbackId}", feedbackId)
                .retrieve()
                .body(StoredEmbeddingResponse.class));
    }

    public StoredEmbeddingResponse getFeedbackEmbedding(UUID feedbackId) {
        return execute(() -> restClient.get()
                .uri("/api/v1/embeddings/feedback/{feedbackId}", feedbackId)
                .retrieve()
                .body(StoredEmbeddingResponse.class));
    }

    public BatchEmbeddingSummaryResponse embedBatch(UUID batchId, Integer limit) {
        return execute(() -> restClient.post()
                .uri(uriBuilder -> {
                    var uri = uriBuilder.path("/api/v1/embeddings/batches/{batchId}");
                    if (limit != null) {
                        uri.queryParam("limit", limit);
                    }
                    return uri.build(batchId);
                })
                .retrieve()
                .body(BatchEmbeddingSummaryResponse.class));
    }

    public List<SemanticSearchResult> semanticSearch(SemanticSearchRequest request) {
        return execute(() -> restClient.post()
                .uri("/api/v1/semantic-search")
                .body(Map.of(
                        "query", request.query(),
                        "limit", request.limit() == null ? 10 : request.limit()
                ))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {}));
    }

    private static <T> T execute(java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException ex) {
            throw new AiServiceException(ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new AiServiceException(502, ex);
        }
    }
}
