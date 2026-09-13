-- ---------------------------------------------------------------------------
-- Phase 10: adaptive cleaning based on historical robot experience
-- ---------------------------------------------------------------------------

-- Historical occupancy grids, so the current map can be diffed against the past.
CREATE TABLE map_snapshots (
    id          UUID PRIMARY KEY,
    house_id    UUID NOT NULL REFERENCES houses (id) ON DELETE CASCADE,
    grid_width  INTEGER NOT NULL,
    grid_height INTEGER NOT NULL,
    cell_size   DOUBLE PRECISION NOT NULL,
    map_data    TEXT NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_snapshots_house ON map_snapshots (house_id, captured_at DESC);

-- Everything the robot has "learned", with a confidence level.
CREATE TABLE learned_insights (
    id                UUID PRIMARY KEY,
    house_id          UUID NOT NULL REFERENCES houses (id) ON DELETE CASCADE,
    category          VARCHAR(32)  NOT NULL,
    subject_key       VARCHAR(160) NOT NULL,
    subject_label     VARCHAR(255) NOT NULL,
    summary           VARCHAR(512) NOT NULL,
    confidence        DOUBLE PRECISION NOT NULL DEFAULT 0,
    evidence_count    INTEGER NOT NULL DEFAULT 0,
    status            VARCHAR(24)  NOT NULL,
    source            VARCHAR(16)  NOT NULL,
    details           TEXT,
    first_learned_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_insight_identity UNIQUE (house_id, category, subject_key)
);
CREATE INDEX idx_insights_house ON learned_insights (house_id);
CREATE INDEX idx_insights_category ON learned_insights (house_id, category);

-- Human corrections that override what the AI assumed.
CREATE TABLE user_corrections (
    id          UUID PRIMARY KEY,
    house_id    UUID NOT NULL REFERENCES houses (id) ON DELETE CASCADE,
    insight_id  UUID REFERENCES learned_insights (id) ON DELETE SET NULL,
    target_type VARCHAR(24)  NOT NULL,
    target_key  VARCHAR(160) NOT NULL,
    assertion   TEXT         NOT NULL,
    applied_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_corrections_house ON user_corrections (house_id, applied_at DESC);
