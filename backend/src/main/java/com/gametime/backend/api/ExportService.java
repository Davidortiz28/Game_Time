package com.gametime.backend.api;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gametime.backend.common.BusinessRuleException;

@Service
public class ExportService {

    private static final String COLUMNS = "id, requested_at, status, decided_at, decision_note";
    private final JdbcClient jdbc;

    public ExportService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ExportRequest> findPending() {
        return jdbc.sql("SELECT " + COLUMNS + " FROM export_requests WHERE status = 'PENDING'")
                .query(ExportService::map).optional();
    }

    public Optional<ExportRequest> findLatest() {
        return jdbc.sql("SELECT " + COLUMNS + " FROM export_requests ORDER BY requested_at DESC LIMIT 1")
                .query(ExportService::map).optional();
    }

    @Transactional
    public ExportRequest request() {
        try {
            return jdbc.sql("INSERT INTO export_requests DEFAULT VALUES RETURNING " + COLUMNS)
                    .query(ExportService::map).single();
        } catch (DuplicateKeyException exception) {
            throw new BusinessRuleException("Ya hay una solicitud de exportación pendiente.");
        }
    }

    @Transactional
    public void approve(long id, boolean confirmed) {
        if (!confirmed) {
            throw new BusinessRuleException("Confirma que quieres aprobar la exportación.");
        }
        decide(id, "APPROVED", null);
    }

    @Transactional
    public void reject(long id, boolean confirmed, String note) {
        if (!confirmed) {
            throw new BusinessRuleException("Confirma que quieres rechazar la exportación.");
        }
        String trimmed = note == null || note.isBlank() ? null : note.trim();
        if (trimmed != null && trimmed.length() > 500) {
            throw new BusinessRuleException("El mensaje puede tener como máximo 500 caracteres.");
        }
        decide(id, "REJECTED", trimmed);
    }

    public byte[] download(long id) {
        String status = jdbc.sql("SELECT status FROM export_requests WHERE id = :id")
                .param("id", id).query(String.class).optional()
                .orElseThrow(() -> new BusinessRuleException("La solicitud de exportación no existe."));
        if (!"APPROVED".equals(status)) {
            throw new BusinessRuleException("El administrador aún no ha aprobado esta exportación.");
        }

        List<String> rows = jdbc.sql("""
                SELECT s.id, s.created_at, s.started_at, s.ended_at, s.duration_seconds,
                       s.status, s.end_reason, l.game_name, l.launched_at, l.finished_at, l.outcome
                FROM play_sessions s
                LEFT JOIN game_launches l ON l.session_id = s.id
                ORDER BY s.created_at, l.launched_at
                """).query((rs, row) -> csvRow(
                rs.getString("id"), rs.getString("created_at"), rs.getString("started_at"),
                rs.getString("ended_at"), Integer.toString(rs.getInt("duration_seconds")),
                rs.getString("status"), rs.getString("end_reason"), rs.getString("game_name"),
                rs.getString("launched_at"), rs.getString("finished_at"), rs.getString("outcome")))
                .list();

        StringBuilder csv = new StringBuilder("session_id,created_at,started_at,ended_at,planned_seconds,status,end_reason,game_name,game_launched_at,game_finished_at,game_outcome\r\n");
        rows.forEach(row -> csv.append(row).append("\r\n"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void decide(long id, String status, String note) {
        int updated = jdbc.sql("""
                UPDATE export_requests SET status = :status, decided_at = CURRENT_TIMESTAMP, decision_note = :note
                WHERE id = :id AND status = 'PENDING'
                """).param("status", status).param("note", note).param("id", id).update();
        if (updated == 0) {
            throw new BusinessRuleException("La solicitud ya fue respondida o no existe.");
        }
    }

    private static String csvRow(String... values) {
        return Arrays.stream(values).map(ExportService::escape).collect(Collectors.joining(","));
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        String safe = value;
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static ExportRequest map(ResultSet rs, int row) throws SQLException {
        return new ExportRequest(rs.getLong("id"), rs.getObject("requested_at", OffsetDateTime.class),
                rs.getString("status"), rs.getObject("decided_at", OffsetDateTime.class), rs.getString("decision_note"));
    }
}
