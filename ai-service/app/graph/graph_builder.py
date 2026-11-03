from typing import Any, Dict, List, Optional

from langgraph.graph import END, START, StateGraph
from langgraph.graph.state import CompiledStateGraph

from app.agents.analytics_agent import AnalyticsAgent
from app.agents.data_quality_agent import DataQualityAgent
from app.agents.reporting_agent import ReportingAgent
from app.agents.retrieval_agent import RetrievalAgent
from app.graph.graph_state import AgentGraphState
from app.nodes.analytics_node import AnalyticsNode
from app.nodes.quality_node import QualityNode
from app.nodes.reporting_node import ReportingNode
from app.nodes.retrieval_node import RetrievalNode
from app.nodes.routing_node import RoutingNode, route_next_nodes

NODE_ROUTING = "routing_node"
NODE_ANALYTICS = "analytics_node"
NODE_RETRIEVAL = "retrieval_node"
NODE_QUALITY = "quality_node"
NODE_REPORTING = "reporting_node"

REGISTERED_NODE_NAMES = [
    NODE_ROUTING,
    NODE_ANALYTICS,
    NODE_RETRIEVAL,
    NODE_QUALITY,
    NODE_REPORTING,
]


def build_feedback_graph(
    analytics_agent: Optional[AnalyticsAgent] = None,
    retrieval_agent: Optional[RetrievalAgent] = None,
    data_quality_agent: Optional[DataQualityAgent] = None,
    reporting_agent: Optional[ReportingAgent] = None,
) -> CompiledStateGraph:
    """
    Builds and compiles the LangGraph StateGraph workflow for customer feedback intelligence.
    Supports dependency injection for agents for modularity and testing.
    """
    builder = StateGraph(AgentGraphState)

    # 1. Register nodes
    builder.add_node(NODE_ROUTING, RoutingNode())
    builder.add_node(NODE_ANALYTICS, AnalyticsNode(analytics_agent))
    builder.add_node(NODE_RETRIEVAL, RetrievalNode(retrieval_agent))
    builder.add_node(NODE_QUALITY, QualityNode(data_quality_agent))
    builder.add_node(NODE_REPORTING, ReportingNode(reporting_agent))

    # 2. Add Start Edge
    builder.add_edge(START, NODE_ROUTING)

    # 3. Add Conditional Routing Edges
    builder.add_conditional_edges(
        NODE_ROUTING,
        route_next_nodes,
        [NODE_ANALYTICS, NODE_RETRIEVAL, NODE_QUALITY, NODE_REPORTING],
    )

    # 4. Fan-in edges to Reporting Node
    builder.add_edge(NODE_ANALYTICS, NODE_REPORTING)
    builder.add_edge(NODE_RETRIEVAL, NODE_REPORTING)
    builder.add_edge(NODE_QUALITY, NODE_REPORTING)

    # 5. Finish edge from Reporting Node to END
    builder.add_edge(NODE_REPORTING, END)

    return builder.compile()


def get_graph_metadata(graph: Optional[CompiledStateGraph] = None) -> Dict[str, Any]:
    """
    Extracts structural metadata and visualization of the LangGraph StateGraph.
    """
    active_graph = graph or build_feedback_graph()
    try:
        drawable = active_graph.get_graph()
        mermaid_code = drawable.draw_mermaid()
        raw_edges = [
            {
                "source": edge.source,
                "target": edge.target,
                "conditional": bool(edge.conditional),
            }
            for edge in drawable.edges
        ]
    except Exception:
        mermaid_code = (
            "graph TD;\n"
            "  START([START]) --> routing_node;\n"
            "  routing_node -.-> analytics_node;\n"
            "  routing_node -.-> retrieval_node;\n"
            "  routing_node -.-> quality_node;\n"
            "  routing_node -.-> reporting_node;\n"
            "  analytics_node --> reporting_node;\n"
            "  retrieval_node --> reporting_node;\n"
            "  quality_node --> reporting_node;\n"
            "  reporting_node --> END([END]);"
        )
        raw_edges = [
            {"source": "__start__", "target": NODE_ROUTING, "conditional": False},
            {"source": NODE_ROUTING, "target": NODE_ANALYTICS, "conditional": True},
            {"source": NODE_ROUTING, "target": NODE_RETRIEVAL, "conditional": True},
            {"source": NODE_ROUTING, "target": NODE_QUALITY, "conditional": True},
            {"source": NODE_ROUTING, "target": NODE_REPORTING, "conditional": True},
            {"source": NODE_ANALYTICS, "target": NODE_REPORTING, "conditional": False},
            {"source": NODE_RETRIEVAL, "target": NODE_REPORTING, "conditional": False},
            {"source": NODE_QUALITY, "target": NODE_REPORTING, "conditional": False},
            {"source": NODE_REPORTING, "target": "__end__", "conditional": False},
        ]

    nodes_info = [
        {"id": NODE_ROUTING, "label": "Routing Node (Supervisor)", "type": "router"},
        {"id": NODE_ANALYTICS, "label": "Analytics Node", "type": "specialist_agent"},
        {"id": NODE_RETRIEVAL, "label": "Retrieval Node", "type": "specialist_agent"},
        {"id": NODE_QUALITY, "label": "Quality Node", "type": "specialist_agent"},
        {"id": NODE_REPORTING, "label": "Reporting Node", "type": "synthesis_agent"},
    ]

    return {
        "node_names": REGISTERED_NODE_NAMES,
        "nodes": nodes_info,
        "edges": raw_edges,
        "mermaid": mermaid_code,
    }
