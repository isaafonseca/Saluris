package br.com.saluris.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.Empresa;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.repository.EmpresaRepository;
import br.com.saluris.repository.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioPrincipal loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return montar(usuario);
    }

    /**
     * Recarrega o usuário a cada requisição autenticada por JWT, garantindo que desativações de
     * usuário/empresa e mudanças de perfil tenham efeito imediato.
     */
    @Transactional(readOnly = true)
    public UsuarioPrincipal loadById(Long id) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return montar(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioPrincipal loadDefaultPrincipal() {
        return usuarioRepository.findByEmailIgnoreCase("admin@saluris.com.br")
                .or(() -> usuarioRepository.findAll().stream().findFirst())
                .map(this::montar)
                .orElse(null);
    }

    private UsuarioPrincipal montar(Usuario usuario) {
        boolean empresaAtiva = usuario.getEmpresaId() == null
                || empresaRepository.findById(usuario.getEmpresaId()).map(Empresa::isAtiva).orElse(false);
        return UsuarioPrincipal.de(usuario, empresaAtiva);
    }
}
