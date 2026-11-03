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


def get_feedback_with_current_analysis(
    feedback_id: str,
) -> Optional[Dict[str, Any]]:
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT feedback.feedback_id, feedback.batch_id, feedback.feedback_text,
                       analysis.analysis_id,
                       analysis.sentiment, analysis.category, analysis.theme,
                       analysis.emotion, analysis.priority,
                       analysis.assigned_department, analysis.short_summary
                FROM feedback
                JOIN feedback_analysis analysis
                  ON analysis.feedback_id = feedback.feedback_id
                 AND analysis.is_current = true
                WHERE feedback.feedback_id = %s
                ORDER BY analysis.analyzed_at DESC
                LIMIT 1
                """,
                (feedback_id,),
            )
            return cur.fetchone()


def get_analytics_snapshot() -> Dict[str, Any]:
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT
                    COUNT(DISTINCT feedback.feedback_id)::int AS feedback_total,
                    COUNT(DISTINCT analysis.feedback_id)::int AS analyzed_total
                FROM feedback
                LEFT JOIN feedback_analysis analysis
                  ON analysis.feedback_id = feedback.feedback_id
                 AND analysis.is_current = true
                """
            )
            totals = cur.fetchone()
            cur.execute(
                """
                SELECT sentiment, COUNT(*)::int AS count
                FROM feedback_analysis
                WHERE is_current = true
                GROUP BY sentiment
                ORDER BY count DESC, sentiment
                """
            )
            sentiments = cur.fetchall()
            cur.execute(
                """
                SELECT category, COUNT(*)::int AS count
                FROM feedback_analysis
                WHERE is_current = true
                GROUP BY category
                ORDER BY count DESC, category
                LIMIT 5
                """
            )
            categories = cur.fetchall()
            cur.execute(
                """
                SELECT priority, COUNT(*)::int AS count
                FROM feedback_analysis
                WHERE is_current = true
                GROUP BY priority
                ORDER BY count DESC, priority
                """
            )
            priorities = cur.fetchall()

    return {
        "feedbackTotal": totals["feedback_total"],
        "analyzedTotal": totals["analyzed_total"],
        "sentiments": sentiments,
        "categories": categories,
        "priorities": priorities,
    }


def get_data_quality_metrics() -> Dict[str, int]:
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT
                    COUNT(*)::int AS feedback_total,
                    COUNT(*) FILTER (
                        WHERE EXISTS (
                            SELECT 1 FROM feedback_analysis analysis
                            WHERE analysis.feedback_id = feedback.feedback_id
                              AND analysis.is_current = true
                        )
                    )::int AS analyzed_total,
                    COUNT(*) FILTER (
                        WHERE EXISTS (
                            SELECT 1
                            FROM feedback_analysis analysis
                            JOIN feedback_embeddings embedding
                              ON embedding.analysis_id = analysis.analysis_id
                             AND embedding.feedback_id = feedback.feedback_id
                             AND embedding.is_current = true
                            WHERE analysis.feedback_id = feedback.feedback_id
                              AND analysis.is_current = true
                        )
                    )::int AS embedded_total,
                    COUNT(*) FILTER (
                        WHERE processing_status = 'FAILED'
                    )::int AS failed_feedback_total
                FROM feedback
                """
            )
            metrics = cur.fetchone()
            cur.execute("SELECT COUNT(*)::int AS count FROM processing_errors")
            errors = cur.fetchone()["count"]

    return {
        "feedbackTotal": metrics["feedback_total"],
        "analyzedTotal": metrics["analyzed_total"],
        "embeddedTotal": metrics["embedded_total"],
        "failedFeedbackTotal": metrics["failed_feedback_total"],
        "processingErrorTotal": errors,
    }


def get_pending_embedding_records(
    batch_id: str,
    model_name: str,
    limit: Optional[int] = None,
) -> List[Dict[str, Any]]:
    query = """
        SELECT feedback.feedback_id, feedback.batch_id, feedback.feedback_text,
               analysis.analysis_id,
               analysis.sentiment, analysis.category, analysis.theme,
               analysis.emotion, analysis.priority,
               analysis.assigned_department, analysis.short_summary
        FROM feedback
        JOIN feedback_analysis analysis
          ON analysis.feedback_id = feedback.feedback_id
         AND analysis.is_current = true
        WHERE feedback.batch_id = %s
          AND NOT EXISTS (
              SELECT 1
              FROM feedback_embeddings embedding
              WHERE embedding.feedback_id = feedback.feedback_id
                AND embedding.is_current = true
                AND embedding.embedding_model = %s
                AND embedding.analysis_id = analysis.analysis_id
          )
        ORDER BY feedback.created_at ASC
    """
    params: List[Any] = [batch_id, model_name]
    if limit is not None:
        query += " LIMIT %s"
        params.append(limit)

    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(query, tuple(params))
            return cur.fetchall()


def save_feedback_embedding(
    feedback_id: str,
    analysis_id: str,
    embedding: List[float],
    model_name: str,
) -> Dict[str, Any]:
    if not embedding:
        raise ValueError("Embedding vector must not be empty.")
    vector_literal = "[" + ",".join(repr(float(value)) for value in embedding) + "]"

    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                UPDATE feedback_embeddings
                SET is_current = false
                WHERE feedback_id = %s AND is_current = true
                """,
                (feedback_id,),
            )
            cur.execute(
                """
                INSERT INTO feedback_embeddings (
                    feedback_id, analysis_id, embedding, embedding_model,
                    embedding_dimension, is_current
                ) VALUES (%s, %s, %s::vector, %s, %s, true)
                RETURNING embedding_id, feedback_id, embedding_model,
                          embedding_dimension, is_current, created_at
                """,
                (feedback_id, analysis_id, vector_literal, model_name, len(embedding)),
            )
            return cur.fetchone()


