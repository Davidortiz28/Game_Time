package com.gametime.backend.sessions;

public enum SessionStatus {
    CREATED("Creada, sin iniciar", true),
    ACTIVE("En curso", true),
    GRACE("Tiempo agotado · gracia de 30 s", true),
    COMPLETED("Terminada por tiempo", false),
    ADMIN_ENDED("Terminada por el administrador", false),
    INTERRUPTED("Interrumpida", false),
    CANCELLED("Cancelada antes de iniciar", false);

    private final String label;
    private final boolean open;

    SessionStatus(String label, boolean open) {
        this.label = label;
        this.open = open;
    }

    public String getLabel() {
        return label;
    }

    /** Creada, activa o en gracia: bloquea la creación de otra sesión. */
    public boolean isOpen() {
        return open;
    }
}
