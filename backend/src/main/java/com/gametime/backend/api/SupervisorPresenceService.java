package com.gametime.backend.api;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupervisorPresenceService {

    private static final long ACTIVE_WINDOW_SECONDS = 10;
    private final JdbcClient jdbc;

    public SupervisorPresenceService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void markSeen() {
        jdbc.sql("""
                INSERT INTO supervisor_presence (singleton_id, last_seen_at)
                VALUES (1, CURRENT_TIMESTAMP)
                ON CONFLICT (singleton_id) DO UPDATE SET last_seen_at = EXCLUDED.last_seen_at
                """).update();
    }

    public boolean isActive() {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM supervisor_presence
                    WHERE singleton_id = 1
                      AND last_seen_at >= CURRENT_TIMESTAMP - (:window * INTERVAL '1 second')
                )
                """).param("window", ACTIVE_WINDOW_SECONDS).query(Boolean.class).single();
    }
}
