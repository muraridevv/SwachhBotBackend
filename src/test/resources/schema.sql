CREATE TABLE houses (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    width       DOUBLE NOT NULL,
    height      DOUBLE NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rooms (
    id          UUID PRIMARY KEY,
    house_id    UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    x           DOUBLE NOT NULL,
    y           DOUBLE NOT NULL,
    width       DOUBLE NOT NULL,
    height      DOUBLE NOT NULL,
    FOREIGN KEY (house_id) REFERENCES houses (id) ON DELETE CASCADE
);

CREATE TABLE furniture (
    id           UUID PRIMARY KEY,
    room_id      UUID NOT NULL,
    type         VARCHAR(64) NOT NULL,
    x            DOUBLE NOT NULL,
    y            DOUBLE NOT NULL,
    width        DOUBLE NOT NULL,
    height       DOUBLE NOT NULL,
    rotation_deg DOUBLE DEFAULT 0 NOT NULL,
    FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
);

CREATE TABLE occupancy_maps (
    id          UUID PRIMARY KEY,
    house_id    UUID NOT NULL,
    grid_width  INTEGER NOT NULL,
    grid_height INTEGER NOT NULL,
    cell_size   DOUBLE NOT NULL,
    map_data    CLOB NOT NULL,
    version     BIGINT DEFAULT 0 NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (house_id) REFERENCES houses (id) ON DELETE CASCADE
);

CREATE TABLE robot_objects (
    id               UUID PRIMARY KEY,
    house_id         UUID NOT NULL,
    type             VARCHAR(128) NOT NULL,
    category         VARCHAR(32) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    x                DOUBLE NOT NULL,
    y                DOUBLE NOT NULL,
    confidence       DOUBLE NOT NULL,
    first_detected   TIMESTAMP NOT NULL,
    last_detected    TIMESTAMP NOT NULL,
    detection_count  INTEGER DEFAULT 1 NOT NULL,
    FOREIGN KEY (house_id) REFERENCES houses (id) ON DELETE CASCADE
);

CREATE TABLE robot_state (
    id          UUID PRIMARY KEY,
    robot_id    VARCHAR(64) NOT NULL UNIQUE,
    house_id    UUID,
    x           DOUBLE NOT NULL,
    y           DOUBLE NOT NULL,
    rotation    DOUBLE NOT NULL,
    velocity    DOUBLE NOT NULL,
    battery     DOUBLE NOT NULL,
    status      VARCHAR(32) NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (house_id) REFERENCES houses (id) ON DELETE SET NULL
);

CREATE TABLE cleaning_sessions (
    id                 UUID PRIMARY KEY,
    house_id           UUID NOT NULL,
    started_at         TIMESTAMP NOT NULL,
    ended_at           TIMESTAMP,
    duration_seconds   BIGINT DEFAULT 0 NOT NULL,
    cleaned_percentage DOUBLE DEFAULT 0 NOT NULL,
    area_cleaned_sqm   DOUBLE DEFAULT 0 NOT NULL,
    FOREIGN KEY (house_id) REFERENCES houses (id) ON DELETE CASCADE
);

CREATE TABLE cleaning_commands (
    id          UUID PRIMARY KEY,
    robot_id    VARCHAR(64) NOT NULL,
    house_id    UUID,
    command     VARCHAR(32) NOT NULL,
    status      VARCHAR(32) NOT NULL,
    payload     CLOB,
    issued_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    acked_at    TIMESTAMP,
    FOREIGN KEY (house_id) REFERENCES houses (id) ON DELETE SET NULL
);

CREATE TABLE problem_areas (
    id          UUID PRIMARY KEY,
    house_id    UUID NOT NULL,
    x           DOUBLE NOT NULL,
    y           DOUBLE NOT NULL,
    radius      DOUBLE NOT NULL,
    description VARCHAR(512) NOT NULL,
    frequency   INTEGER DEFAULT 1 NOT NULL,
    last_seen   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (house_id) REFERENCES houses (id) ON DELETE CASCADE
);

CREATE TABLE cleaning_plans (
    id                  UUID PRIMARY KEY,
    house_id            UUID NOT NULL,
    natural_language    CLOB,
    action              VARCHAR(32) NOT NULL,
    priority            VARCHAR(16) NOT NULL,
    passes              INTEGER NOT NULL,
    excluded_areas      CLOB,
    rooms_json          CLOB NOT NULL,
    reason              CLOB,
    estimated_seconds   BIGINT NOT NULL,
    status              VARCHAR(16) NOT NULL,
    ai_generated        BOOLEAN DEFAULT false NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (house_id) REFERENCES houses (id) ON DELETE CASCADE
);

CREATE TABLE assistant_actions (
    id              UUID PRIMARY KEY,
    house_id        UUID NOT NULL,
    robot_id        VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(128),
    action_type     VARCHAR(32) NOT NULL,
    status          VARCHAR(24) NOT NULL,
    summary         VARCHAR(512) NOT NULL,
    payload         CLOB,
    plan_id         UUID,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    resolved_at     TIMESTAMP,
    FOREIGN KEY (house_id) REFERENCES houses (id) ON DELETE CASCADE
);

CREATE TABLE map_snapshots (
    id          UUID PRIMARY KEY,
    house_id    UUID NOT NULL,
    grid_width  INTEGER NOT NULL,
    grid_height INTEGER NOT NULL,
    cell_size   DOUBLE NOT NULL,
    map_data    CLOB NOT NULL,
    captured_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (house_id) REFERENCES houses (id) ON DELETE CASCADE
);

CREATE TABLE learned_insights (
    id                UUID PRIMARY KEY,
    house_id          UUID NOT NULL,
    category          VARCHAR(32)  NOT NULL,
    subject_key       VARCHAR(160) NOT NULL,
    subject_label     VARCHAR(255) NOT NULL,
    summary           VARCHAR(512) NOT NULL,
    confidence        DOUBLE DEFAULT 0 NOT NULL,
    evidence_count    INTEGER DEFAULT 0 NOT NULL,
    status            VARCHAR(24)  NOT NULL,
    source            VARCHAR(16)  NOT NULL,
    details           CLOB,
    first_learned_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (house_id, category, subject_key),
    FOREIGN KEY (house_id) REFERENCES houses (id) ON DELETE CASCADE
);

CREATE TABLE user_corrections (
    id          UUID PRIMARY KEY,
    house_id    UUID NOT NULL,
    insight_id  UUID,
    target_type VARCHAR(24)  NOT NULL,
    target_key  VARCHAR(160) NOT NULL,
    assertion   CLOB         NOT NULL,
    applied_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (house_id) REFERENCES houses (id) ON DELETE CASCADE,
    FOREIGN KEY (insight_id) REFERENCES learned_insights (id) ON DELETE SET NULL
);
