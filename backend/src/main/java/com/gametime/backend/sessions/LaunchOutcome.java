package com.gametime.backend.sessions;

public enum LaunchOutcome {
    EXITED("El jugador cerró el juego"),
    CLOSED_NORMALLY("Cerrado normalmente por GameTime"),
    IGNORED_CLOSE("No respondió al cierre normal; siguió abierto"),
    LAUNCH_FAILED("No se pudo iniciar");

    private final String label;

    LaunchOutcome(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
