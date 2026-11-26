CREATE TABLE supervisor_presence (
    singleton_id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (singleton_id = 1),
    last_seen_at TIMESTAMPTZ NOT NULL
);
