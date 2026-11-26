package com.gametime.backend.sessions;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Sesión de juego. Los segundos restantes se calculan con el reloj de PostgreSQL al leer.
 *
 * @param remainingSeconds segundos hasta que se agote el tiempo (solo en sesiones iniciadas)
 * @param closeInSeconds   segundos hasta que el cliente debe solicitar el cierre de los juegos
 */
public record PlaySession(
        UUID id,
        SessionStatus status,
        int durationSeconds,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime graceUntil,
        OffsetDateTime endedAt,
        String endReason,
        Integer remainingSeconds,
        Integer closeInSeconds) {

    public int durationMinutes() {
        return durationSeconds / 60;
    }

    public String endReasonLabel() {
        if (endReason == null) {
            return "";
        }
        return switch (endReason) {
            case "TIME_EXPIRED" -> "Se agotó el tiempo";
            case "ADMIN" -> "Terminada por el administrador";
            case "ADMIN_CANCELLED" -> "Cancelada por el administrador";
            case "SUPERVISION_LOST" -> "El cliente dejó de reportarse (reinicio o fallo)";
            default -> endReason;
        };
    }
}
