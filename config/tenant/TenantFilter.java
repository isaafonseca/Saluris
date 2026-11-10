package br.com.saluris.tenant;

/** Definição do filtro Hibernate de multitenancy lógico. */
public final class TenantFilter {

    public static final String NAME = "tenantFilter";
    public static final String PARAM = "empresaId";
    /** Condição padrão para tabelas com a coluna {@code empresa_id}. */
    public static final String CONDITION = "empresa_id = :empresaId";
    /** Condição para a própria tabela {@code empresas}. */
    public static final String CONDITION_EMPRESA = "id = :empresaId";

    private TenantFilter() {
    }
}
