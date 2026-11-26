package com.gametime.backend.sessions;

/** Datos que el cliente necesita para iniciar un juego autorizado. */
public record LaunchTicket(
        long launchId,
        long gameId,
        String gameName,
        String launcher,
        String launchTarget,
        String launchArguments,
        String processName) {
}
