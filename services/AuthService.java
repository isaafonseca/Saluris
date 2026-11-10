package br.com.saluris.service;

import java.time.OffsetDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.auth.dto.LoginRequest;
import br.com.saluris.auth.dto.LoginResponse;
import br.com.saluris.auth.dto.TrocarSenhaRequest;
import br.com.saluris.auth.dto.UsuarioLogadoResponse;
import br.com.saluris.domain.entity.Empresa;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.exception.RegraNegocioException;
import br.com.saluris.repository.EmpresaRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.security.JwtService;
import br.com.saluris.security.UsuarioPrincipal;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
            UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.senha()));
        UsuarioPrincipal principal = (UsuarioPrincipal) auth.getPrincipal();

        Usuario usuario = buscar(principal.getId());
        usuario.setUltimoAcesso(OffsetDateTime.now());

        JwtService.TokenEmitido emitido = jwtService.gerarToken(principal);
        return new LoginResponse(emitido.token(), "Bearer", emitido.expiraEm(), paraResposta(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioLogadoResponse usuarioLogado(UsuarioPrincipal principal) {
        return paraResposta(buscar(principal.getId()));
    }

    @Transactional
    public void trocarSenha(UsuarioPrincipal principal, TrocarSenhaRequest request) {
        Usuario usuario = buscar(principal.getId());
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual está incorreta.");
        }
        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw new RegraNegocioException("A nova senha deve ser diferente da atual.");
        }
        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuario.setDeveTrocarSenha(false);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private UsuarioLogadoResponse paraResposta(Usuario u) {
        String empresaNome = u.getEmpresaId() == null ? null
                : empresaRepository.findById(u.getEmpresaId())
                        .map(e -> e.getNomeFantasia() != null && !e.getNomeFantasia().isBlank()
                                ? e.getNomeFantasia() : e.getRazaoSocial())
                        .orElse(null);
        return new UsuarioLogadoResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.getEmpresaId(),
                empresaNome, u.isDeveTrocarSenha(), u.getCrm(), u.getCrmUf());
    }
}
