from typing import List, Optional

from app.models.agents import SupportingEvidence
from app.service.embedding_service import FeedbackEmbeddingService


class RetrievalAgent:
    name = "Retrieval Agent"

    def __init__(self, service: Optional[FeedbackEmbeddingService] = None):
        self.service = service or FeedbackEmbeddingService()

    def run(self, query: str, limit: int) -> List[SupportingEvidence]:
        results = self.service.search(query, limit)
        return [
            SupportingEvidence(
                evidenceId=f"feedback-{result.feedbackId}",
                source="feedback + feedback_analysis + feedback_embeddings",
                title=f"Similar feedback ({result.similarity:.0%} similarity)",
                content=result.feedbackText,
                metadata={
                    "feedbackId": str(result.feedbackId),
                    "batchId": str(result.batchId),
                    "similarity": result.similarity,
                    "sentiment": result.sentiment,
                    "category": result.category,
                    "theme": result.theme,
                    "emotion": result.emotion,
                    "priority": result.priority,
                    "summary": result.summary,
                },
            )
            for result in results
        ]
