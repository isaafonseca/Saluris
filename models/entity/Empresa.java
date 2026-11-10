package br.com.saluris.domain.entity;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import br.com.saluris.domain.enums.PorteEmpresa;
import br.com.saluris.tenant.TenantFilter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Empresa cliente (tenant). Esta classe também declara o {@code @FilterDef} do filtro de tenant, usado
 * por todas as entidades com {@code empresa_id}. Para a própria tabela, o filtro restringe por {@code id}.
 */
@Entity
@Table(name = "empresas")
@FilterDef(name = TenantFilter.NAME, parameters = @ParamDef(name = TenantFilter.PARAM, type = Long.class))
@Filter(name = TenantFilter.NAME, condition = TenantFilter.CONDITION_EMPRESA)
@Getter
@Setter
@NoArgsConstructor
public class Empresa extends AuditableEntity {

    /** Somente dígitos (14). */
    @Column(name = "cnpj", nullable = false, length = 14)
    private String cnpj;

    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 200)
    private String nomeFantasia;

    @Column(name = "setor_atuacao", nullable = false, length = 150)
    private String setorAtuacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "porte", nullable = false, length = 10)
    private PorteEmpresa porte;

    @Column(name = "contato_nome", length = 150)
    private String contatoNome;

    @Column(name = "email_contato", nullable = false, length = 255)
    private String emailContato;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "ativa", nullable = false)
    private boolean ativa = true;
}
