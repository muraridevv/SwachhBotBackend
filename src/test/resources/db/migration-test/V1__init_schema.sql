-- ---------------------------------------------------------------------------
-- SwachhBot backend schema (PostgreSQL)
-- ---------------------------------------------------------------------------

CREATE TABLE houses (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    width       DOUBLE PRECISION NOT NULL,
    height      DOUBLE PRECISION NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE rooms (
    id          UUID PRIMARY KEY,
    house_id    UUID NOT NULL REFERENCES houses (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    x           DOUBLE PRECISION NOT NULL,
    y           DOUBLE PRECISION NOT NULL,
    width       DOUBLE PRECISION NOT NULL,
    height      DOUBLE PRECISION NOT NULL
);
CREATE INDEX idx_rooms_house ON rooms (house_id);

CREATE TABLE furniture (
    id           UUID PRIMARY KEY,
    room_id      UUID NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    type         VARCHAR(64) NOT NULL,
    x            DOUBLE PRECISION NOT NULL,
    y            DOUBLE PRECISION NOT NULL,
    width        DOUBLE PRECISION NOT NULL,
    height       DOUBLE PRECISION NOT NULL,
    rotation_deg DOUBLE PRECISION NOT NULL DEFAULT 0
);
CREATE INDEX idx_furniture_room ON furniture (room_id);

CREATE TABLE occupancy_maps (
    id          UUID PRIMARY KEY,
    house_id    UUID NOT NULL REFERENCES houses (id) ON DELETE CASCADE,
    grid_width  INTEGER NOT NULL,
    grid_height INTEGER NOT NULL,
    cell_size   DOUBLE PRECISION NOT NULL,
    map_data    TEXT NOT NULL,
    version     BIGINT NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_maps_house ON occupancy_maps (house_id);

CREATE TABLE robot_objects (
    id               UUID PRIMARY KEY,
    house_id         UUID NOT NULL REFERENCES houses (id) ON DELETE CASCADE,
    type             VARCHAR(128) NOT NULL,
    category         VARCHAR(32) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    x                DOUBLE PRECISION NOT NULL,
    y                DOUBLE PRECISION NOT NULL,
    confidence       DOUBLE PRECISION NOT NULL,
    first_detected   TIMESTAMPTZ NOT NULL,
    last_detected    TIMESTAMPTZ NOT NULL,
    detection_count  INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX idx_objects_house ON robot_objects (house_id);
CREATE INDEX idx_objects_type ON robot_objects (house_id, type);

CREATE TABLE robot_state (
    id          UUID PRIMARY KEY,
    robot_id    VARCHAR(64) NOT NULL UNIQUE,
    house_id    UUID REFERENCES houses (id) ON DELETE SET NULL,
    x           DOUBLE PRECISION NOT NULL,
    y           DOUBLE PRECISION NOT NULL,
    rotation    DOUBLE PRECISION NOT NULL,
    velocity    DOUBLE PRECISION NOT NULL,
    battery     DOUBLE PRECISION NOT NULL,
    status      VARCHAR(32) NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cleaning_sessions (
    id                 UUID PRIMARY KEY,
    house_id           UUID NOT NULL REFERENCES houses (id) ON DELETE CASCADE,
    started_at         TIMESTAMPTZ NOT NULL,
    ended_at           TIMESTAMPTZ,
    duration_seconds   BIGINT NOT NULL DEFAULT 0,
    cleaned_percentage DOUBLE PRECISION NOT NULL DEFAULT 0,
    area_cleaned_sqm   DOUBLE PRECISION NOT NULL DEFAULT 0
);
CREATE INDEX idx_sessions_house ON cleaning_sessions (house_id);

CREATE TABLE cleaning_commands (
    id          UUID PRIMARY KEY,
    robot_id    VARCHAR(64) NOT NULL,
    house_id    UUID REFERENCES houses (id) ON DELETE SET NULL,
    command     VARCHAR(32) NOT NULL,
    status      VARCHAR(32) NOT NULL,
    payload     TEXT,
    issued_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    acked_at    TIMESTAMPTZ
);
CREATE INDEX idx_commands_robot ON cleaning_commands (robot_id);

CREATE TABLE problem_areas (
    id          UUID PRIMARY KEY,
    house_id    UUID NOT NULL REFERENCES houses (id) ON DELETE CASCADE,
    x           DOUBLE PRECISION NOT NULL,
    y           DOUBLE PRECISION NOT NULL,
    radius      DOUBLE PRECISION NOT NULL,
    description VARCHAR(512) NOT NULL,
    frequency   INTEGER NOT NULL DEFAULT 1,
    last_seen   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_problems_house ON problem_areas (house_id);
