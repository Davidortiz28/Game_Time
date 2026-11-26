package com.gametime.backend.games;

public enum Launcher {
    STEAM("Steam", "steam://"),
    EPIC("Epic Games", "com.epicgames.launcher://"),
    OTHER("Ejecutable", null);

    private final String label;
    private final String uriPrefix;

    Launcher(String label, String uriPrefix) {
        this.label = label;
        this.uriPrefix = uriPrefix;
    }

    public String getLabel() {
        return label;
    }

    public String getUriPrefix() {
        return uriPrefix;
    }
}
