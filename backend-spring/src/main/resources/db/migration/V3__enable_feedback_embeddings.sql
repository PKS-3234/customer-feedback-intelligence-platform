-- Fix the embedding size to match Gemini text-embedding-004 and enable cosine ANN search.
ALTER TABLE feedback_embeddings
    ALTER COLUMN embedding TYPE vector(768)
    USING embedding::vector(768);

ALTER TABLE feedback_embeddings
    ADD COLUMN analysis_id UUID REFERENCES feedback_analysis(analysis_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_embeddings_one_current_per_feedback
    ON feedback_embeddings(feedback_id)
    WHERE is_current = true;

CREATE INDEX IF NOT EXISTS idx_embeddings_cosine_hnsw
    ON feedback_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WHERE is_current = true;
