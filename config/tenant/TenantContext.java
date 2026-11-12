package br.com.saluris.tenant;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.saluris.exception.RegraNegocioException;
import br.com.saluris.security.UsuarioPrincipal;

/** Acesso ao usuário/tenant da requisição atual e regras de resolução de empresa. */
public final class TenantContext {

    private TenantContext() {
    }

    public static Optional<UsuarioPrincipal> usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UsuarioPrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    public static UsuarioPrincipal exigirUsuario() {
        return usuarioAtual().orElseThrow(
                () -> new AuthenticationCredentialsNotFoundException("Usuário não autenticado"));
    }

    public static boolean isAdministrador() {
        return usuarioAtual().map(UsuarioPrincipal::isAdministrador).orElse(false);
    }

    public static boolean isAdminEmpresa() {
        return usuarioAtual().map(UsuarioPrincipal::isAdminEmpresa).orElse(false);
    }

    public static Long exigirEmpresaId() {
        UsuarioPrincipal usuario = exigirUsuario();
        if (usuario.isAdministrador() || usuario.getEmpresaId() == null) {
            throw new RegraNegocioException("Esta operação exige usuário vinculado a uma empresa.");
        }
        return usuario.getEmpresaId();
    }

    /**
     * Resolve a empresa em que a operação deve ocorrer.
     * <ul>
     *   <li>Usuário de empresa: sempre a própria; se {@code solicitada} for outra, nega o acesso.</li>
     *   <li>Administrador (sem empresa): exige que a empresa seja informada.</li>
     * </ul>
     */
    public static Long resolverEmpresaId(Long solicitada) {
        UsuarioPrincipal usuario = exigirUsuario();
        if (usuario.isAdministrador()) {
            if (solicitada == null) {
                throw new RegraNegocioException("Informe a empresa.");
            }
            return solicitada;
        }
        if (solicitada != null && !solicitada.equals(usuario.getEmpresaId())) {
            throw new AccessDeniedException("Acesso negado a dados de outra empresa.");
        }
        return usuario.getEmpresaId();
    }

    /**
     * Resolve a empresa em consultas de listagem/dashboard:
     * para o ADMINISTRADOR permite {@code null} (retornando os registros de todas as empresas);
     * para os demais perfis retorna sempre a empresa do próprio usuário.
     */
    public static Long resolverEmpresaIdOpcional(Long solicitada) {
        UsuarioPrincipal usuario = exigirUsuario();
        if (usuario.isAdministrador()) {
            return solicitada;
        }
        if (solicitada != null && !solicitada.equals(usuario.getEmpresaId())) {
            throw new AccessDeniedException("Acesso negado a dados de outra empresa.");
        }
        return usuario.getEmpresaId();
    }

    /** Garante que um registro já carregado pertence à empresa do usuário (Administrador passa). */
    public static void exigirMesmaEmpresa(Long empresaIdDoRegistro) {
        UsuarioPrincipal usuario = exigirUsuario();
        if (!usuario.isAdministrador() && !usuario.getEmpresaId().equals(empresaIdDoRegistro)) {
            throw new AccessDeniedException("Acesso negado a dados de outra empresa.");
        }
    }
}
