package com.gametime.backend.setup;

import java.io.Console;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

import org.flywaydb.core.Flyway;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class AdminSetupCommand {

    private static final String USAGE = "Uso: setup o reset-password";
    private static final int MIN_PASSWORD_LENGTH = 15;
    private static final int MAX_PASSWORD_LENGTH = 256;

    private AdminSetupCommand() {
    }

    public static void main(String[] args) throws Exception {
        try {
            run(args);
        } catch (Exception exception) {
            System.err.println("No se pudo configurar la cuenta: " + exception.getMessage());
            throw new IllegalStateException("Configuración de administrador fallida", exception);
        }
    }

    private static void run(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("setup") || args[0].equals("reset-password"))) {
            throw new IllegalArgumentException(USAGE);
        }
        if (!isWindowsAdministrator()) {
            throw new SecurityException("Abre PowerShell con Ejecutar como administrador y vuelve a intentarlo.");
        }

        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("La herramienta requiere una consola interactiva de PowerShell.");
        }

        Properties properties = readLocalConfiguration();
        String url = setting("DB_URL", properties, "jdbc:postgresql://localhost:5432/gametime");
        String username = setting("DB_USER", properties, "gametime_app");
        String databasePassword = setting("DB_PASSWORD", properties, "");
        if (databasePassword.isBlank()) {
            throw new IllegalStateException("Configura DB_PASSWORD en el archivo .env local antes de continuar.");
        }

        try (Connection connection = DriverManager.getConnection(url, username, databasePassword)) {
            Flyway.configure().dataSource(url, username, databasePassword).load().migrate();
            if (args[0].equals("setup")) {
                createAdmin(connection, console);
            } else {
                resetAdminPassword(connection, console);
            }
        }
    }

    private static void createAdmin(Connection connection, Console console) throws Exception {
        if (adminExists(connection)) {
            throw new IllegalStateException("La cuenta GameTime ya existe. Para cambiarla usa reset-password.");
        }
        char[] password = readAndConfirmPassword(console);
        try {
            String hash = passwordEncoder().encode(new String(password));
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO admin_account (singleton_id, username, password_hash) VALUES (1, 'admin', ?)")) {
                statement.setString(1, hash);
                statement.executeUpdate();
            }
            console.printf("Cuenta GameTime creada. Ya puedes iniciar el backend y entrar en http://localhost:8080.%n");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static void resetAdminPassword(Connection connection, Console console) throws Exception {
        if (!adminExists(connection)) {
            throw new IllegalStateException("No existe una cuenta. Ejecuta primero setup.");
        }
        char[] password = readAndConfirmPassword(console);
        try {
            String hash = passwordEncoder().encode(new String(password));
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE admin_account SET password_hash = ?, password_changed_at = CURRENT_TIMESTAMP WHERE singleton_id = 1")) {
                statement.setString(1, hash);
                statement.executeUpdate();
            }
            console.printf("Contraseña restablecida. Reinicia el backend para invalidar las sesiones activas.%n");
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static char[] readAndConfirmPassword(Console console) throws Exception {
        char[] password = console.readPassword("Nueva contraseña (15 a 256 caracteres): ");
        if (password == null) {
            throw new IllegalStateException("No se recibió una contraseña.");
        }
        int length = Character.codePointCount(password, 0, password.length);
        if (length < MIN_PASSWORD_LENGTH || length > MAX_PASSWORD_LENGTH) {
            Arrays.fill(password, '\0');
            throw new IllegalArgumentException("La contraseña debe tener entre 15 y 256 caracteres.");
        }

        char[] confirmation = console.readPassword("Confirma la contraseña: ");
        if (confirmation == null || !MessageDigest.isEqual(
                new String(password).getBytes(StandardCharsets.UTF_8),
                new String(confirmation).getBytes(StandardCharsets.UTF_8))) {
            Arrays.fill(password, '\0');
            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        Arrays.fill(confirmation, '\0');
        return password;
    }

    private static PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }

    private static boolean adminExists(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM admin_account WHERE singleton_id = 1");
             ResultSet result = statement.executeQuery()) {
            return result.next();
        }
    }

    private static Properties readLocalConfiguration() throws IOException {
        Path envFile = Path.of(".env");
        if (!Files.exists(envFile)) {
            envFile = Path.of("..", ".env");
        }
        if (!Files.exists(envFile)) {
            throw new IllegalStateException("No se encontró .env. Ejecuta la herramienta desde el repositorio GameTime.");
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(envFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static String setting(String name, Properties properties, String fallback) {
        String environmentValue = System.getenv(name);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        return properties.getProperty(name, fallback).trim();
    }

    private static boolean isWindowsAdministrator() throws IOException, InterruptedException {
        if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) {
            return false;
        }

        String command = "$p = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent()); "
                + "if ($p.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) { exit 0 } else { exit 1 }";
        Process process = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", command)
                .redirectErrorStream(true)
                .start();
        process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
        return process.waitFor() == 0;
    }
}
