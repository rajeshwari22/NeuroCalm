-- NeuroCalm initial schema

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE massage_sessions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id),
    start_time       TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time         TIMESTAMP WITH TIME ZONE,
    status           VARCHAR(20) NOT NULL,
    intensity        VARCHAR(20),
    rhythm           VARCHAR(100),
    duration_seconds BIGINT
);
CREATE INDEX idx_sessions_user ON massage_sessions(user_id);

-- Raw PPG samples are always kept. Quality / motor flag are derived values.
CREATE TABLE ppg_readings (
    id                    BIGSERIAL PRIMARY KEY,
    session_id            BIGINT NOT NULL REFERENCES massage_sessions(id) ON DELETE CASCADE,
    recorded_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    raw_value             DOUBLE PRECISION NOT NULL,
    bvp                   DOUBLE PRECISION,
    heart_rate            INTEGER,
    heart_rate_estimated  BOOLEAN NOT NULL DEFAULT FALSE,
    quality               VARCHAR(20) NOT NULL,
    motor_active_during   BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_ppg_session_time ON ppg_readings(session_id, recorded_at);

CREATE TABLE motor_events (
    id             BIGSERIAL PRIMARY KEY,
    session_id     BIGINT NOT NULL REFERENCES massage_sessions(id) ON DELETE CASCADE,
    zone           VARCHAR(20) NOT NULL,
    activity_type  VARCHAR(30) NOT NULL,
    pwm_duty       INTEGER NOT NULL,
    rhythm         VARCHAR(100),
    event_time     TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_motor_session_time ON motor_events(session_id, event_time);

CREATE TABLE session_summaries (
    id                                BIGSERIAL PRIMARY KEY,
    session_id                        BIGINT NOT NULL UNIQUE REFERENCES massage_sessions(id) ON DELETE CASCADE,
    duration_seconds                  BIGINT NOT NULL,
    total_readings                    BIGINT NOT NULL,
    good_readings                     BIGINT NOT NULL,
    moderate_readings                 BIGINT NOT NULL,
    poor_readings                     BIGINT NOT NULL,
    readings_during_motor_activity    BIGINT NOT NULL,
    poor_readings_during_motor        BIGINT NOT NULL,
    average_heart_rate                DOUBLE PRECISION,
    peak_heart_rate                   INTEGER,
    note                              VARCHAR(500),
    created_at                        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
