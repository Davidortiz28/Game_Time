package com.gametime.backend.sessions;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GameLaunch(
        long id,
        UUID sessionId,
        String gameName,
        OffsetDateTime launchedAt,
        OffsetDateTime finishedAt,
        LaunchOutcome outcome) {

    public String outcomeLabel() {
        return outcome == null ? "En ejecución o sin informar" : outcome.getLabel();
    }
}
