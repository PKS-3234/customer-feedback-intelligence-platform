import logging

from fastapi import APIRouter, HTTPException, status

from app.agents.supervisor_agent import SupervisorAgent
from app.models.agents import AgentQueryRequest, AgentQueryResponse

logger = logging.getLogger(__name__)

router = APIRouter(prefix="", tags=["Agents"])


@router.post(
    "/agents/query",
    response_model=AgentQueryResponse,
    summary="Route a feedback question through the bounded agent workflow",
)
def query_agents(request: AgentQueryRequest) -> AgentQueryResponse:
    try:
        return SupervisorAgent().execute(request)
    except Exception as ex:
        logger.error("Agent query failed.", exc_info=ex)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Agent query failed.",
        ) from ex
