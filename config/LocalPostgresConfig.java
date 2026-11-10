package br.com.saluris.config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.annotation.PreDestroy;

/**
 * Conecta prioritariamente ao banco PostgreSQL instalado na máquina (ex.: PostgreSQL 18 / banco Saluris na porta 5432).
 * Caso não esteja acessível, faz fallback automático para o PostgreSQL embarcado na porta 54329.
 */
@Configuration
@Profile("local")
public class LocalPostgresConfig {

    private static final Logger log = LoggerFactory.getLogger(LocalPostgresConfig.class);
    private static final int EMBEDDED_PORT = 54329;

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/Saluris}")
    private String configuredUrl;

    @Value("${spring.datasource.username:postgres}")
    private String configuredUsername;

    @Value("${spring.datasource.password:postgres}")
    private String configuredPassword;

    private EmbeddedPostgres embeddedPostgres;

    @Bean
    @Primary
    public DataSource dataSource() throws IOException {
        // 1. Tenta conectar primeiro ao PostgreSQL instalado na máquina (porta 5432 - banco Saluris)
        for (String candidateUrl : new String[] {
                configuredUrl,
                "jdbc:postgresql://localhost:5432/Saluris",
                "jdbc:postgresql://localhost:5432/saluris"
        }) {
            if (canConnect(candidateUrl, configuredUsername, configuredPassword)) {
                log.info("Conectado com sucesso ao PostgreSQL local instalado: {}", candidateUrl);
                return DataSourceBuilder.create()
                        .driverClassName("org.postgresql.Driver")
                        .url(candidateUrl)
                        .username(configuredUsername)
                        .password(configuredPassword)
                        .build();
            }
        }

        log.warn("Não foi possível conectar ao PostgreSQL da porta 5432 com o usuário '{}' (verifique a senha em config/application.yml). Usando PostgreSQL embarcado na porta {}.",
                configuredUsername, EMBEDDED_PORT);

        Path dataDir = Path.of(System.getProperty("user.home"), ".saluris", "pgdata");
        Files.createDirectories(dataDir);

        if (isPostgresRunning(EMBEDDED_PORT)) {
            log.info("PostgreSQL embarcado já está em execução na porta {} (dados em {}).",
                    EMBEDDED_PORT, dataDir);
        } else {
            Path pidFile = dataDir.resolve("postmaster.pid");
            if (Files.exists(pidFile)) {
                Files.deleteIfExists(pidFile);
            }
            this.embeddedPostgres = EmbeddedPostgres.builder()
                    .setDataDirectory(dataDir.toFile())
                    .setCleanDataDirectory(false)
                    .setPort(EMBEDDED_PORT)
                    .start();
        }

        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url("jdbc:postgresql://127.0.0.1:" + EMBEDDED_PORT + "/postgres")
                .username("postgres")
                .password("postgres")
                .build();
    }

    private boolean canConnect(String url, String user, String pass) {
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean isPostgresRunning(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @PreDestroy
    public void stop() throws IOException {
        if (embeddedPostgres != null) {
            embeddedPostgres.close();
        }
    }
}
