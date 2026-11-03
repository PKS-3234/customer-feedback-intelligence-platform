from contextlib import contextmanager
import logging
from typing import Optional, Dict, Any, List
import psycopg
from psycopg.rows import dict_row

from app.core.config import get_settings

logger = logging.getLogger(__name__)


@contextmanager
def get_db_connection():
    """
    Context manager that yields a psycopg connection configured with dict_row.
    Automatically commits on normal exit and rolls back on exception.
    """
    settings = get_settings()
    conn = psycopg.connect(settings.DATABASE_URL, row_factory=dict_row)
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def get_feedback_record(feedback_id: str) -> Optional[Dict[str, Any]]:
    """
    Fetches raw feedback record by UUID from the database.
    """
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT feedback_id, batch_id, external_reference, customer_reference,
                       feedback_text, feedback_date, channel, rating, product,
                       region, customer_segment, language, processing_status
                FROM feedback
                WHERE feedback_id = %s
                """,
                (feedback_id,)
            )
            return cur.fetchone()


def get_pending_feedback_records(batch_id: str, limit: Optional[int] = None) -> List[Dict[str, Any]]:
    """
    Fetches all feedback records for a batch where processing_status is AI_PENDING.
    """
    query = """
        SELECT feedback_id, batch_id, external_reference, customer_reference,
               feedback_text, feedback_date, channel, rating, product,
               region, customer_segment, language, processing_status
        FROM feedback
        WHERE batch_id = %s AND processing_status = 'AI_PENDING'
        ORDER BY created_at ASC
    """
    params = [batch_id]
    if limit is not None and limit > 0:
        query += " LIMIT %s"
        params.append(limit)

    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(query, tuple(params))
            return cur.fetchall()


def save_feedback_analysis(
    feedback_id: str,
    analysis_data: Dict[str, Any],
    model_name: str,
    prompt_version: str
) -> Dict[str, Any]:
    """
    Persists analysis result to feedback_analysis.
    Enforces versioning by setting existing current records to is_current = false.
    Updates feedback.processing_status to 'STORED'.
    """
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            # 1. Supersede any existing current analysis for this feedback
            cur.execute(
                """
                UPDATE feedback_analysis
                SET is_current = false
                WHERE feedback_id = %s AND is_current = true
                """,
                (feedback_id,)
            )

            # 2. Insert new analysis record
            cur.execute(
                """
                INSERT INTO feedback_analysis (
                    feedback_id, sentiment, category, theme, emotion, priority,
                    assigned_department, short_summary, confidence_score,
                    analyzed_at, model_name, prompt_version, is_current
                ) VALUES (
                    %s, %s, %s, %s, %s, %s,
                    %s, %s, %s,
                    CURRENT_TIMESTAMP, %s, %s, true
                )
                RETURNING analysis_id, feedback_id, sentiment, category, theme,
                          emotion, priority, assigned_department, short_summary,
                          confidence_score, analyzed_at, model_name, prompt_version,
                          is_current, created_at
                """,
                (
                    feedback_id,
                    analysis_data.get("sentiment"),
                    analysis_data.get("category"),
                    analysis_data.get("theme"),
                    analysis_data.get("emotion"),
                    analysis_data.get("priority"),
                    analysis_data.get("assignedDepartment"),
                    analysis_data.get("summary") or analysis_data.get("short_summary"),
                    analysis_data.get("confidence") or analysis_data.get("confidence_score"),
                    model_name,
                    prompt_version,
                )
            )
            saved_record = cur.fetchone()

            # 3. Update feedback status
            cur.execute(
                """
                UPDATE feedback
                SET processing_status = 'STORED', updated_at = CURRENT_TIMESTAMP
                WHERE feedback_id = %s
                """,
                (feedback_id,)
            )

            return saved_record


def get_latest_feedback_analysis(feedback_id: str) -> Optional[Dict[str, Any]]:
    """
    Retrieves the current active analysis record for a feedback ID.
    """
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT analysis_id, feedback_id, sentiment, category, theme,
                       emotion, priority, assigned_department, short_summary,
                       confidence_score, analyzed_at, model_name, prompt_version,
                       is_current, created_at
                FROM feedback_analysis
                WHERE feedback_id = %s AND is_current = true
                ORDER BY analyzed_at DESC
                LIMIT 1
                """,
                (feedback_id,)
            )
            return cur.fetchone()


def record_analysis_error(
    batch_id: str,
    feedback_id: Optional[str],
    error_code: str,
    error_message: str,
    raw_row: Optional[Dict[str, Any]] = None
) -> None:
    """
    Records an AI analysis processing error into processing_errors table.
    """
    import json
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO processing_errors (
                    batch_id, feedback_id, processing_stage, error_code,
                    error_message, original_row, created_at
                ) VALUES (
                    %s, %s, 'AI_ANALYSIS', %s, %s, %s, CURRENT_TIMESTAMP
                )
                """,
                (
                    batch_id,
                    feedback_id,
                    error_code,
                    error_message,
                    json.dumps(raw_row) if raw_row else None
                )
            )
