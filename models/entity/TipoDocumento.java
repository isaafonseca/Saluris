package br.com.saluris.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Catálogo global (Administrador): PGR, PCMSO, LTCAT... */
@Entity
@Table(name = "tipos_documento")
@Getter
@Setter
@NoArgsConstructor
public class TipoDocumento extends AuditableEntity {

    @Column(name = "sigla", nullable = false, length = 20)
    private String sigla;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    /** 12 (anual) ou 24 (bienal) - RN03. */
    @Column(name = "periodicidade_padrao_meses", nullable = false)
    private Integer periodicidadePadraoMeses;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
}
