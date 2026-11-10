package br.com.saluris.domain.entity;

import org.hibernate.annotations.Filter;

import br.com.saluris.tenant.TenantFilter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Matriz de exames: periodicidade (6 a 24 meses) por combinação FUNÇÃO + SETOR + TIPO DE EXAME (RN01).
 */
@Entity
@Table(name = "matriz_exames")
@Filter(name = TenantFilter.NAME, condition = TenantFilter.CONDITION)
@Getter
@Setter
@NoArgsConstructor
public class MatrizExame extends TenantAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcao_id", nullable = false)
    private Funcao funcao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "setor_id", nullable = false)
    private Setor setor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_exame_id", nullable = false)
    private TipoExame tipoExame;

    @Column(name = "periodicidade_meses", nullable = false)
    private Integer periodicidadeMeses;

    @Column(name = "obrigatorio", nullable = false)
    private boolean obrigatorio = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "configurado_por", nullable = false)
    private Usuario configuradoPor;
}
