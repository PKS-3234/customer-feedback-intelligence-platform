import logging
from typing import List, Optional

from app.agents.analytics_agent import AnalyticsAgent
from app.agents.data_quality_agent import DataQualityAgent
from app.agents.reporting_agent import ReportingAgent
from app.agents.retrieval_agent import RetrievalAgent
from app.models.agents import AgentQueryRequest, AgentQueryResponse, SupportingEvidence

logger = logging.getLogger(__name__)


class SupervisorAgent:
    name = "Supervisor Agent"

    _ANALYTICS_TERMS = {
        "average", "breakdown", "category", "compare", "count", "how many",
        "most", "percentage", "priority", "sentiment", "total", "trend",
    }
    _RETRIEVAL_TERMS = {
        "comment", "complaint", "example", "find", "issue", "similar",
        "saying", "search", "show",
    }
    _QUALITY_TERMS = {
        "coverage", "data quality", "embedding", "error", "failed", "incomplete",
        "missing", "quality", "status", "unanalysed", "unanalyzed", "unembedded",
    }

    def __init__(
        self,
        analytics_agent: Optional[AnalyticsAgent] = None,
        retrieval_agent: Optional[RetrievalAgent] = None,
        data_quality_agent: Optional[DataQualityAgent] = None,
        reporting_agent: Optional[ReportingAgent] = None,
    ):
        self.analytics_agent = analytics_agent or AnalyticsAgent()
        self.retrieval_agent = retrieval_agent or RetrievalAgent()
        self.data_quality_agent = data_quality_agent or DataQualityAgent()
        self.reporting_agent = reporting_agent or ReportingAgent()

    @classmethod
    def route(cls, query: str) -> List[str]:
        normalized = query.casefold()
        selected = []
        if any(term in normalized for term in cls._ANALYTICS_TERMS):
            selected.append("analytics")
        if any(term in normalized for term in cls._RETRIEVAL_TERMS):
            selected.append("retrieval")
        if any(term in normalized for term in cls._QUALITY_TERMS):
            selected.append("quality")
        if not selected:
            selected.extend(("analytics", "retrieval", "quality"))
        return selected

    def execute(self, request: AgentQueryRequest) -> AgentQueryResponse:
        routed_agents = self.route(request.query)
        evidence: List[SupportingEvidence] = []
        agents_used = [self.name]

        if "analytics" in routed_agents:
            evidence.extend(self.analytics_agent.run())
            agents_used.append(self.analytics_agent.name)
        if "retrieval" in routed_agents:
            evidence.extend(
                self.retrieval_agent.run(request.query, request.limit)
            )
            agents_used.append(self.retrieval_agent.name)
        if "quality" in routed_agents:
            evidence.extend(self.data_quality_agent.run())
            agents_used.append(self.data_quality_agent.name)

        answer = self.reporting_agent.run(request.query, evidence)
        agents_used.append(self.reporting_agent.name)
        logger.info(
            "Supervisor routed query to %s; %d evidence items collected.",
            routed_agents,
            len(evidence),
        )
        return AgentQueryResponse(
            answer=answer,
            agentsUsed=agents_used,
            supportingEvidence=evidence,
        )
