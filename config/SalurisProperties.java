package br.com.saluris.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Propriedades da aplicação (prefixo {@code saluris}).
 */
@ConfigurationProperties(prefix = "saluris")
public record SalurisProperties(
        @DefaultValue Security security,
        @DefaultValue BootstrapAdmin bootstrapAdmin,
        @DefaultValue Db db,
        @DefaultValue Storage storage,
        @DefaultValue Ia ia) {

    public static SalurisProperties comSeguranca(Security security, BootstrapAdmin bootstrapAdmin) {
        return new SalurisProperties(security, bootstrapAdmin, null, null, null);
    }

    public record Security(
            @DefaultValue Jwt jwt,
            @DefaultValue("http://localhost:5173") List<String> corsOrigens) {
    }

    public record Jwt(
            String secret,
            @DefaultValue("480") long expiracaoMinutos) {
    }

    public record BootstrapAdmin(
            @DefaultValue("Administrador") String nome,
            String email,
            String senha) {
    }

    public record Db(
            @DefaultValue("true") boolean autoInit) {
    }

    public record Storage(
            String dir) {
    }

    public record Ia(
            String apiKey,
            @DefaultValue("gpt-4o-mini") String modelo,
            @DefaultValue("https://api.openai.com/v1") String baseUrl) {
    }
}
