package com.gametime.client;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;

import com.gametime.client.BackendClient.ClientState;
import com.gametime.client.BackendClient.GameView;
import com.gametime.client.BackendClient.RequestView;
import com.gametime.client.BackendClient.SessionView;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Cliente del jugador: muestra la biblioteca, la cuenta regresiva y los avisos. Mientras haya una
 * sesión abierta o un juego en ejecución, cerrar la ventana la oculta en la bandeja del sistema.
 */
public class GameTimeClient extends Application {

    private static final String STYLE = """
            -fx-background-color: #111827;
            -fx-font-family: 'Segoe UI', system-ui, sans-serif;
            """;

    private final Label statusLabel = label("Conectando…", 16, "#d1d5db");
    private final Label countdownLabel = label("--:--", 48, "#f9fafb");
    private final Label requestLabel = label("", 13, "#d1d5db");
    private final Label exportLabel = label("", 13, "#d1d5db");
    private final Label messageLabel = label("", 13, "#fbbf24");
    private final ListView<GameView> gameList = new ListView<>();
    private final Button playButton = new Button("Jugar");
    private final Button requestButton = new Button("Solicitar otra sesión");
    private final Button addGameButton = new Button("Agregar juego");
    private final Button editGameButton = new Button("Editar juego");
    private final Button exportButton = new Button("Solicitar CSV del historial");
    private final Button downloadExportButton = new Button("Descargar CSV aprobado");

    private Stage stage;
    private SessionSupervisor supervisor;
    private BackendClient backend;
    private TrayIcon trayIcon;

    private volatile ClientState state;
    private volatile Instant stateReceivedAt = Instant.now();
    private volatile long downloadedExportId = -1;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        Platform.setImplicitExit(false);

