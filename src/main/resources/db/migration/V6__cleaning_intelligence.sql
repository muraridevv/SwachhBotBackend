-- ---------------------------------------------------------------------------
-- Phase 19: Adaptive Cleaning Intelligence
-- ---------------------------------------------------------------------------

ALTER TABLE rooms ADD COLUMN user_priority INTEGER DEFAULT 1;
ALTER TABLE cleaning_sessions ADD COLUMN room_id UUID REFERENCES rooms (id) ON DELETE SET NULL;

-- To track per-room cleaning effectiveness.
CREATE TABLE room_cleaning_stats (
    room_id           UUID PRIMARY KEY REFERENCES rooms (id) ON DELETE CASCADE,
    last_cleaned_at   TIMESTAMPTZ,
    total_cleaned_count INTEGER NOT NULL DEFAULT 0,
    avg_coverage      DOUBLE PRECISION NOT NULL DEFAULT 0,
    avg_duration_sec  BIGINT NOT NULL DEFAULT 0,
    dirt_score        DOUBLE PRECISION NOT NULL DEFAULT 0 -- Updated by intelligence service
);
