from functools import lru_cache
from typing import Optional

from app.core.config import get_settings
from app.provider.base import AnalysisProvider
from app.provider.gemini import GeminiAnalysisProvider


class ProviderFactory:
    """
    Factory creating AnalysisProvider instances based on application configuration.
    Permits swapping or adding providers (Azure OpenAI, OpenAI) with zero workflow changes.
    """

    @staticmethod
    def get_analysis_provider(
        provider_name: Optional[str] = None,
    ) -> AnalysisProvider:
        settings = get_settings()
        name = (provider_name or settings.MODEL_PROVIDER).lower()

        if name == "gemini":
            return GeminiAnalysisProvider()
        elif name in ("openai", "azure_openai"):
            raise NotImplementedError(
                f"Provider '{name}' is reserved for subsequent phases. Currently 'gemini' is active."
            )
        else:
            raise ValueError(f"Unknown AI analysis provider: '{name}'")


@lru_cache
def get_default_provider() -> AnalysisProvider:
    return ProviderFactory.get_analysis_provider()
