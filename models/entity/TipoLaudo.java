package br.com.saluris.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Catálogo global (Administrador). */
@Entity
@Table(name = "tipos_laudo")
@Getter
@Setter
@NoArgsConstructor
public class TipoLaudo extends AuditableEntity {

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
}
