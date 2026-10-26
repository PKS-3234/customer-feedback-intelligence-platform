-- ==============================================================================
-- Flyway Migration V2: Create Ingestion Schema and Feedback Storage
-- ==============================================================================
-- Defines core relational schema for ingestion batches, raw feedback,
-- processing errors, and reserved tables for future AI analysis and embeddings.
-- ==============================================================================

-- 1. Ingestion Batches Table
CREATE TABLE IF NOT EXISTS ingestion_batches (
    batch_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(255) NOT NULL,
    source_type VARCHAR(50) NOT NULL DEFAULT 'CSV',
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    total_rows INT NOT NULL DEFAULT 0 CHECK (total_rows >= 0),
    valid_rows INT NOT NULL DEFAULT 0 CHECK (valid_rows >= 0),
    successful_rows INT NOT NULL DEFAULT 0 CHECK (successful_rows >= 0),
    failed_rows INT NOT NULL DEFAULT 0 CHECK (failed_rows >= 0),
    duplicate_rows INT NOT NULL DEFAULT 0 CHECK (duplicate_rows >= 0),
    status VARCHAR(50) NOT NULL CHECK (status IN (
        'RECEIVED', 'VALIDATING', 'VALIDATED', 'IMPORTING', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED'
    )),
    file_hash VARCHAR(64) NOT NULL,
    uploaded_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS idx_batches_status ON ingestion_batches(status);
CREATE INDEX IF NOT EXISTS idx_batches_uploaded_at ON ingestion_batches(uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_batches_file_hash ON ingestion_batches(file_hash);

-- 2. Feedback Table (Immutable Raw Source of Truth)
CREATE TABLE IF NOT EXISTS feedback (
    feedback_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES ingestion_batches(batch_id) ON DELETE CASCADE,
    external_reference VARCHAR(100) NOT NULL,
    customer_reference VARCHAR(100),
    feedback_text TEXT NOT NULL,
    feedback_date DATE NOT NULL,
    channel VARCHAR(50) NOT NULL,
    rating INT CHECK (rating IS NULL OR (rating >= 1 AND rating <= 5)),
    product VARCHAR(100),
    region VARCHAR(100),
    customer_segment VARCHAR(100),
    language VARCHAR(20),
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    processing_status VARCHAR(50) NOT NULL DEFAULT 'AI_PENDING' CHECK (processing_status IN (
        'VALIDATED', 'STORED', 'AI_PENDING', 'FAILED'
    )),
    deduplication_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS idx_feedback_dedup_hash ON feedback(deduplication_hash);
CREATE INDEX IF NOT EXISTS idx_feedback_batch_id ON feedback(batch_id);
CREATE INDEX IF NOT EXISTS idx_feedback_date ON feedback(feedback_date DESC);
CREATE INDEX IF NOT EXISTS idx_feedback_channel ON feedback(channel);
CREATE INDEX IF NOT EXISTS idx_feedback_product ON feedback(product);
CREATE INDEX IF NOT EXISTS idx_feedback_rating ON feedback(rating);
CREATE INDEX IF NOT EXISTS idx_feedback_status ON feedback(processing_status);

-- 3. Feedback Analysis Table (Reserved for Future AI Classification - Phase 3+)
CREATE TABLE IF NOT EXISTS feedback_analysis (
    analysis_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL REFERENCES feedback(feedback_id) ON DELETE CASCADE,
    sentiment VARCHAR(20),
    category VARCHAR(50),
    theme VARCHAR(50),
    emotion VARCHAR(50),
    priority VARCHAR(20),
    assigned_department VARCHAR(50),
    short_summary TEXT,
    confidence_score NUMERIC(3,2) CHECK (confidence_score IS NULL OR (confidence_score >= 0.00 AND confidence_score <= 1.00)),
    analyzed_at TIMESTAMP WITH TIME ZONE,
    model_name VARCHAR(100),
    prompt_version VARCHAR(50),
    is_current BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS idx_analysis_feedback_id ON feedback_analysis(feedback_id);
CREATE INDEX IF NOT EXISTS idx_analysis_sentiment ON feedback_analysis(sentiment);
CREATE INDEX IF NOT EXISTS idx_analysis_category ON feedback_analysis(category);
CREATE INDEX IF NOT EXISTS idx_analysis_theme ON feedback_analysis(theme);
CREATE INDEX IF NOT EXISTS idx_analysis_priority ON feedback_analysis(priority);
CREATE INDEX IF NOT EXISTS idx_analysis_is_current ON feedback_analysis(is_current);

-- 4. Feedback Embeddings Table (Reserved for Future PGVector Embeddings - Phase 5+)
CREATE TABLE IF NOT EXISTS feedback_embeddings (
    embedding_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID NOT NULL REFERENCES feedback(feedback_id) ON DELETE CASCADE,
    embedding vector,
    embedding_model VARCHAR(100),
    embedding_dimension INT,
    is_current BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
);

CREATE INDEX IF NOT EXISTS idx_embeddings_feedback_id ON feedback_embeddings(feedback_id);
CREATE INDEX IF NOT EXISTS idx_embeddings_is_current ON feedback_embeddings(is_current);

-- 5. Processing Errors Table (Row-level and File-level Validation / Pipeline Errors)
CREATE TABLE IF NOT EXISTS processing_errors (
    error_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES ingestion_batches(batch_id) ON DELETE CASCADE,
    feedback_id UUID REFERENCES feedback(feedback_id) ON DELETE SET NULL,
    csv_row_number INT,
    processing_stage VARCHAR(50) NOT NULL CHECK (processing_stage IN (
        'FILE_VALIDATION', 'CSV_PARSING', 'ROW_VALIDATION', 'DUPLICATE_CHECK', 'DATABASE_STORAGE', 'AI_ANALYSIS', 'EMBEDDING_GENERATION'
    )),
    error_code VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,
    original_row JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_errors_batch_id ON processing_errors(batch_id);
CREATE INDEX IF NOT EXISTS idx_errors_stage ON processing_errors(processing_stage);
CREATE INDEX IF NOT EXISTS idx_errors_code ON processing_errors(error_code);
CREATE INDEX IF NOT EXISTS idx_errors_created_at ON processing_errors(created_at DESC);