        ClientConfig config;
        try {
            config = ClientConfig.load();
        } catch (IllegalStateException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR, exception.getMessage());
            alert.setHeaderText("No se pudo iniciar GameTime");
            alert.showAndWait();
            Platform.exit();
            return;
        }

        buildWindow();
        installTrayIcon();

        backend = new BackendClient(config);
        supervisor = new SessionSupervisor(backend, new SupervisorListener());
        supervisor.start();

        Timeline countdown = new Timeline(new KeyFrame(Duration.millis(250), event -> refreshCountdown()));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    private void buildWindow() {
        Label title = label("GameTime", 22, "#a7f3d0");
        title.setStyle(title.getStyle() + "-fx-font-weight: bold;");

        gameList.setPlaceholder(label("No hay juegos disponibles.", 13, "#9ca3af"));
        gameList.setStyle("-fx-control-inner-background: #1f2937; -fx-background-color: #1f2937; -fx-font-size: 14px;");
        VBox.setVgrow(gameList, Priority.ALWAYS);

        playButton.setDefaultButton(true);
        playButton.setDisable(true);
        playButton.setOnAction(event -> {
            GameView selected = gameList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                playButton.setDisable(true);
                supervisor.launch(selected);
            }
        });
        gameList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !playButton.isDisabled()) {
                playButton.fire();
            }
        });
        gameList.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> updateButtons());

        addGameButton.setOnAction(event -> showGameEditor(null));
        editGameButton.setDisable(true);
        editGameButton.setOnAction(event -> editSelectedGame());
        HBox libraryActions = new HBox(8, addGameButton, editGameButton);
        libraryActions.setAlignment(Pos.CENTER_LEFT);

        requestButton.setVisible(false);
        requestButton.managedProperty().bind(requestButton.visibleProperty());
        requestButton.setOnAction(event -> {
            requestButton.setDisable(true);
            supervisor.requestSession();
        });

        exportButton.setOnAction(event -> {
            downloadedExportId = -1;
            exportButton.setDisable(true);
            supervisor.requestExport();
        });
        downloadExportButton.setVisible(false);
        downloadExportButton.managedProperty().bind(downloadExportButton.visibleProperty());
        downloadExportButton.setOnAction(event -> downloadApprovedExport());

        HBox actions = new HBox(10, playButton, requestButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox exportActions = new HBox(8, exportButton, downloadExportButton);
        exportActions.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10, title, statusLabel, countdownLabel, requestLabel,
                label("Biblioteca", 14, "#9ca3af"), libraryActions, gameList, actions,
                label("Exportación de datos", 14, "#9ca3af"), exportActions, exportLabel, messageLabel);
        root.setPadding(new Insets(20));
        root.setStyle(STYLE);

        stage.setTitle("GameTime");
        stage.setScene(new Scene(root, 520, 560));
        stage.setOnCloseRequest(event -> {
            event.consume();
            hideOrExit();
        });
        stage.show();
    }

    private void hideOrExit() {
        if (supervisor != null && supervisor.mustStayInBackground()) {
            if (trayIcon != null) {
                stage.hide();
                notifyTray("GameTime sigue activo", "La sesión continúa en segundo plano. Abre GameTime desde la bandeja.");
            } else {
                stage.setIconified(true);
            }
            return;
        }
        exit();
    }

    private void exit() {
        if (supervisor != null) {
            supervisor.stop();
        }
        if (trayIcon != null) {
            TrayIcon icon = trayIcon;
            EventQueue.invokeLater(() -> SystemTray.getSystemTray().remove(icon));
        }
        Platform.exit();
        System.exit(0);
    }

    // ------------------------------------------------------------------ estado

    private void applyState(ClientState newState) {
        state = newState;
        stateReceivedAt = Instant.now();

        GameView selected = gameList.getSelectionModel().getSelectedItem();
        gameList.getItems().setAll(newState.games());
        if (selected != null) {
            newState.games().stream().filter(g -> g.id() == selected.id()).findFirst()
                    .ifPresent(g -> gameList.getSelectionModel().select(g));
        }

        SessionView session = newState.session();
        statusLabel.setText(session == null ? "No hay sesión. Pide una al administrador." : describe(session));
        requestButton.setVisible(newState.canRequestSession());
        requestButton.setDisable(false);
        requestLabel.setText(describe(newState.latestRequest(), session));
        BackendClient.ExportRequestView export = newState.latestExportRequest();
        exportButton.setDisable(export != null && ("PENDING".equals(export.status())
                || ("APPROVED".equals(export.status()) && downloadedExportId != export.id())));
        downloadExportButton.setVisible(export != null && "APPROVED".equals(export.status()));
        exportLabel.setText(describeExport(export));
        updateButtons();
        refreshCountdown();
    }

    private void updateButtons() {
        ClientState current = state;
        boolean canLaunch = current != null && current.canLaunch();
        GameView selected = gameList.getSelectionModel().getSelectedItem();
        playButton.setDisable(!canLaunch || selected == null || !selected.enabled()
                || !selected.processSupervisionApproved());
        editGameButton.setDisable(selected == null);
    }

    private void editSelectedGame() {
        GameView selected = gameList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        messageLabel.setText("Cargando los datos del juego…");
        javafx.concurrent.Task<BackendClient.GameCatalogView> task = new javafx.concurrent.Task<>() {
            @Override
            protected BackendClient.GameCatalogView call() throws Exception {
                return backend.gameCatalog().stream()
                        .filter(game -> game.id() == selected.id())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("El juego ya no existe en la biblioteca."));
            }
        };
        task.setOnSucceeded(event -> {
            messageLabel.setText("");
            showGameEditor(task.getValue());
        });
        task.setOnFailed(event -> showError(task.getException() == null
                ? "No se pudieron cargar los datos del juego."
                : task.getException().getMessage()));
        Thread worker = new Thread(task, "gametime-game-editor-load");
        worker.setDaemon(true);
        worker.start();
    }

    private void showGameEditor(BackendClient.GameCatalogView existing) {
        Dialog<BackendClient.GameInput> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle(existing == null ? "Agregar juego" : "Editar juego");
        dialog.setHeaderText("Datos que usará GameTime para mostrar e iniciar el juego");

        TextField name = new TextField(existing == null ? "" : existing.name());
        ComboBox<LauncherChoice> launcher = new ComboBox<>();
        launcher.getItems().setAll(
                new LauncherChoice("STEAM", "Steam"),
                new LauncherChoice("EPIC", "Epic Games"),
                new LauncherChoice("OTHER", "Otro ejecutable"));
        launcher.setValue(launcher.getItems().stream()
                .filter(option -> existing != null && option.code().equals(existing.launcher()))
                .findFirst().orElse(launcher.getItems().getFirst()));

        TextField target = new TextField(existing == null ? "" : existing.launchTarget());
        TextField process = new TextField(existing == null ? "" : existing.processName());
        TextField arguments = new TextField(existing == null ? "" : safe(existing.launchArguments()));
        TextField category = new TextField(existing == null ? "" : safe(existing.category()));
        TextField description = new TextField(existing == null ? "" : safe(existing.description()));
        CheckBox enabled = new CheckBox("Disponible para iniciar");
        enabled.setSelected(existing == null || existing.enabled());

        Button browse = new Button("Buscar…");
        browse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Selecciona el ejecutable o acceso directo del juego");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Ejecutables y accesos directos", "*.exe", "*.lnk"));
            File selected = chooser.showOpenDialog(stage);
            if (selected != null) {
                target.setText(selected.getAbsolutePath());
                if (selected.getName().toLowerCase().endsWith(".exe") && process.getText().isBlank()) {
                    process.setText(selected.getName());
                }
            }
        });

        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(9);
        fields.setPadding(new Insets(12, 0, 0, 0));
        fields.addRow(0, new Label("Nombre *"), name);
        fields.addRow(1, new Label("Launcher *"), launcher);
        fields.addRow(2, new Label("Destino *"), new HBox(8, target, browse));
        fields.addRow(3, new Label("Proceso .exe *"), process);
        fields.addRow(4, new Label("Argumentos"), arguments);
        fields.addRow(5, new Label("Categoría"), category);
        fields.addRow(6, new Label("Descripción"), description);
        fields.add(enabled, 1, 7);
        fields.add(label("Steam/Epic: pega una URI de inicio. Otro: selecciona un .exe o .lnk.", 12, "#9ca3af"), 1, 8);
        name.setPrefWidth(390);
        launcher.setPrefWidth(390);
        target.setPrefWidth(300);
        process.setPrefWidth(390);
        arguments.setPrefWidth(390);
        category.setPrefWidth(390);
        description.setPrefWidth(390);
        dialog.getDialogPane().setContent(fields);
        dialog.getDialogPane().setPrefWidth(650);

        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (name.getText().isBlank() || launcher.getValue() == null
                    || target.getText().isBlank() || process.getText().isBlank()) {
                event.consume();
                showError("Completa el nombre, launcher, destino y nombre del proceso .exe.");
            }
        });
        dialog.setResultConverter(button -> button == save
                ? new BackendClient.GameInput(name.getText(), launcher.getValue().code(), target.getText(),
                        blankToNull(arguments.getText()), process.getText(), blankToNull(category.getText()),
                        blankToNull(description.getText()), enabled.isSelected())
                : null);
        dialog.showAndWait().ifPresent(input -> saveGame(existing, input));
    }

    private void saveGame(BackendClient.GameCatalogView existing, BackendClient.GameInput input) {
        messageLabel.setText("Guardando juego…");
        javafx.concurrent.Task<BackendClient.GameCatalogView> task = new javafx.concurrent.Task<>() {
            @Override
            protected BackendClient.GameCatalogView call() throws Exception {
                return existing == null ? backend.createGame(input) : backend.updateGame(existing.id(), input);
            }
        };
        task.setOnSucceeded(event -> {
            messageLabel.setText(existing == null ? "Juego agregado a la biblioteca." : "Cambios guardados.");
            supervisor.refresh();
        });
        task.setOnFailed(event -> {
            String error = task.getException() == null ? "No se pudo guardar el juego." : task.getException().getMessage();
            messageLabel.setText("");
            showError(error);
        });
        Thread worker = new Thread(task, "gametime-game-editor-save");
        worker.setDaemon(true);
        worker.start();
    }

    private void downloadApprovedExport() {
        BackendClient.ExportRequestView export = state == null ? null : state.latestExportRequest();
        if (export == null || !"APPROVED".equals(export.status())) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar historial GameTime");
        chooser.setInitialFileName("gametime-sessions.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));
        File target = chooser.showSaveDialog(stage);
        if (target == null) {
            return;
        }
        messageLabel.setText("Descargando historial…");
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                byte[] content = backend.downloadExport(export.id());
                java.nio.file.Files.write(target.toPath(), content);
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            downloadedExportId = export.id();
            exportButton.setDisable(false);
            messageLabel.setText("CSV guardado en " + target.getAbsolutePath());
        });
        task.setOnFailed(event -> {
            messageLabel.setText("");
            showError(task.getException() == null ? "No se pudo guardar el CSV." : task.getException().getMessage());
        });
        Thread worker = new Thread(task, "gametime-export-download");
        worker.setDaemon(true);
        worker.start();
    }

    private static String describeExport(BackendClient.ExportRequestView export) {
        if (export == null) {
            return "El CSV incluye sesiones y juegos iniciados. El administrador debe autorizar la descarga.";
        }
        return switch (export.status()) {
            case "PENDING" -> "Solicitud enviada. Esperando la aprobación del administrador.";
            case "APPROVED" -> "Exportación aprobada. Ya puedes guardar el archivo CSV.";
            case "REJECTED" -> "La solicitud fue rechazada. "
                    + (export.decisionNote() == null ? "" : export.decisionNote());
            default -> "";
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Error desconocido." : message);
        alert.setHeaderText("GameTime");
        alert.showAndWait();
    }

    private record LauncherChoice(String code, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private void refreshCountdown() {
        ClientState current = state;
        SessionView session = current == null ? null : current.session();
        if (session == null) {
            countdownLabel.setText("--:--");
            return;
        }
        long elapsed = java.time.Duration.between(stateReceivedAt, Instant.now()).toSeconds();
        switch (session.status()) {
            case "ACTIVE" -> {
                countdownLabel.setText(format(Math.max(0, session.remainingSeconds() - elapsed)));
                countdownColor("#f9fafb");
            }
            case "GRACE", "ADMIN_ENDED" -> {
                long closeIn = session.closeInSeconds() == null ? 0 : Math.max(0, session.closeInSeconds() - elapsed);
                countdownLabel.setText(closeIn > 0 ? "Guarda: " + closeIn + " s" : "00:00");
                countdownColor("#fbbf24");
            }
            case "CREATED" -> {
                countdownLabel.setText(format(session.durationSeconds()));
                countdownColor("#9ca3af");
            }
            default -> {
                countdownLabel.setText("00:00");
                countdownColor("#9ca3af");
            }
        }
    }

    private void countdownColor(String color) {
        countdownLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }

    private static String describe(SessionView session) {
        return switch (session.status()) {
            case "CREATED" -> "Sesión lista. Espera a que el administrador inicie el temporizador.";
            case "ACTIVE" -> "Sesión en curso. Puedes cambiar de juego; el tiempo no se reinicia.";
            case "GRACE" -> "Se acabó el tiempo. Guarda tu partida.";
            case "ADMIN_ENDED" -> "El administrador terminó la sesión.";
            case "COMPLETED" -> "La sesión terminó. Para seguir jugando, solicita otra.";
            case "INTERRUPTED" -> "La sesión se interrumpió y no se reanuda. Puedes solicitar otra; el administrador debe crearla e iniciarla.";
            default -> session.statusLabel();
        };
    }

    private static String describe(RequestView request, SessionView session) {
        if (request == null) {
            return "";
        }
        return switch (request.status()) {
            case "PENDING" -> "Solicitud enviada: esperando respuesta del administrador.";
            case "REJECTED" -> session != null && session.isOpen() ? ""
                    : "Tu última solicitud fue rechazada"
                    + (request.decisionNote() == null ? "." : ": " + request.decisionNote());
            default -> "";
        };
    }

    private static String format(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    // ------------------------------------------------------------------ avisos

    private void showWarning(String title, String message) {
        Toolkit.getDefaultToolkit().beep();
        notifyTray(title, message);

        Stage popup = new Stage(StageStyle.UTILITY);
        popup.setAlwaysOnTop(true);
        popup.setTitle("GameTime");
        Label heading = label(title, 18, "#fbbf24");
        heading.setStyle(heading.getStyle() + "-fx-font-weight: bold;");
        Label body = label(message, 14, "#f9fafb");
        body.setWrapText(true);
        Button ok = new Button("Entendido");
        ok.setOnAction(event -> popup.close());
        VBox box = new VBox(12, heading, body, ok);
        box.setPadding(new Insets(20));
        box.setStyle(STYLE);
        popup.setScene(new Scene(box, 380, 180));
        popup.show();
        popup.toFront();

        // El aviso se cierra solo para no quedarse encima del juego.
        Timeline autoClose = new Timeline(new KeyFrame(Duration.seconds(12), event -> popup.close()));
        autoClose.play();
    }

    private void installTrayIcon() {
        if (!SystemTray.isSupported()) {
            return;
        }
        EventQueue.invokeLater(() -> {
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x34d399));
            g.fillOval(1, 1, 14, 14);
            g.setColor(new Color(0x111827));
            g.fillRect(7, 4, 2, 5);
            g.fillRect(7, 7, 4, 2);
            g.dispose();

            PopupMenu menu = new PopupMenu();
            MenuItem open = new MenuItem("Abrir GameTime");
            open.addActionListener(event -> Platform.runLater(this::showWindow));
            MenuItem quit = new MenuItem("Salir");
            quit.addActionListener(event -> Platform.runLater(this::tryQuitFromTray));
            menu.add(open);
            menu.add(quit);

            TrayIcon icon = new TrayIcon(image, "GameTime", menu);
            icon.setImageAutoSize(true);
            icon.addActionListener(event -> Platform.runLater(this::showWindow));
            try {
                SystemTray.getSystemTray().add(icon);
                trayIcon = icon;
            } catch (AWTException exception) {
                // Sin bandeja: la ventana se minimiza en lugar de ocultarse.
            }
        });
    }

    private void tryQuitFromTray() {
        if (supervisor.mustStayInBackground()) {
            showWindow();
            messageLabel.setText("No puedes salir mientras haya una sesión abierta o un juego en ejecución.");
            return;
        }
        exit();
    }

    private void showWindow() {
        stage.show();
        stage.setIconified(false);
        stage.toFront();
    }

    private void notifyTray(String title, String message) {
        TrayIcon icon = trayIcon;
        if (icon != null) {
            EventQueue.invokeLater(() -> icon.displayMessage(title, message, TrayIcon.MessageType.WARNING));
        }
    }

    private static Label label(String text, int size, String color) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: " + size + "px; -fx-text-fill: " + color + ";");
        return label;
    }

    private final class SupervisorListener implements SessionSupervisor.Listener {
        @Override
        public void onState(ClientState newState) {
            Platform.runLater(() -> {
                if (messageLabel.getText().startsWith("Sin conexión")) {
                    messageLabel.setText("");
                }
                applyState(newState);
            });
        }

        @Override
        public void onConnectionProblem(String message) {
            Platform.runLater(() -> {
                messageLabel.setText("Sin conexión: " + message);
                playButton.setDisable(true);
            });
        }

        @Override
        public void onWarning(String title, String message) {
            Platform.runLater(() -> showWarning(title, message));
        }

        @Override
        public void onInfo(String message) {
            Platform.runLater(() -> {
                messageLabel.setText(message);
                updateButtons();
            });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
