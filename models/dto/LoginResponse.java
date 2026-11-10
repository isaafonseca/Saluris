package br.com.saluris.auth.dto;

import java.time.Instant;

public record LoginResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioLogadoResponse usuario) {
}
