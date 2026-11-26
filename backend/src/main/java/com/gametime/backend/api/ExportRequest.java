package com.gametime.backend.api;

import java.time.OffsetDateTime;

public record ExportRequest(long id, OffsetDateTime requestedAt, String status,
        OffsetDateTime decidedAt, String decisionNote) {
}
