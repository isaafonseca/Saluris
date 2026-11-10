package br.com.saluris.tenant;

/** Entidade que pertence a uma empresa (tenant). */
public interface TenantScoped {

    Long getEmpresaId();

    void setEmpresaId(Long empresaId);
}
