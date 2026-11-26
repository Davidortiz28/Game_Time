package com.gametime.client;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuración del cliente: dirección del backend local y token del cliente.
 * Se leen de variables de entorno o del archivo .env del repositorio.
 */
record ClientConfig(String serverUrl, String token) {

    static ClientConfig load() {
        Properties env = readEnvFile();
        String url = setting("GAMETIME_SERVER_URL", env, "http://127.0.0.1:8080");
        String token = setting("CLIENT_API_TOKEN", env, "");
        if (token.isBlank()) {
            throw new IllegalStateException(
                    "Falta CLIENT_API_TOKEN. Configúralo en el archivo .env (el mismo que usa el backend).");
        }
        return new ClientConfig(url.replaceAll("/+$", ""), token);
    }

    private static String setting(String name, Properties env, String fallback) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return env.getProperty(name, fallback).trim();
    }

    private static Properties readEnvFile() {
        Properties properties = new Properties();
        for (Path candidate : new Path[] {Path.of(".env"), Path.of("..", ".env")}) {
            if (Files.isRegularFile(candidate)) {
                try (Reader reader = Files.newBufferedReader(candidate, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                    break;
                } catch (IOException exception) {
                    throw new IllegalStateException("No se pudo leer " + candidate.toAbsolutePath(), exception);
                }
            }
        }
        return properties;
    }
}
