package com.gametime.backend.sessions;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionRequest(
        long id,
        OffsetDateTime requestedAt,
        String status,
        OffsetDateTime decidedAt,
        String decisionNote,
        UUID createdSessionId) {

    public boolean pending() {
        return "PENDING".equals(status);
    }

    public String statusLabel() {
        return switch (status) {
            case "PENDING" -> "Pendiente";
            case "APPROVED" -> "Aprobada";
            case "REJECTED" -> "Rechazada";
            default -> status;
        };
    }
}
