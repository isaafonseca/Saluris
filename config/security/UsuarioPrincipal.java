package br.com.saluris.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Usuário autenticado. Carrega o {@code empresaId} (tenant) e o perfil usados em toda a
 * autorização e no isolamento de dados.
 */
@Getter
@AllArgsConstructor
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final Perfil perfil;
    /** {@code null} apenas para o ADMINISTRADOR (T.I. da plataforma). */
    private final Long empresaId;
    private final boolean ativo;
    private final boolean empresaAtiva;
    private final boolean deveTrocarSenha;

    public static UsuarioPrincipal de(Usuario u, boolean empresaAtiva) {
        return new UsuarioPrincipal(u.getId(), u.getNome(), u.getEmail(), u.getSenhaHash(), u.getPerfil(),
                u.getEmpresaId(), u.isAtivo(), empresaAtiva, u.isDeveTrocarSenha());
    }

    public boolean isAdministrador() {
        return perfil == Perfil.ADMINISTRADOR;
    }

    public boolean isAdminEmpresa() {
        return perfil == Perfil.ADMIN_EMPRESA;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(perfil.role()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    /** Usuários de uma empresa desativada ficam bloqueados. */
    @Override
    public boolean isAccountNonLocked() {
        return empresaAtiva;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
