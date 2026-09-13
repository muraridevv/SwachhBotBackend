-- ---------------------------------------------------------------------------
-- Phase 18: Advanced Vision and Environment Change Detection
-- ---------------------------------------------------------------------------

ALTER TABLE robot_objects ADD COLUMN room_name VARCHAR(255);
ALTER TABLE robot_objects ADD COLUMN previous_x DOUBLE PRECISION;
ALTER TABLE robot_objects ADD COLUMN previous_y DOUBLE PRECISION;

-- Log of environmental changes detected by the robot.
CREATE TABLE environment_changes (
    id              UUID PRIMARY KEY,
    house_id        UUID NOT NULL REFERENCES houses (id) ON DELETE CASCADE,
    object_id       UUID NOT NULL REFERENCES robot_objects (id) ON DELETE CASCADE,
    change_type     VARCHAR(32) NOT NULL, -- ADDED, MOVED, REMOVED, RELOCATED
    description     TEXT,
    confidence      DOUBLE PRECISION NOT NULL,
    detected_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_env_changes_house ON environment_changes (house_id);
CREATE INDEX idx_env_changes_object ON environment_changes (object_id);
