package com.gametime.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

/** Comunicación con la API local /api/client del backend. */
final class BackendClient {

    private static final String TOKEN_HEADER = "X-GameTime-Client-Token";

    private final ClientConfig config;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper json = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    BackendClient(ClientConfig config) {
        this.config = config;
    }

    ClientState heartbeat() throws BackendException {
        return post("/api/client/heartbeat", null, ClientState.class);
    }

    LaunchTicket authorizeLaunch(long gameId) throws BackendException {
        return post("/api/client/launches", Map.of("gameId", gameId), LaunchTicket.class);
    }

    void reportOutcome(long launchId, String outcome) throws BackendException {
        post("/api/client/launches/" + launchId + "/outcome", Map.of("outcome", outcome), Void.class);
    }

    RequestView requestSession() throws BackendException {
        return post("/api/client/session-requests", null, RequestView.class);
    }

    List<GameCatalogView> gameCatalog() throws BackendException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.serverUrl() + "/api/client/games"))
                    .timeout(Duration.ofSeconds(5))
                    .header(TOKEN_HEADER, config.token())
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response);
            return json.readValue(response.body(), new TypeReference<>() {});
        } catch (IOException exception) {
            throw new BackendException("No se pudo cargar la biblioteca.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BackendException("Operación interrumpida.", exception);
        }
    }

    GameCatalogView createGame(GameInput input) throws BackendException {
        return post("/api/client/games", input, GameCatalogView.class);
    }

    GameCatalogView updateGame(long id, GameInput input) throws BackendException {
        return post("/api/client/games/" + id, input, GameCatalogView.class);
    }

    private <T> T post(String path, Object body, Class<T> type) throws BackendException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.serverUrl() + path))
                    .timeout(Duration.ofSeconds(5))
                    .header(TOKEN_HEADER, config.token())
                    .header("Content-Type", "application/json")
                    .POST(body == null
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response);
            if (type == Void.class || response.body().isBlank()) {
                return null;
            }
            return json.readValue(response.body(), type);
        } catch (IOException exception) {
            throw new BackendException("No hay conexión con el backend de GameTime.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BackendException("Operación interrumpida.", exception);
        }
    }

    private void ensureSuccess(HttpResponse<String> response) throws IOException, BackendException {
        int status = response.statusCode();
        if (status == 401) {
            throw new BackendException("El backend rechazó el token del cliente. Revisa CLIENT_API_TOKEN.");
        }
        if (status == 409) {
            JsonNode node = json.readTree(response.body());
            throw new BackendException(node.path("message").asText("Acción no permitida."));
        }
        if (status >= 400) {
            String detail = "";
            try {
                JsonNode node = json.readTree(response.body());
                detail = node.path("message").asText(node.path("detail").asText(""));
            } catch (IOException ignored) {
                // Keep a concise status message if the server did not return JSON.
            }
            throw new BackendException(detail.isBlank()
                    ? "El backend respondió con un error (" + status + ")."
                    : detail);
        }
    }

    static final class BackendException extends Exception {
        BackendException(String message) {
            super(message);
        }

        BackendException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    record ClientState(
            SessionView session,
            boolean canLaunch,
            boolean canRequestSession,
            RequestView latestRequest,
            List<GameView> games) {
    }

    record SessionView(
            String id,
            String status,
            String statusLabel,
            int durationSeconds,
            Integer remainingSeconds,
            Integer closeInSeconds) {

        boolean isOpen() {
            return "CREATED".equals(status) || "ACTIVE".equals(status) || "GRACE".equals(status);
        }
    }

    record GameView(long id, String name, String launcher, String category, boolean enabled) {
        @Override
        public String toString() {
            String details = category == null ? name + "  ·  " + launcher : name + "  ·  " + launcher + "  ·  " + category;
            return enabled ? details : details + "  ·  Desactivado";
        }
    }

    record GameCatalogView(long id, String name, String launcher, String launchTarget,
            String launchArguments, String processName, String category, String description, boolean enabled) {
    }

    record GameInput(String name, String launcher, String launchTarget, String launchArguments,
            String processName, String category, String description, boolean enabled) {
    }

    record RequestView(long id, String status, String decisionNote) {
    }

    record LaunchTicket(
            long launchId,
            long gameId,
            String gameName,
            String launcher,
            String launchTarget,
            String launchArguments,
            String processName) {
    }
}
