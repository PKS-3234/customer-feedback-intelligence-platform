from abc import ABC, abstractmethod
from typing import Optional, Dict, Any

from app.models.analysis import FeedbackAnalysisDTO


class ChatModelProvider(ABC):
    """
    Abstract interface for underlying LLM chat/completion model access.
    """

    @abstractmethod
    def generate(
        self,
        prompt: str,
        system_instruction: Optional[str] = None,
        temperature: float = 0.0,
    ) -> str:
        """
        Executes raw text generation against the model.
        """
        pass


class AnalysisProvider(ABC):
    """
    Abstract interface for feedback analysis.
    Decouples domain workflows from specific LLM providers (Gemini, OpenAI, Azure OpenAI).
    """

    @abstractmethod
    def analyze(
        self,
        feedback_text: str,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> FeedbackAnalysisDTO:
        """
        Analyzes a single feedback text, enforcing structured schema output,
        handling provider-specific retries and timeouts, and returning a typed DTO.
        """
        pass

    @abstractmethod
    def get_model_name(self) -> str:
        """
        Returns the identifier of the model being utilized.
        """
        pass

    @abstractmethod
    def get_prompt_version(self) -> str:
        """
        Returns the prompt version identifier for auditability and schema versioning.
        """
        pass
