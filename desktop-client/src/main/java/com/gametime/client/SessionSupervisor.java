package com.gametime.client;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.gametime.client.BackendClient.BackendException;
import com.gametime.client.BackendClient.ClientState;
import com.gametime.client.BackendClient.GameView;
import com.gametime.client.BackendClient.LaunchTicket;
import com.gametime.client.BackendClient.SessionView;

/**
 * Supervisa la sesión desde el escritorio del jugador: envía la señal de vida al backend, emite los
 * avisos de 10, 5 y 1 minuto, y al terminar la gracia pide el cierre normal de los juegos que
 * GameTime inició. Toda la lógica corre en un único hilo para evitar condiciones de carrera.
 */
final class SessionSupervisor {

    static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
    private static final Duration CLOSE_RESPONSE_WAIT = Duration.ofSeconds(20);
    private static final Duration NEVER_STARTED_TIMEOUT = Duration.ofMinutes(10);
    private static final int[] WARNING_SECONDS = {600, 300, 60};

    interface Listener {
        void onState(ClientState state);

        void onConnectionProblem(String message);

        void onWarning(String title, String message);

        void onInfo(String message);
    }

    private final BackendClient backend;
    private final Listener listener;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "gametime-supervisor");
        thread.setDaemon(true);
        return thread;
    });

    private final List<TrackedGame> tracked = new ArrayList<>();
    private final Set<String> warningsShown = new HashSet<>();
    private volatile ClientState lastState;
    private volatile int runningGames;

    SessionSupervisor(BackendClient backend, Listener listener) {
        this.backend = backend;
        this.listener = listener;
    }

    void start() {
        executor.scheduleWithFixedDelay(this::tick, 0, POLL_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    void refresh() {
        executor.execute(this::tick);
    }

    void stop() {
        executor.shutdownNow();
    }

    /** Hay una sesión abierta o algún juego iniciado por GameTime sigue en ejecución. */
    boolean mustStayInBackground() {
        ClientState state = lastState;
        boolean open = state != null && state.session() != null && state.session().isOpen();
        return open || runningGames > 0;
    }

    void launch(GameView game) {
        executor.execute(() -> {
            LaunchTicket ticket;
            try {
                ticket = backend.authorizeLaunch(game.id());
            } catch (BackendException exception) {
                listener.onInfo(exception.getMessage());
                return;
            }
            try {
                GameProcesses.launch(ticket);
                tracked.add(new TrackedGame(ticket));
                runningGames = tracked.size();
                listener.onInfo("Iniciando " + ticket.gameName() + "…");
            } catch (IOException exception) {
                report(ticket.launchId(), "LAUNCH_FAILED");
                listener.onInfo("No se pudo iniciar " + ticket.gameName() + ": " + exception.getMessage());
            }
        });
    }

    void requestSession() {
        executor.execute(() -> {
            try {
                backend.requestSession();
                listener.onInfo("Solicitud enviada. Espera la respuesta del administrador.");
                tick();
            } catch (BackendException exception) {
                listener.onInfo(exception.getMessage());
            }
        });
    }

    void requestExport() {
        executor.execute(() -> {
            try {
                backend.requestExport();
                listener.onInfo("Solicitud CSV enviada. El administrador debe aprobarla.");
                tick();
            } catch (BackendException exception) {
                listener.onInfo(exception.getMessage());
            }
        });
    }

    private void tick() {
        ClientState state;
        try {
            state = backend.heartbeat();
        } catch (BackendException exception) {
            listener.onConnectionProblem(exception.getMessage());
            superviseGames(null);
            return;
        } catch (RuntimeException exception) {
            listener.onConnectionProblem("Error inesperado: " + exception.getMessage());
            return;
        }
        lastState = state;
        listener.onState(state);
        emitWarnings(state.session());
        superviseGames(state.session());
    }

    private void emitWarnings(SessionView session) {
        if (session == null) {
            return;
        }
        String id = session.id();
        switch (session.status()) {
            case "ACTIVE" -> {
                int remaining = session.remainingSeconds() == null ? Integer.MAX_VALUE : session.remainingSeconds();
                // Solo el umbral más cercano: si el cliente arranca tarde no se acumulan avisos.
                for (int i = WARNING_SECONDS.length - 1; i >= 0; i--) {
                    int threshold = WARNING_SECONDS[i];
                    if (remaining <= threshold && session.durationSeconds() > threshold) {
                        if (warningsShown.add(id + ":" + threshold)) {
                            for (int j = 0; j < i; j++) {
                                warningsShown.add(id + ":" + WARNING_SECONDS[j]);
                            }
                            int minutes = threshold / 60;
                            listener.onWarning("Quedan " + minutes + (minutes == 1 ? " minuto" : " minutos"),
                                    "Ve buscando un buen momento para guardar tu partida.");
                        }
                        break;
                    }
                }
            }
            case "GRACE" -> {
                if (warningsShown.add(id + ":grace")) {
                    listener.onWarning("Se acabó el tiempo",
                            "Tienes " + session.closeInSeconds() + " segundos para guardar. Después GameTime pedirá cerrar el juego.");
                }
            }
            case "ADMIN_ENDED" -> {
                Integer closeIn = session.closeInSeconds();
                if (closeIn != null && closeIn > 0 && warningsShown.add(id + ":admin")) {
                    listener.onWarning("El administrador terminó la sesión",
                            "Tienes " + closeIn + " segundos para guardar. Después GameTime pedirá cerrar el juego.");
                }
            }
            default -> {
                // Sin avisos para el resto de estados.
            }
        }
    }

    private void superviseGames(SessionView session) {
        boolean closeTime = session != null
                && ("COMPLETED".equals(session.status()) || "ADMIN_ENDED".equals(session.status()))
                && session.closeInSeconds() != null && session.closeInSeconds() == 0;
        Instant now = Instant.now();

        for (TrackedGame game : List.copyOf(tracked)) {
            boolean running = GameProcesses.isRunning(game.ticket.processName());
            if (running) {
                game.seenRunning = true;
            }

            if (!running && (game.seenRunning || game.closeRequestedAt != null)) {
                String outcome = game.closeRequestedAt != null && !game.ignoredReported ? "CLOSED_NORMALLY" : "EXITED";
                report(game.ticket.launchId(), outcome);
                tracked.remove(game);
                continue;
            }
            if (!running && Duration.between(game.launchedAt, now).compareTo(NEVER_STARTED_TIMEOUT) > 0) {
                // El proceso nunca apareció: probablemente el nombre del proceso está mal configurado.
                listener.onInfo("No se detectó " + game.ticket.processName()
                        + ". Pide al administrador que revise el nombre del proceso.");
                tracked.remove(game);
                continue;
            }

            if (closeTime && running && game.closeRequestedAt == null) {
                game.closeRequestedAt = now;
                GameProcesses.requestClose(game.ticket.processName());
                listener.onInfo("Pidiendo a " + game.ticket.gameName() + " que se cierre…");
            } else if (running && game.closeRequestedAt != null && !game.ignoredReported
                    && Duration.between(game.closeRequestedAt, now).compareTo(CLOSE_RESPONSE_WAIT) > 0) {
                game.ignoredReported = true;
                report(game.ticket.launchId(), "IGNORED_CLOSE");
                listener.onWarning(game.ticket.gameName() + " no se cerró",
                        "El juego no respondió al cierre normal y GameTime no lo forzará. Guarda y ciérralo tú.");
            }
        }
        runningGames = tracked.size();
    }

    private void report(long launchId, String outcome) {
        try {
            backend.reportOutcome(launchId, outcome);
        } catch (BackendException exception) {
            // Si el backend no está disponible se pierde el detalle, no el control del juego.
        }
    }

    private static final class TrackedGame {
        final LaunchTicket ticket;
        final Instant launchedAt = Instant.now();
        boolean seenRunning;
        Instant closeRequestedAt;
        boolean ignoredReported;

        TrackedGame(LaunchTicket ticket) {
            this.ticket = ticket;
        }
    }
}
