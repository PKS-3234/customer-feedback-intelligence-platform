from typing import List

from app.core import database
from app.models.agents import SupportingEvidence


class DataQualityAgent:
    name = "Data Quality Agent"

    def run(self) -> List[SupportingEvidence]:
        metrics = database.get_data_quality_metrics()
        return [
            SupportingEvidence(
                evidenceId="quality-coverage",
                source="feedback + feedback_analysis + feedback_embeddings",
                title="Analysis and embedding coverage",
                content=(
                    f"{metrics['feedbackTotal']} feedback records; "
                    f"{metrics['analyzedTotal']} currently analyzed; "
                    f"{metrics['embeddedTotal']} have a current embedding for "
                    "their current analysis."
                ),
                metadata=metrics,
            ),
            SupportingEvidence(
                evidenceId="quality-processing",
                source="feedback + processing_errors",
                title="Processing status and errors",
                content=(
                    f"{metrics['failedFeedbackTotal']} feedback records are marked FAILED; "
                    f"{metrics['processingErrorTotal']} processing errors are recorded."
                ),
                metadata={
                    "failedFeedbackTotal": metrics["failedFeedbackTotal"],
                    "processingErrorTotal": metrics["processingErrorTotal"],
                },
            ),
        ]
