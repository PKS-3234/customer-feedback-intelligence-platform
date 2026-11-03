import logging
from typing import Optional

from fastapi import APIRouter, HTTPException, Query, status

from app.agents.supervisor_agent import SupervisorAgent
from app.models.agents import (
    AgentQueryRequest,
    AgentQueryResponse,
    WorkflowVisibilityResponse,
)
from app.workflow.langgraph_workflow import LangGraphWorkflow

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


@router.get(
    "/agents/workflow",
    response_model=WorkflowVisibilityResponse,
    summary="Get LangGraph workflow structure, node names, and execution path",
)
def get_agent_workflow(
    query: Optional[str] = Query(
        None,
        description="Optional question to evaluate specific execution path",
    ),
) -> WorkflowVisibilityResponse:
    try:
        workflow = LangGraphWorkflow()
        info = workflow.get_workflow_info(query=query)
        return WorkflowVisibilityResponse(**info)
    except Exception as ex:
        logger.error("Failed to retrieve agent workflow visibility.", exc_info=ex)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to retrieve agent workflow visibility.",
        ) from ex
