package com.gametime.backend.games;

public record Game(
        long id,
        String name,
        Launcher launcher,
        String launchTarget,
        String launchArguments,
        String processName,
        String category,
        String description,
        boolean enabled) {
}
