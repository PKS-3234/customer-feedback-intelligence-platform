import logging
import math
import time
from typing import Any, Dict, List, Optional

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)


class GeminiEmbeddingProvider:
    def __init__(
        self,
        api_key: Optional[str] = None,
        model_name: Optional[str] = None,
        dimensions: Optional[int] = None,
        timeout: Optional[float] = None,
        retries: Optional[int] = None,
    ):
        settings = get_settings()
        self.api_key = api_key or settings.GEMINI_API_KEY
        if not self.api_key:
            raise ValueError("GEMINI_API_KEY environment variable is not configured.")
        if settings.EMBEDDING_PROVIDER.lower() != "gemini":
            raise ValueError(
                f"Unsupported embedding provider: '{settings.EMBEDDING_PROVIDER}'."
            )

        self.model_name = model_name or settings.GEMINI_EMBEDDING_MODEL
        self.dimensions = dimensions or settings.EMBEDDING_DIMENSIONS
        if self.dimensions < 1:
            raise ValueError("Embedding dimensions must be a positive integer.")
        self.timeout = timeout or settings.AI_TIMEOUT_SECONDS
        self.retries = retries if retries is not None else settings.AI_RETRY_COUNT
        self.endpoint_url = (
            "https://generativelanguage.googleapis.com/v1beta/models/"
            f"{self.model_name}:embedContent"
        )

    def embed(
        self,
        text: str,
        task_type: str = "RETRIEVAL_DOCUMENT",
    ) -> List[float]:
        if not text or not text.strip():
            raise ValueError("Embedding text must not be empty.")

        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": self.api_key,
        }
        payload: Dict[str, Any] = {
            "model": f"models/{self.model_name}",
            "content": {"parts": [{"text": text.strip()}]},
            "taskType": task_type,
            "outputDimensionality": self.dimensions,
        }

        last_exception: Optional[Exception] = None
        for attempt in range(1, self.retries + 1):
            try:
                with httpx.Client(timeout=self.timeout) as client:
                    response = client.post(
                        self.endpoint_url,
                        headers=headers,
                        json=payload,
                    )
                response.raise_for_status()
                data = response.json()
                values = data.get("embedding", {}).get("values")
                if not isinstance(values, list) or len(values) != self.dimensions:
                    raise ValueError(
                        "Gemini returned an embedding with an unexpected dimension."
                    )
                vector = [float(value) for value in values]
                if not all(math.isfinite(value) for value in vector):
                    raise ValueError("Gemini returned non-finite embedding values.")
                return vector
            except (httpx.HTTPError, ValueError, TypeError, KeyError) as ex:
                last_exception = ex
                logger.warning(
                    "Gemini embedding attempt %d/%d failed (%s).",
                    attempt,
                    self.retries,
                    type(ex).__name__,
                )
                if attempt < self.retries:
                    time.sleep(0.1 * (2 ** (attempt - 1)))

        raise RuntimeError(
            f"Gemini embedding failed after {self.retries} attempts."
        ) from last_exception
