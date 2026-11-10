package br.com.saluris.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.exception.RegraNegocioException;
import br.com.saluris.security.UsuarioPrincipal;

class TenantContextTest {

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    static void autenticarComo(Perfil perfil, Long empresaId) {
        UsuarioPrincipal p = new UsuarioPrincipal(1L, "Teste", "t@t.com", "hash", perfil, empresaId, true, true, false);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities()));
    }

    @Test
    void usuarioDeEmpresaSempreUsaASuaEmpresa() {
        autenticarComo(Perfil.RH_EMPRESA, 10L);
        assertThat(TenantContext.resolverEmpresaId(null)).isEqualTo(10L);
        assertThat(TenantContext.resolverEmpresaId(10L)).isEqualTo(10L);
    }

    @Test
    void usuarioDeEmpresaNaoAcessaOutraEmpresa() {
        autenticarComo(Perfil.ESPECIALISTA_SST, 10L);
        assertThatThrownBy(() -> TenantContext.resolverEmpresaId(11L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> TenantContext.exigirMesmaEmpresa(11L)).isInstanceOf(AccessDeniedException.class);
        TenantContext.exigirMesmaEmpresa(10L); // não lança
    }

    @Test
    void administradorPrecisaInformarAEmpresa() {
        autenticarComo(Perfil.ADMINISTRADOR, null);
        assertThat(TenantContext.isAdministrador()).isTrue();
        assertThat(TenantContext.resolverEmpresaId(5L)).isEqualTo(5L);
        assertThatThrownBy(() -> TenantContext.resolverEmpresaId(null)).isInstanceOf(RegraNegocioException.class);
        TenantContext.exigirMesmaEmpresa(99L); // administrador passa
    }

    @Test
    void semUsuarioNaoResolve() {
        assertThat(TenantContext.usuarioAtual()).isEmpty();
        assertThat(TenantContext.isAdministrador()).isFalse();
        assertThatThrownBy(() -> TenantContext.resolverEmpresaId(1L))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
