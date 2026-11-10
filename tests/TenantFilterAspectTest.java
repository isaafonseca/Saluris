package br.com.saluris.tenant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.security.UsuarioPrincipal;
import jakarta.persistence.EntityManagerFactory;

/** Comportamento do aspecto fora de transação/OSIV (sem EntityManager vinculado à thread). */
class TenantFilterAspectTest {

    private final EntityManagerFactory emf = mock(EntityManagerFactory.class);
    private final TenantFilterAspect aspect = new TenantFilterAspect(emf);

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticarComo(Perfil perfil, Long empresaId) {
        UsuarioPrincipal p = new UsuarioPrincipal(1L, "Teste", "t@t.com", "hash", perfil, empresaId, true, true, false);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities()));
    }

    @Test
    void usuarioDeEmpresaSemEntityManagerVinculadoFalhaDeFormaSegura() {
        autenticarComo(Perfil.RH_EMPRESA, 10L);

        assertThatThrownBy(aspect::habilitarFiltroDeTenant)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EntityManager");
    }

    @Test
    void administradorNaoRecebeFiltro() {
        autenticarComo(Perfil.ADMINISTRADOR, null);

        assertThatCode(aspect::habilitarFiltroDeTenant).doesNotThrowAnyException();
        verifyNoInteractions(emf);
    }

    @Test
    void processoSemUsuarioNaoRecebeFiltro() {
        assertThatCode(aspect::habilitarFiltroDeTenant).doesNotThrowAnyException();
        verifyNoInteractions(emf);
    }
}
