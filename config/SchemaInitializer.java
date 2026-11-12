package br.com.saluris.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Inicializa o schema e os seeds do PostgreSQL automaticamente caso as tabelas ainda não existam.
 * <p>Executa antes do {@link AdminBootstrapRunner} ({@code @Order(1)}).
 */
@Component
@Order(1)
public class SchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private final DataSource dataSource;
    private final SalurisProperties props;

    public SchemaInitializer(DataSource dataSource, SalurisProperties props) {
        this.dataSource = dataSource;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (props.db() != null && !props.db().autoInit()) {
            return;
        }
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("postgresql")) {
                return;
            }
            if (!tabelaExiste(conn, "empresas")) {
                log.info("Aplicando scripts de criação do banco (01_schema.sql, 02_seed.sql, 03_nr_seed.sql)...");
                executarScript(conn, "db/01_schema.sql");
                executarScript(conn, "db/02_seed.sql");
                executarScript(conn, "db/03_nr_seed.sql");
                log.info("Banco de dados inicializado com sucesso.");
            } else {
                garantirConstraintPerfilUsuarios(conn);
                if (contarRegistros(conn, "nr_trechos") == 0) {
                    log.info("Populando base de conhecimento de NRs (03_nr_seed.sql)...");
                    executarScript(conn, "db/03_nr_seed.sql");
                }
            }
        }
    }

    private void garantirConstraintPerfilUsuarios(Connection conn) throws SQLException {
        if (!tabelaExiste(conn, "usuarios")) {
            return;
        }
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS ck_usuarios_perfil;
                    ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_perfil CHECK (
                        perfil IN ('ADMINISTRADOR', 'ADMIN_EMPRESA', 'ESPECIALISTA_SST', 'MEDICO_TRABALHO', 'RH_EMPRESA')
                    );
                    """);
        }
    }

    private boolean tabelaExiste(Connection conn, String tabela) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, "public", tabela, new String[] { "TABLE" })) {
            return rs.next();
        }
    }

    private long contarRegistros(Connection conn, String tabela) throws SQLException {
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private void executarScript(Connection conn, String classpathPath) throws IOException, SQLException {
        ClassPathResource res = new ClassPathResource(classpathPath);
        if (!res.exists()) {
            log.warn("Script {} não encontrado no classpath.", classpathPath);
            return;
        }
        String sql = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }
}
