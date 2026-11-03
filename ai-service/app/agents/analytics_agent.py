from typing import List

from app.core import database
from app.models.agents import SupportingEvidence


class AnalyticsAgent:
    name = "Analytics Agent"

    def run(self) -> List[SupportingEvidence]:
        snapshot = database.get_analytics_snapshot()
        evidence = [
            SupportingEvidence(
                evidenceId="analytics-overview",
                source="feedback + feedback_analysis",
                title="Feedback and analysis totals",
                content=(
                    f"{snapshot['feedbackTotal']} feedback records; "
                    f"{snapshot['analyzedTotal']} have current AI analysis."
                ),
                metadata={
                    "feedbackTotal": snapshot["feedbackTotal"],
                    "analyzedTotal": snapshot["analyzedTotal"],
                },
            )
        ]

        if snapshot["sentiments"]:
            breakdown = ", ".join(
                f"{row['sentiment']}: {row['count']}" for row in snapshot["sentiments"]
            )
            evidence.append(SupportingEvidence(
                evidenceId="analytics-sentiment",
                source="feedback_analysis",
                title="Current sentiment breakdown",
                content=breakdown,
                metadata={"sentiments": snapshot["sentiments"]},
            ))

        if snapshot["categories"]:
            breakdown = ", ".join(
                f"{row['category']}: {row['count']}" for row in snapshot["categories"]
            )
            evidence.append(SupportingEvidence(
                evidenceId="analytics-categories",
                source="feedback_analysis",
                title="Top analyzed feedback categories",
                content=breakdown,
                metadata={"categories": snapshot["categories"]},
            ))

        if snapshot["priorities"]:
            breakdown = ", ".join(
                f"{row['priority']}: {row['count']}" for row in snapshot["priorities"]
            )
            evidence.append(SupportingEvidence(
                evidenceId="analytics-priorities",
                source="feedback_analysis",
                title="Current priority breakdown",
                content=breakdown,
                metadata={"priorities": snapshot["priorities"]},
            ))
        return evidence