def get_current_feedback_embedding(
    feedback_id: str,
) -> Optional[Dict[str, Any]]:
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT embedding_id, feedback_id, embedding_model,
                       embedding_dimension, is_current, created_at
                FROM feedback_embeddings
                WHERE feedback_id = %s AND is_current = true
                  AND EXISTS (
                      SELECT 1
                      FROM feedback_analysis analysis
                      WHERE analysis.analysis_id = feedback_embeddings.analysis_id
                        AND analysis.is_current = true
                  )
                ORDER BY created_at DESC
                LIMIT 1
                """,
                (feedback_id,),
            )
            return cur.fetchone()


def search_feedback_by_embedding(
    embedding: List[float],
    limit: int,
    model_name: str,
) -> List[Dict[str, Any]]:
    vector_literal = "[" + ",".join(repr(float(value)) for value in embedding) + "]"
    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT feedback.feedback_id, feedback.batch_id,
                       feedback.feedback_text, analysis.sentiment,
                       analysis.category, analysis.theme, analysis.emotion,
                       analysis.priority, analysis.short_summary,
                       GREATEST(0.0, LEAST(
                           1.0, 1.0 - (embedding.embedding <=> %s::vector)
                       )) AS similarity
                FROM feedback_embeddings embedding
                JOIN feedback ON feedback.feedback_id = embedding.feedback_id
                JOIN feedback_analysis analysis
                  ON analysis.feedback_id = feedback.feedback_id
                 AND analysis.is_current = true
                 AND embedding.analysis_id = analysis.analysis_id
                WHERE embedding.is_current = true
                  AND embedding.embedding_model = %s
                ORDER BY embedding.embedding <=> %s::vector
                LIMIT %s
                """,
                (vector_literal, model_name, vector_literal, limit),
            )
            return cur.fetchall()


def record_embedding_error(
    batch_id: str,
    feedback_id: Optional[str],
    error_code: str,
    error_message: str,
    raw_row: Optional[Dict[str, Any]] = None,
) -> None:
    import json

    with get_db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO processing_errors (
                    batch_id, feedback_id, processing_stage, error_code,
                    error_message, original_row, created_at
                ) VALUES (
                    %s, %s, 'EMBEDDING_GENERATION', %s, %s, %s, CURRENT_TIMESTAMP
                )
                """,
                (
                    batch_id,
                    feedback_id,
                    error_code,
                    error_message,
                    json.dumps(raw_row, default=str) if raw_row else None,
                ),
            )


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
