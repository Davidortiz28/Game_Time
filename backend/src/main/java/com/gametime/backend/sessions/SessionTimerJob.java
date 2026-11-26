package com.gametime.backend.sessions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Avanza los temporizadores aunque nadie consulte el panel ni el cliente. */
@Component
class SessionTimerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionTimerJob.class);

    private final SessionService sessions;

    SessionTimerJob(SessionService sessions) {
        this.sessions = sessions;
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
    void tick() {
        try {
            sessions.advanceTimers();
        } catch (RuntimeException exception) {
            LOGGER.error("No se pudieron actualizar los temporizadores", exception);
        }
    }
}
