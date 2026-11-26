package com.gametime.supervisor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Headless supervisor entry point compatible with Apache Commons Daemon Procrun JVM mode. */
public final class GameTimeSupervisorService {

    private static final Logger LOGGER = Logger.getLogger(GameTimeSupervisorService.class.getName());
    private static final String TOKEN_HEADER = "X-GameTime-Client-Token";
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
    private static final Duration CLOSE_RESPONSE_WAIT = Duration.ofSeconds(20);
    private static final Object LOCK = new Object();
    private static RuntimeState runtime;

    private GameTimeSupervisorService() {
    }

    /** Procrun calls this method on service start and expects it to wait until stop is called. */
    public static void start(String[] args) throws InterruptedException {
        RuntimeState state;
        synchronized (LOCK) {
            if (runtime != null) {
                throw new IllegalStateException("El supervisor ya está iniciado.");
            }
            Config config = Config.load(args);
            state = new RuntimeState(config);
            runtime = state;
            state.executor.scheduleWithFixedDelay(state::tickSafely, 0,
                    POLL_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
        }
        LOGGER.info("GameTime Windows Supervisor iniciado");
        try {
            state.stopped.await();
        } finally {
            synchronized (LOCK) {
                if (runtime == state) {
                    runtime = null;
                }
            }
        }
    }

    /** Procrun calls this method from another thread to stop the service cleanly. */
    public static void stop(String[] args) {
        synchronized (LOCK) {
            if (runtime != null) {
                runtime.stop();
                runtime = null;
            }
        }
        LOGGER.info("GameTime Windows Supervisor detenido");
    }

    private static final class RuntimeState {
        private final Config config;
        private final HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build();
        private final ObjectMapper json = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "gametime-windows-supervisor");
            thread.setDaemon(false);
            return thread;
        });
        private final CountDownLatch stopped = new CountDownLatch(1);
        private final Map<Long, TrackedLaunch> launches = new HashMap<>();

        RuntimeState(Config config) {
            this.config = config;
        }

        void stop() {
            executor.shutdownNow();
            stopped.countDown();
        }

        void tickSafely() {
            try {
                tick();
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "No se pudo actualizar la supervisión; se reintentará", exception);
            }
        }

        void tick() throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.serverUrl + "/api/client/supervision"))
                    .timeout(Duration.ofSeconds(8))
                    .header(TOKEN_HEADER, config.token)
                    .GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("El backend respondió HTTP " + response.statusCode());
            }
            Snapshot snapshot = json.readValue(response.body(), Snapshot.class);
            if (snapshot.session() == null) {
                launches.clear();
                return;
            }
            for (Launch launch : snapshot.launches()) {
                supervise(snapshot.session(), launch);
            }
        }

        private void supervise(Session session, Launch launch) throws IOException, InterruptedException {
            if (launch.processName() == null || launch.processName().isBlank()
                    || !(launch.outcome() == null || "IGNORED_CLOSE".equals(launch.outcome()))) {
                launches.remove(launch.launchId());
                return;
            }

            TrackedLaunch tracked = launches.computeIfAbsent(launch.launchId(), ignored -> new TrackedLaunch());
            boolean running = isRunning(launch.processName());
            if (!running) {
                if (tracked.seenRunning || tracked.closeRequestedAt != null) {
                    report(launch.launchId(), tracked.closeRequestedAt == null ? "EXITED" : "CLOSED_NORMALLY");
                    launches.remove(launch.launchId());
                }
                return;
            }
            tracked.seenRunning = true;

            boolean closeDue = ("COMPLETED".equals(session.status()) || "ADMIN_ENDED".equals(session.status()))
                    && session.closeInSeconds() != null && session.closeInSeconds() == 0;
            if (closeDue && tracked.closeRequestedAt == null) {
                tracked.closeRequestedAt = Instant.now();
                requestNormalClose(launch.processName());
                LOGGER.info("Solicitud de cierre normal enviada al proceso " + launch.processName());
            } else if (tracked.closeRequestedAt != null && !tracked.ignoredReported
                    && Duration.between(tracked.closeRequestedAt, Instant.now()).compareTo(CLOSE_RESPONSE_WAIT) >= 0) {
                tracked.ignoredReported = true;
                report(launch.launchId(), "IGNORED_CLOSE");
                LOGGER.warning("El proceso ignoró el cierre normal; no se forzará: " + launch.processName());
            }
        }

        private boolean isRunning(String processName) throws IOException, InterruptedException {
            Process process = new ProcessBuilder("tasklist.exe", "/FI", "IMAGENAME eq " + processName,
                    "/FO", "CSV", "/NH").redirectErrorStream(true).start();
            byte[] output = process.getInputStream().readAllBytes();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return new String(output, Charset.defaultCharset()).toLowerCase(java.util.Locale.ROOT)
                    .contains("\"" + processName.toLowerCase(java.util.Locale.ROOT) + "\"");
        }

        private void requestNormalClose(String processName) throws IOException, InterruptedException {
            Process process = new ProcessBuilder("taskkill.exe", "/IM", processName)
                    .redirectErrorStream(true).start();
            process.getInputStream().readAllBytes();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }

        private void report(long launchId, String outcome) {
            try {
                String body = json.writeValueAsString(Map.of("outcome", outcome));
                HttpRequest request = HttpRequest.newBuilder(URI.create(config.serverUrl
                                + "/api/client/launches/" + launchId + "/outcome"))
                        .timeout(Duration.ofSeconds(5)).header(TOKEN_HEADER, config.token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                http.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "No se pudo registrar el resultado del proceso " + launchId, exception);
            }
        }
    }

    private static final class TrackedLaunch {
        boolean seenRunning;
        Instant closeRequestedAt;
        boolean ignoredReported;
    }

    private record Snapshot(Session session, List<Launch> launches) {
    }

    private record Session(String status, Integer closeInSeconds) {
    }

    private record Launch(long launchId, String processName, String outcome) {
    }

    private record Config(String serverUrl, String token) {
        static Config load(String[] args) {
            if (args == null || args.length == 0 || args[0].isBlank()) {
                throw new IllegalArgumentException("Procrun debe pasar la ruta a supervisor.properties.");
            }
            Path file = Path.of(args[0]).toAbsolutePath().normalize();
            if (!Files.isRegularFile(file)) {
                throw new IllegalArgumentException("No existe el archivo de configuración: " + file);
            }
            Properties properties = new Properties();
            try (var input = Files.newInputStream(file)) {
                properties.load(input);
            } catch (IOException exception) {
                throw new IllegalStateException("No se pudo leer la configuración del supervisor.", exception);
            }
            String url = properties.getProperty("gametime.server-url", "http://127.0.0.1:8080").trim()
                    .replaceAll("/+$", "");
            String token = properties.getProperty("gametime.client-token", "").trim();
            if (token.length() < 32) {
                throw new IllegalArgumentException("El token del supervisor debe tener al menos 32 caracteres.");
            }
            return new Config(url, token);
        }
    }
}
