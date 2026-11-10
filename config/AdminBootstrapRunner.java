package br.com.saluris.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.repository.UsuarioRepository;

/**
 * Cria o primeiro ADMINISTRADOR (T.I.) a partir de {@code SALURIS_ADMIN_EMAIL} / {@code SALURIS_ADMIN_SENHA}
 * quando ainda não existe nenhum. Se as variáveis não estiverem definidas, não faz nada.
 */
@Component
@Order(2)
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final int TAMANHO_MINIMO_SENHA = 12;

    private final SalurisProperties props;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(SalurisProperties props, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.props = props;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        var cfg = props.bootstrapAdmin();
        if (cfg.email() == null || cfg.email().isBlank() || cfg.senha() == null || cfg.senha().isBlank()) {
            return;
        }
        if (usuarioRepository.existsByPerfil(Perfil.ADMINISTRADOR)) {
            return;
        }
        if (cfg.senha().length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalStateException(
                    "SALURIS_ADMIN_SENHA deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        Usuario admin = new Usuario();
        admin.setNome(cfg.nome());
        admin.setEmail(cfg.email().trim().toLowerCase());
        admin.setSenhaHash(passwordEncoder.encode(cfg.senha()));
        admin.setPerfil(Perfil.ADMINISTRADOR);
        admin.setDeveTrocarSenha(false);
        usuarioRepository.save(admin);
        log.info("Administrador inicial criado: {}", admin.getEmail());
    }
}
