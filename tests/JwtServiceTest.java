package br.com.saluris.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.saluris.config.SalurisProperties;
import br.com.saluris.domain.enums.Perfil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[48]);
    private static final Instant AGORA = Instant.parse("2026-01-10T12:00:00Z");

    private static SalurisProperties props(String secret, long minutos) {
        return SalurisProperties.comSeguranca(
                new SalurisProperties.Security(new SalurisProperties.Jwt(secret, minutos), List.of()),
                new SalurisProperties.BootstrapAdmin("Admin", null, null));
    }

    private static JwtService servico(Instant instante) {
        return new JwtService(props(SECRET, 60), Clock.fixed(instante, ZoneOffset.UTC));
    }

    private static UsuarioPrincipal principal() {
        return new UsuarioPrincipal(42L, "Maria", "maria@empresa.com", "hash", Perfil.RH_EMPRESA, 7L,
                true, true, false);
    }

    @Test
    void geraEValidaToken() {
        JwtService jwt = servico(AGORA);
        var emitido = jwt.gerarToken(principal());

        assertThat(jwt.extrairUsuarioId(emitido.token())).isEqualTo(42L);
        assertThat(emitido.expiraEm()).isEqualTo(AGORA.plus(Duration.ofMinutes(60)));
    }

    @Test
    void rejeitaTokenExpirado() {
        String token = servico(AGORA).gerarToken(principal()).token();

        JwtService duasHorasDepois = servico(AGORA.plus(Duration.ofHours(2)));
        assertThatThrownBy(() -> duasHorasDepois.extrairUsuarioId(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejeitaTokenAdulterado() {
        JwtService jwt = servico(AGORA);
        String token = jwt.gerarToken(principal()).token();
        String adulterado = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwt.extrairUsuarioId(adulterado)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejeitaTokenAssinadoComOutraChave() {
        String outraChave = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
                14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32});
        String token = new JwtService(props(outraChave, 60), Clock.fixed(AGORA, ZoneOffset.UTC))
                .gerarToken(principal()).token();

        assertThatThrownBy(() -> servico(AGORA).extrairUsuarioId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void exigeSegredoForte() {
        Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
        assertThatThrownBy(() -> new JwtService(props("", 60), clock)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(props(null, 60), clock)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(props(Base64.getEncoder().encodeToString(new byte[16]), 60), clock))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 bytes");
        assertThatThrownBy(() -> new JwtService(props("isso não é base64!!", 60), clock))
                .isInstanceOf(IllegalStateException.class);
    }
}
