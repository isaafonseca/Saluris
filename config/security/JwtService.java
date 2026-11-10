package br.com.saluris.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import br.com.saluris.config.SalurisProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/** Emissão e validação de tokens JWT (HMAC). O token só identifica o usuário; o resto é recarregado do banco. */
@Service
public class JwtService {

    public record TokenEmitido(String token, Instant expiraEm) {
    }

    private final SecretKey chave;
    private final Duration expiracao;
    private final Clock clock;

    public JwtService(SalurisProperties props, Clock clock) {
        String secret = props.security().jwt().secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Defina a variável de ambiente SALURIS_JWT_SECRET (Base64 de pelo menos 32 bytes aleatórios).");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret.trim());
        } catch (RuntimeException e) {
            throw new IllegalStateException("SALURIS_JWT_SECRET deve estar em Base64.", e);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("SALURIS_JWT_SECRET deve ter pelo menos 32 bytes (256 bits).");
        }
        long minutos = props.security().jwt().expiracaoMinutos();
        if (minutos <= 0) {
            throw new IllegalStateException("saluris.security.jwt.expiracao-minutos deve ser maior que zero.");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracao = Duration.ofMinutes(minutos);
        this.clock = clock;
    }

    public TokenEmitido gerarToken(UsuarioPrincipal principal) {
        Instant agora = clock.instant();
        Instant expiraEm = agora.plus(expiracao);
        String token = Jwts.builder()
                .subject(String.valueOf(principal.getId()))
                .claim("perfil", principal.getPerfil().name())
                .claim("empresaId", principal.getEmpresaId())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiraEm))
                .signWith(chave)
                .compact();
        return new TokenEmitido(token, expiraEm);
    }

    /**
     * @return id do usuário contido no token
     * @throws JwtException se o token for inválido, adulterado ou expirado
     */
    public Long extrairUsuarioId(String token) {
        String subject = Jwts.parser()
                .verifyWith(chave)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new JwtException("Subject inválido", e);
        }
    }
}
