-- ---------------------------------------------------------------------------
-- Phase 11: AI assistant (chat + tool calling)
-- ---------------------------------------------------------------------------
-- The LLM never executes a robot command directly. When a command tool is
-- called it writes a row here with status PENDING. A human must explicitly
-- confirm it before the deterministic execution path runs.

CREATE TABLE assistant_actions (
    id              UUID PRIMARY KEY,
    house_id        UUID NOT NULL REFERENCES houses (id) ON DELETE CASCADE,
    robot_id        VARCHAR(64)  NOT NULL,
    conversation_id VARCHAR(64),
    action_type     VARCHAR(32)  NOT NULL,   -- START_CLEANING | PAUSE_CLEANING | STOP_CLEANING
    status          VARCHAR(24)  NOT NULL,   -- PENDING | CONFIRMED | EXECUTED | REJECTED | EXPIRED
    summary         VARCHAR(512) NOT NULL,
    payload         TEXT,
    plan_id         UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL,
    resolved_at     TIMESTAMPTZ
);
CREATE INDEX idx_actions_house ON assistant_actions (house_id, created_at DESC);
CREATE INDEX idx_actions_conversation ON assistant_actions (conversation_id);
