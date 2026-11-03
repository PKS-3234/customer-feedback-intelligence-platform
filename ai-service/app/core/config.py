from functools import lru_cache
import os
from pathlib import Path
from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict

# Locate root .env and local .env
ROOT_DIR = Path(__file__).resolve().parent.parent.parent.parent
ROOT_ENV = ROOT_DIR / ".env"
LOCAL_ENV = Path(__file__).resolve().parent.parent.parent / ".env"

env_files = []
if ROOT_ENV.exists():
    env_files.append(str(ROOT_ENV))
if LOCAL_ENV.exists():
    env_files.append(str(LOCAL_ENV))
if not env_files:
    env_files = [".env"]


class Settings(BaseSettings):
    """
    Application configuration loaded strictly from environment variables and .env file.
    All secrets and provider configs are loaded dynamically without hardcoding.
    """
    APP_NAME: str = "customer-feedback-ai-service"
    APP_VERSION: str = "1.0.0"
    ENVIRONMENT: str = "development"
    DEBUG: bool = False

    AI_SERVICE_HOST: str = "0.0.0.0"
    AI_SERVICE_PORT: int = 8000

    CORS_ORIGINS: List[str] = [
        "http://localhost:4200",
        "http://localhost:8080",
        "http://127.0.0.1:4200",
        "http://127.0.0.1:8080",
    ]

    # Database
    DATABASE_URL: str = "postgresql://postgres:postgres@localhost:5432/feedback_intel"

    # AI Model & Provider
    MODEL_PROVIDER: str = "gemini"
    GEMINI_API_KEY: str = ""
    GEMINI_CHAT_MODEL: str = "gemini-2.5-flash"
    EMBEDDING_PROVIDER: str = "gemini"
    GEMINI_EMBEDDING_MODEL: str = "gemini-embedding-001"
    EMBEDDING_DIMENSIONS: int = 768
    PROMPT_VERSION: str = "v1.0"
    MAX_FEEDBACK_ANALYSIS_TOKENS: int = 2048

    # Execution & Rate Limiting
    AI_BATCH_SIZE: int = 25
    AI_RETRY_COUNT: int = 3
    AI_TIMEOUT_SECONDS: float = 60.0
    AI_RATE_LIMIT_DELAY: float = 0.5

    model_config = SettingsConfigDict(
        env_file=tuple(env_files),
        env_file_encoding="utf-8",
        extra="ignore"
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
