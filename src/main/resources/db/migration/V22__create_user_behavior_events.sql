CREATE TABLE user_behavior_events (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    event_type  VARCHAR(50)  NOT NULL,
    entity_type VARCHAR(30),
    entity_id   BIGINT,
    metadata    TEXT,
    session_id  VARCHAR(36),
    occurred_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_behavior_user_type ON user_behavior_events (user_id, event_type);
CREATE INDEX idx_behavior_entity ON user_behavior_events (entity_type, entity_id);
