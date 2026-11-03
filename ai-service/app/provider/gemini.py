import json
import logging
import time
from typing import Optional, Dict, Any
import httpx
from pydantic import ValidationError

from app.core.config import get_settings
from app.models.analysis import FeedbackAnalysisDTO
from app.provider.base import AnalysisProvider, ChatModelProvider

logger = logging.getLogger(__name__)

# Strict JSON Schema definition for Gemini Structured Outputs
GEMINI_ANALYSIS_SCHEMA = {
    "type": "OBJECT",
    "properties": {
        "sentiment": {
            "type": "STRING",
            "enum": ["POSITIVE", "NEUTRAL", "NEGATIVE"],
        },
        "category": {
            "type": "STRING",
        },
        "theme": {
            "type": "STRING",
        },
        "emotion": {
            "type": "STRING",
        },
        "priority": {
            "type": "STRING",
            "enum": ["LOW", "MEDIUM", "HIGH", "CRITICAL"],
        },
        "assignedDepartment": {
            "type": "STRING",
        },
        "summary": {
            "type": "STRING",
        },
        "confidence": {
            "type": "NUMBER",
        },
    },
    "required": [
        "sentiment",
        "category",
        "theme",
        "emotion",
        "priority",
        "assignedDepartment",
        "summary",
        "confidence",
    ],
}

SYSTEM_INSTRUCTION = (
    "You are an expert customer feedback intelligence and decision-support engine for financial and banking operations. "
    "Your objective is to analyze customer comments and extract structured analytical metrics strictly following the provided schema. "
    "Categorize the comment into functional categories (e.g. Authentication, Payments, Transfers, Mobile App, Support, Account Services, Cards). "
    "Identify the root issue theme (e.g. OTP Issue, Login Failure, Duplicate Debit, App Crash, Slow Performance, Refund Delay, Unhelpful Support). "
    "Detect primary customer emotion (e.g. Satisfied, Happy, Frustrated, Angry, Disappointed, Confused). "
    "Assign actionable priority (LOW, MEDIUM, HIGH, CRITICAL) based on financial loss, account lockout, or widespread operational breakdown. "
    "Assign the resolving department (e.g. Mobile Engineering, Payments Team, Support Team, Operations Team). "
    "Provide a factual summary of 200 characters or less. "
    "Output confidence score between 0.0 and 1.0."
)


class GeminiAnalysisProvider(AnalysisProvider, ChatModelProvider):
    """
    Production-grade Google Gemini Analysis Provider.
    Enforces structured JSON outputs, configured timeouts, retries, and strict schema validation.
    Never exposes or logs the API key.
    """

    def __init__(
        self,
        api_key: Optional[str] = None,
        model_name: Optional[str] = None,
        prompt_version: Optional[str] = None,
        timeout: Optional[float] = None,
        retries: Optional[int] = None,
    ):
        settings = get_settings()
        self._api_key = api_key or settings.GEMINI_API_KEY
        if not self._api_key:
            raise ValueError("GEMINI_API_KEY environment variable is not configured.")

        self.model_name = model_name or settings.GEMINI_CHAT_MODEL
        self.prompt_version = prompt_version or settings.PROMPT_VERSION
        self.timeout = timeout or settings.AI_TIMEOUT_SECONDS
        self.retries = retries if retries is not None else settings.AI_RETRY_COUNT
        self.endpoint_url = (
            f"https://generativelanguage.googleapis.com/v1beta/models/{self.model_name}:generateContent"
        )

    def get_model_name(self) -> str:
        return self.model_name

    def get_prompt_version(self) -> str:
        return self.prompt_version

    def generate(
        self,
        prompt: str,
        system_instruction: Optional[str] = None,
        temperature: float = 0.0,
    ) -> str:
        """
        Executes raw text generation against Gemini.
        """
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": self._api_key,
        }
        payload: Dict[str, Any] = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {"temperature": temperature},
        }
        if system_instruction:
            payload["systemInstruction"] = {"parts": [{"text": system_instruction}]}

        with httpx.Client(timeout=self.timeout) as client:
            response = client.post(self.endpoint_url, headers=headers, json=payload)
            response.raise_for_status()
            data = response.json()
            candidates = data.get("candidates", [])
            if not candidates:
                raise RuntimeError("Gemini returned empty candidate list.")
            return candidates[0]["content"]["parts"][0]["text"]

    def analyze(
        self,
        feedback_text: str,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> FeedbackAnalysisDTO:
        """
        Executes schema-constrained structured analysis with automatic retries and exponential backoff.
        """
        prompt = self._build_analysis_prompt(feedback_text, metadata)
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": self._api_key,
        }
        payload = {
            "contents": [{"parts": [{"text": prompt}]}],
            "systemInstruction": {"parts": [{"text": SYSTEM_INSTRUCTION}]},
            "generationConfig": {
                "response_mime_type": "application/json",
                "response_schema": GEMINI_ANALYSIS_SCHEMA,
                "temperature": 0.1,
                "maxOutputTokens": 1024,
            },
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

                if response.status_code != 200:
                    logger.warning(
                        "Gemini API returned HTTP %s on attempt %d/%d",
                        response.status_code,
                        attempt,
                        self.retries,
                    )
                    response.raise_for_status()

                data = response.json()
                candidates = data.get("candidates", [])
                if not candidates:
                    raise ValueError("Gemini returned empty candidate list.")

                raw_json_text = candidates[0]["content"]["parts"][0]["text"]
                parsed_json = json.loads(raw_json_text)

                # Validate against Pydantic DTO
                dto = FeedbackAnalysisDTO.model_validate(parsed_json)
                return dto

            except (httpx.HTTPError, json.JSONDecodeError, ValidationError, ValueError, Exception) as ex:
                last_exception = ex
                logger.warning(
                    "Analysis attempt %d/%d failed with error: %s (%s). Retrying...",
                    attempt,
                    self.retries,
                    type(ex).__name__,
                    str(ex),
                )
                if attempt < self.retries:
                    time.sleep(0.1 * (2 ** (attempt - 1)))

        raise RuntimeError(
            f"Gemini analysis failed after {self.retries} attempts. Cause: {last_exception}"
        ) from last_exception

    def _build_analysis_prompt(
        self,
        feedback_text: str,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> str:
        prompt_parts = ["Please analyze the following customer feedback comment:\n"]
        prompt_parts.append(f'Feedback Text: "{feedback_text.strip()}"')

        if metadata:
            if metadata.get("rating"):
                prompt_parts.append(f"Customer Rating: {metadata['rating']}/5")
            if metadata.get("channel"):
                prompt_parts.append(f"Submission Channel: {metadata['channel']}")
            if metadata.get("product"):
                prompt_parts.append(f"Associated Product: {metadata['product']}")
            if metadata.get("customer_segment"):
                prompt_parts.append(f"Customer Segment: {metadata['customer_segment']}")
            if metadata.get("region"):
                prompt_parts.append(f"Region: {metadata['region']}")

        prompt_parts.append(
            "\nProduce the required structured JSON output conforming to the schema."
        )
        return "\n".join(prompt_parts)
