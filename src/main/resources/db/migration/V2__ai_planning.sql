-- ---------------------------------------------------------------------------
-- Phase 9: AI planning layer
-- ---------------------------------------------------------------------------

-- Semantic retrieval store (Spring AI PgVectorStore manages its own table,
-- but the extension must exist first).
CREATE EXTENSION IF NOT EXISTS vector;

-- Audit trail of every generated plan (validated or rejected).
-- Storing plans lets us explain *why* the robot did something, and gives the
-- AI searchable history of its own decisions.
CREATE TABLE cleaning_plans (
    id                  UUID PRIMARY KEY,
    house_id            UUID NOT NULL REFERENCES houses (id) ON DELETE CASCADE,
    natural_language    TEXT,
    action              VARCHAR(32) NOT NULL,
    priority            VARCHAR(16) NOT NULL,
    passes              INTEGER NOT NULL,
    excluded_areas      TEXT,
    rooms_json          TEXT NOT NULL,
    reason              TEXT,
    estimated_seconds   BIGINT NOT NULL,
    status              VARCHAR(16) NOT NULL,
    ai_generated        BOOLEAN NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_plans_house ON cleaning_plans (house_id);
CREATE INDEX idx_plans_created ON cleaning_plans (created_at DESC);
