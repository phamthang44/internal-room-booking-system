CREATE TABLE user_room_preference_scores (
    id             BIGSERIAL      PRIMARY KEY,
    user_id        BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    classroom_id   BIGINT         NOT NULL REFERENCES classrooms (id)    ON DELETE CASCADE,
    view_count     INT            NOT NULL DEFAULT 0,
    click_count    INT            NOT NULL DEFAULT 0,
    dismiss_count  INT            NOT NULL DEFAULT 0,
    cancel_count   INT            NOT NULL DEFAULT 0,
    avg_rating     NUMERIC(3, 2),
    behavior_score NUMERIC(8, 2)  NOT NULL DEFAULT 0,
    computed_at    TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_user_classroom_pref UNIQUE (user_id, classroom_id)
);
