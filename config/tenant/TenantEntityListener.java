package br.com.saluris.tenant;

import org.springframework.security.access.AccessDeniedException;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Proteção de escrita: usuários de empresa só podem gravar registros da própria empresa.
 * Se o {@code empresaId} não vier preenchido, é herdado do usuário; se vier diferente, a gravação é negada.
 * Sem usuário autenticado (CRON, bootstrap, login) nada é validado.
 */
public class TenantEntityListener {

    @PrePersist
    @PreUpdate
    public void validar(Object entidade) {
        if (!(entidade instanceof TenantScoped escopo)) {
            return;
        }
        TenantContext.usuarioAtual()
                .filter(u -> !u.isAdministrador())
                .ifPresent(u -> {
                    if (escopo.getEmpresaId() == null) {
                        escopo.setEmpresaId(u.getEmpresaId());
                    } else if (!escopo.getEmpresaId().equals(u.getEmpresaId())) {
                        throw new AccessDeniedException("Acesso negado a dados de outra empresa.");
                    }
                });
    }
}
