package com.gametime.backend.common;

/**
 * Regla de negocio incumplida. El mensaje está pensado para mostrarse al usuario.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
