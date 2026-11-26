package com.gametime.backend.sessions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gametime.backend.common.BusinessRuleException;
import com.gametime.backend.games.Game;
import com.gametime.backend.games.GameService;

/**
 * Reglas de las sesiones de juego. El temporizador vive en el servidor: los estados se calculan
 * con el reloj de PostgreSQL y el cliente solo muestra la cuenta regresiva que recibe.
 */
@Service
public class SessionService {

    public static final int GRACE_SECONDS = 30;
    public static final int MIN_MINUTES = 1;
    public static final int MAX_MINUTES = 720;

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionService.class);

    private static final String SESSION_COLUMNS = """
            id, status, duration_seconds, created_at, started_at, expires_at, grace_until, ended_at, end_reason,
            CASE WHEN expires_at IS NULL THEN NULL
                 ELSE GREATEST(0, CEIL(EXTRACT(EPOCH FROM (expires_at - CURRENT_TIMESTAMP))))::int END AS remaining_seconds,
            CASE WHEN grace_until IS NULL THEN NULL
                 ELSE GREATEST(0, CEIL(EXTRACT(EPOCH FROM (grace_until - CURRENT_TIMESTAMP))))::int END AS close_in_seconds
            """;

    private static final String REQUEST_COLUMNS =
            "id, requested_at, status, decided_at, decision_note, created_session_id";

    private final JdbcClient jdbc;
    private final GameService games;
    private final long heartbeatTimeoutSeconds;

    public SessionService(
            JdbcClient jdbc,
            GameService games,
            @Value("${gametime.sessions.heartbeat-timeout:90s}") Duration heartbeatTimeout) {
        this.jdbc = jdbc;
        this.games = games;
        this.heartbeatTimeoutSeconds = heartbeatTimeout.toSeconds();
    }

    // ---------------------------------------------------------------- temporizador

    /**
     * Avanza los estados según el reloj: marca como interrumpidas las sesiones cuyo cliente dejó de
     * reportarse, pasa a gracia las que llegaron a cero y termina las que agotaron la gracia.
     */
    @Transactional
    public void advanceTimers() {
        int interrupted = jdbc.sql("""
                UPDATE play_sessions
                SET status = 'INTERRUPTED', interrupted_at = CURRENT_TIMESTAMP,
                    ended_at = CURRENT_TIMESTAMP, end_reason = 'SUPERVISION_LOST'
                WHERE status IN ('ACTIVE', 'GRACE')
                  AND COALESCE(last_heartbeat_at, started_at) < CURRENT_TIMESTAMP - (:timeout * INTERVAL '1 second')
                """)
                .param("timeout", heartbeatTimeoutSeconds)
                .update();
        int grace = jdbc.sql("""
                UPDATE play_sessions SET status = 'GRACE'
                WHERE status = 'ACTIVE' AND expires_at <= CURRENT_TIMESTAMP
                """).update();
        int completed = jdbc.sql("""
                UPDATE play_sessions
                SET status = 'COMPLETED', ended_at = grace_until, end_reason = 'TIME_EXPIRED'
                WHERE status = 'GRACE' AND grace_until <= CURRENT_TIMESTAMP
                """).update();

        if (interrupted > 0) {
            LOGGER.warn("Sesión marcada como interrumpida: el cliente no se reportó en {} s", heartbeatTimeoutSeconds);
        }
        if (grace > 0) {
            LOGGER.info("Sesión sin tiempo restante; comienza la gracia de {} s", GRACE_SECONDS);
        }
        if (completed > 0) {
            LOGGER.info("Sesión terminada por tiempo");
        }
    }

    // ---------------------------------------------------------------- consultas

    public Optional<PlaySession> findOpen() {
        return jdbc.sql("SELECT " + SESSION_COLUMNS
                        + " FROM play_sessions WHERE status IN ('CREATED', 'ACTIVE', 'GRACE')")
                .query(SessionService::mapSession)
                .optional();
    }

    /** La sesión más reciente, abierta o no. */
    public Optional<PlaySession> findLatest() {
        return jdbc.sql("SELECT " + SESSION_COLUMNS + " FROM play_sessions ORDER BY created_at DESC LIMIT 1")
                .query(SessionService::mapSession)
                .optional();
    }

    public List<PlaySession> findRecent(int limit) {
        return jdbc.sql("SELECT " + SESSION_COLUMNS + " FROM play_sessions ORDER BY created_at DESC LIMIT :limit")
                .param("limit", limit)
                .query(SessionService::mapSession)
                .list();
    }

    public Map<UUID, List<GameLaunch>> launchesBySession(List<UUID> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql("""
                        SELECT id, session_id, game_name, launched_at, finished_at, outcome
                        FROM game_launches WHERE session_id IN (:ids) ORDER BY launched_at
                        """)
                .param("ids", sessionIds)
                .query(SessionService::mapLaunch)
                .list()
                .stream()
                .collect(Collectors.groupingBy(GameLaunch::sessionId));
    }

    public Optional<SessionRequest> findPendingRequest() {
        return jdbc.sql("SELECT " + REQUEST_COLUMNS + " FROM session_requests WHERE status = 'PENDING'")
                .query(SessionService::mapRequest)
                .optional();
    }

    public Optional<SessionRequest> findLatestRequest() {
        return jdbc.sql("SELECT " + REQUEST_COLUMNS + " FROM session_requests ORDER BY requested_at DESC LIMIT 1")
                .query(SessionService::mapRequest)
                .optional();
    }

    // ---------------------------------------------------------------- acciones del administrador

    @Transactional
    public UUID create(int minutes) {
        validateMinutes(minutes);
        advanceTimers();
        try {
            UUID id = jdbc.sql("INSERT INTO play_sessions (duration_seconds) VALUES (:seconds) RETURNING id")
                    .param("seconds", minutes * 60)
                    .query(UUID.class)
                    .single();
            LOGGER.info("Sesión creada por el administrador ({} min)", minutes);
            return id;
        } catch (DuplicateKeyException exception) {
            throw new BusinessRuleException("Ya hay una sesión abierta. Termínala antes de crear otra.");
        }
    }

    @Transactional
    public void start(UUID id) {
        int updated = jdbc.sql("""
                UPDATE play_sessions
                SET status = 'ACTIVE',
                    started_at = CURRENT_TIMESTAMP,
                    expires_at = CURRENT_TIMESTAMP + duration_seconds * INTERVAL '1 second',
                    grace_until = CURRENT_TIMESTAMP + (duration_seconds + :grace) * INTERVAL '1 second',
                    last_heartbeat_at = CURRENT_TIMESTAMP
                WHERE id = :id AND status = 'CREATED'
                """)
                .param("id", id)
                .param("grace", GRACE_SECONDS)
                .update();
        if (updated == 0) {
            throw new BusinessRuleException("La sesión ya no está pendiente de iniciar.");
        }
        LOGGER.info("Temporizador iniciado por el administrador");
    }

    /**
     * Termina una sesión desde el panel. Una sesión creada se cancela; una sesión en curso pasa a
     * terminada y el cliente dispone de {@value #GRACE_SECONDS} s para que el jugador guarde.
     */
    @Transactional
    public void endByAdmin(UUID id) {
        advanceTimers();
        int cancelled = jdbc.sql("""
                UPDATE play_sessions
                SET status = 'CANCELLED', ended_at = CURRENT_TIMESTAMP, end_reason = 'ADMIN_CANCELLED'
                WHERE id = :id AND status = 'CREATED'
                """).param("id", id).update();
        if (cancelled > 0) {
            LOGGER.info("Sesión cancelada por el administrador antes de iniciar");
            return;
        }
        int ended = jdbc.sql("""
                UPDATE play_sessions
                SET status = 'ADMIN_ENDED', ended_at = CURRENT_TIMESTAMP, end_reason = 'ADMIN',
                    grace_until = LEAST(grace_until, CURRENT_TIMESTAMP + :grace * INTERVAL '1 second')
                WHERE id = :id AND status IN ('ACTIVE', 'GRACE')
                """)
                .param("id", id)
                .param("grace", GRACE_SECONDS)
                .update();
        if (ended == 0) {
            throw new BusinessRuleException("La sesión ya había terminado.");
        }
        LOGGER.info("Sesión terminada por el administrador");
    }

    @Transactional
    public UUID approveRequest(long requestId, int minutes) {
        validateMinutes(minutes);
        SessionRequest request = lockPendingRequest(requestId);
        UUID sessionId = create(minutes);
        jdbc.sql("""
                UPDATE session_requests
                SET status = 'APPROVED', decided_at = CURRENT_TIMESTAMP, created_session_id = :session
                WHERE id = :id
                """)
                .param("session", sessionId)
                .param("id", request.id())
                .update();
        LOGGER.info("Solicitud de sesión aprobada");
        return sessionId;
    }

    @Transactional
    public void rejectRequest(long requestId, String note) {
        SessionRequest request = lockPendingRequest(requestId);
        String trimmed = note == null || note.isBlank() ? null : note.trim();
        if (trimmed != null && trimmed.length() > 500) {
            throw new BusinessRuleException("El mensaje puede tener como máximo 500 caracteres.");
        }
        jdbc.sql("""
                UPDATE session_requests
                SET status = 'REJECTED', decided_at = CURRENT_TIMESTAMP, decision_note = :note
                WHERE id = :id
                """)
                .param("note", trimmed)
                .param("id", request.id())
                .update();
        LOGGER.info("Solicitud de sesión rechazada");
    }

    // ---------------------------------------------------------------- acciones del jugador (cliente)

    /** Registra que el cliente sigue supervisando la sesión en curso. */
    @Transactional
    public void heartbeat() {
        advanceTimers();
        jdbc.sql("UPDATE play_sessions SET last_heartbeat_at = CURRENT_TIMESTAMP WHERE status IN ('ACTIVE', 'GRACE')")
                .update();
    }

    @Transactional
    public SessionRequest requestNewSession() {
        advanceTimers();
        if (findOpen().isPresent()) {
            throw new BusinessRuleException("Todavía hay una sesión abierta; no hace falta pedir otra.");
        }
        try {
            SessionRequest request = jdbc.sql("INSERT INTO session_requests DEFAULT VALUES RETURNING " + REQUEST_COLUMNS)
                    .query(SessionService::mapRequest)
                    .single();
            LOGGER.info("El jugador solicitó una nueva sesión");
            return request;
        } catch (DuplicateKeyException exception) {
            throw new BusinessRuleException("Ya hay una solicitud pendiente de respuesta.");
        }
    }

    /** Autoriza el inicio de un juego solo si hay una sesión activa con tiempo restante. */
    @Transactional
    public LaunchTicket authorizeLaunch(long gameId) {
        advanceTimers();
        Optional<UUID> session = jdbc.sql("""
                        SELECT id FROM play_sessions
                        WHERE status = 'ACTIVE' AND expires_at > CURRENT_TIMESTAMP
                        FOR UPDATE
                        """)
                .query(UUID.class)
                .optional();
        if (session.isEmpty()) {
            throw new BusinessRuleException("No hay una sesión activa con tiempo disponible.");
        }
        Game game = games.findById(gameId)
                .filter(Game::enabled)
                .orElseThrow(() -> new BusinessRuleException("El juego no existe o está desactivado."));

        long launchId = jdbc.sql("""
                        INSERT INTO game_launches (session_id, game_id, game_name)
                        VALUES (:session, :game, :name) RETURNING id
                        """)
                .param("session", session.get())
                .param("game", game.id())
                .param("name", game.name())
                .query(Long.class)
                .single();
        LOGGER.info("Inicio autorizado: {}", game.name());
        return new LaunchTicket(launchId, game.id(), game.name(), game.launcher().name(),
                game.launchTarget(), game.launchArguments(), game.processName());
    }

    @Transactional
    public void reportLaunchOutcome(long launchId, LaunchOutcome outcome) {
        // Un juego que ignoró el cierre puede informarse después como cerrado por el jugador.
        int updated = jdbc.sql("""
                UPDATE game_launches
                SET outcome = :outcome,
                    finished_at = CASE WHEN :outcome = 'IGNORED_CLOSE' THEN NULL ELSE CURRENT_TIMESTAMP END
                WHERE id = :id AND (outcome IS NULL OR outcome = 'IGNORED_CLOSE')
                """)
                .param("outcome", outcome.name())
                .param("id", launchId)
                .update();
        if (updated > 0) {
            LOGGER.info("Resultado de juego informado: {}", outcome);
        }
    }

    // ---------------------------------------------------------------- utilidades

    private SessionRequest lockPendingRequest(long requestId) {
        return jdbc.sql("SELECT " + REQUEST_COLUMNS + " FROM session_requests WHERE id = :id AND status = 'PENDING' FOR UPDATE")
                .param("id", requestId)
                .query(SessionService::mapRequest)
                .optional()
                .orElseThrow(() -> new BusinessRuleException("La solicitud ya fue respondida."));
    }

    private static void validateMinutes(int minutes) {
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            throw new BusinessRuleException("La duración debe estar entre " + MIN_MINUTES + " y " + MAX_MINUTES + " minutos.");
        }
    }

    private static PlaySession mapSession(ResultSet rs, int row) throws SQLException {
        return new PlaySession(
                rs.getObject("id", UUID.class),
                SessionStatus.valueOf(rs.getString("status")),
                rs.getInt("duration_seconds"),
                localTime(rs, "created_at"),
                localTime(rs, "started_at"),
                localTime(rs, "expires_at"),
                localTime(rs, "grace_until"),
                localTime(rs, "ended_at"),
                rs.getString("end_reason"),
                (Integer) rs.getObject("remaining_seconds"),
                (Integer) rs.getObject("close_in_seconds"));
    }

    private static SessionRequest mapRequest(ResultSet rs, int row) throws SQLException {
        return new SessionRequest(
                rs.getLong("id"),
                localTime(rs, "requested_at"),
                rs.getString("status"),
                localTime(rs, "decided_at"),
                rs.getString("decision_note"),
                rs.getObject("created_session_id", UUID.class));
    }

    /** PostgreSQL devuelve las fechas en UTC; el panel las muestra en la zona horaria del equipo. */
    private static OffsetDateTime localTime(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static GameLaunch mapLaunch(ResultSet rs, int row) throws SQLException {
        String outcome = rs.getString("outcome");
        return new GameLaunch(
                rs.getLong("id"),
                rs.getObject("session_id", UUID.class),
                rs.getString("game_name"),
                localTime(rs, "launched_at"),
                localTime(rs, "finished_at"),
                outcome == null ? null : LaunchOutcome.valueOf(outcome));
    }
}
