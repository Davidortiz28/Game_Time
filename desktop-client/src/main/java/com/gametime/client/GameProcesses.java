package com.gametime.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gametime.client.BackendClient.LaunchTicket;

/** Inicia, detecta y pide el cierre normal de los juegos en Windows. Nunca fuerza el cierre. */
final class GameProcesses {

    private static final Pattern ARGUMENT = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    private GameProcesses() {
    }

    static void launch(LaunchTicket ticket) throws IOException {
        String target = ticket.launchTarget();
        String lower = target.toLowerCase(Locale.ROOT);
        List<String> command = new ArrayList<>();

        if ("OTHER".equals(ticket.launcher()) && lower.endsWith(".exe")) {
            command.add(target);
            if (ticket.launchArguments() != null) {
                Matcher matcher = ARGUMENT.matcher(ticket.launchArguments());
                while (matcher.find()) {
                    command.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
                }
            }
            ProcessBuilder builder = new ProcessBuilder(command);
            Path parent = Path.of(target).getParent();
            if (parent != null) {
                builder.directory(parent.toFile());
            }
            builder.start();
            return;
        }

        // URI de Steam/Epic o acceso directo .lnk: se abre con el manejador registrado en Windows,
        // sin pasar por cmd.exe para que caracteres como & no se interpreten.
        new ProcessBuilder("rundll32.exe", "url.dll,FileProtocolHandler", target).start();
    }

    static boolean isRunning(String processName) {
        try {
            Process process = new ProcessBuilder(
                    "tasklist.exe", "/FI", "IMAGENAME eq " + processName, "/FO", "CSV", "/NH")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
            process.waitFor(5, TimeUnit.SECONDS);
            return output.toLowerCase(Locale.ROOT).contains("\"" + processName.toLowerCase(Locale.ROOT) + "\"");
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Envía la solicitud de cierre normal (WM_CLOSE) sin /F: el juego puede guardar o ignorarla. */
    static void requestClose(String processName) {
        try {
            Process process = new ProcessBuilder("taskkill.exe", "/IM", processName)
                    .redirectErrorStream(true)
                    .start();
            process.getInputStream().readAllBytes();
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (IOException exception) {
            // Si no se pudo enviar, el seguimiento lo detectará como "no respondió".
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
