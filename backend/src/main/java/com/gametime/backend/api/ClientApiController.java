package com.gametime.backend.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gametime.backend.common.BusinessRuleException;
import com.gametime.backend.games.GameService;
import com.gametime.backend.sessions.LaunchOutcome;
import com.gametime.backend.sessions.LaunchTicket;
import com.gametime.backend.sessions.PlaySession;
import com.gametime.backend.sessions.SessionRequest;
import com.gametime.backend.sessions.SessionService;
import com.gametime.backend.sessions.SessionStatus;

/**
 * API local que usa el cliente JavaFX del jugador. Solo expone acciones de jugador; las decisiones
 * sobre sesiones las toma el administrador desde el panel web.
 */
@RestController
@RequestMapping("/api/client")
public class ClientApiController {

    private final SessionService sessions;
    private final GameService games;

    public ClientApiController(SessionService sessions, GameService games) {
        this.sessions = sessions;
        this.games = games;
    }

    /** Señal de vida del cliente; devuelve el estado que debe mostrar. */
    @PostMapping("/heartbeat")
    public ClientState heartbeat() {
        sessions.heartbeat();
        Optional<PlaySession> latest = sessions.findLatest();
        Optional<SessionRequest> request = sessions.findLatestRequest();

        boolean active = latest.map(s -> s.status() == SessionStatus.ACTIVE).orElse(false);
        boolean open = latest.map(s -> s.status().isOpen()).orElse(false);
        boolean pending = request.map(SessionRequest::pending).orElse(false);

        List<GameView> gameViews = games.findAll().stream()
                .map(g -> new GameView(g.id(), g.name(), g.launcher().getLabel(), g.category(), g.enabled()))
                .toList();

        return new ClientState(
                latest.map(ClientApiController::toView).orElse(null),
                active,
                !open && !pending,
                request.map(r -> new RequestView(r.id(), r.status(), r.decisionNote())).orElse(null),
                gameViews);
    }

    @PostMapping("/launches")
    public LaunchTicket launch(@RequestBody LaunchRequest body) {
        if (body == null || body.gameId() == null) {
            throw new BusinessRuleException("Falta el juego.");
        }
        return sessions.authorizeLaunch(body.gameId());
    }

    @PostMapping("/launches/{id}/outcome")
    public ResponseEntity<Void> outcome(@PathVariable long id, @RequestBody OutcomeRequest body) {
        if (body == null || body.outcome() == null) {
            throw new BusinessRuleException("Falta el resultado.");
        }
        sessions.reportLaunchOutcome(id, body.outcome());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/session-requests")
    public RequestView requestSession() {
        SessionRequest request = sessions.requestNewSession();
        return new RequestView(request.id(), request.status(), request.decisionNote());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<Map<String, String>> rejected(BusinessRuleException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }

    private static SessionView toView(PlaySession session) {
        return new SessionView(
                session.id().toString(),
                session.status().name(),
                session.status().getLabel(),
                session.durationSeconds(),
                session.remainingSeconds(),
                session.closeInSeconds());
    }

    public record ClientState(
            SessionView session,
            boolean canLaunch,
            boolean canRequestSession,
            RequestView latestRequest,
            List<GameView> games) {
    }

    public record SessionView(
            String id,
            String status,
            String statusLabel,
            int durationSeconds,
            Integer remainingSeconds,
            Integer closeInSeconds) {
    }

    public record GameView(long id, String name, String launcher, String category, boolean enabled) {
    }

    public record RequestView(long id, String status, String decisionNote) {
    }

    public record LaunchRequest(Long gameId) {
    }

    public record OutcomeRequest(LaunchOutcome outcome) {
    }
}
