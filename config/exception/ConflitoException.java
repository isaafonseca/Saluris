package br.com.saluris.exception;

import java.util.Map;

/** Conflito de dados (HTTP 409) com propriedades adicionais opcionais (ex.: {@code existenteId}). */
public class ConflitoException extends RuntimeException {

    private final Map<String, Object> propriedades;

    public ConflitoException(String message) {
        this(message, Map.of());
    }

    public ConflitoException(String message, Map<String, Object> propriedades) {
        super(message);
        this.propriedades = propriedades == null ? Map.of() : Map.copyOf(propriedades);
    }

    public Map<String, Object> getPropriedades() {
        return propriedades;
    }
}
