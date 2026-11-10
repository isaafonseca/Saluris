package br.com.saluris.domain.entity;

import br.com.saluris.tenant.TenantEntityListener;
import br.com.saluris.tenant.TenantScoped;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base das entidades que pertencem a uma empresa (tenant). O {@code empresaId} é imutável após a criação
 * e validado em escrita por {@link TenantEntityListener}.
 */
@MappedSuperclass
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
public abstract class TenantAuditableEntity extends AuditableEntity implements TenantScoped {

    @Column(name = "empresa_id", updatable = false)
    private Long empresaId;
}
