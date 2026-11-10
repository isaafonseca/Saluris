package br.com.saluris.domain.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Trecho de uma Norma Regulamentadora usado como contexto pelo Agente IA. Base global (sem tenant).
 * A coluna gerada {@code busca} (tsvector) não é mapeada: a busca textual é feita por consulta nativa.
 */
@Entity
@Table(name = "nr_trechos")
@Getter
@Setter
@NoArgsConstructor
public class NrTrecho extends BaseEntity {

    /** Ex.: NR-07. */
    @Column(name = "norma", nullable = false, length = 10)
    private String norma;

    @Column(name = "titulo_norma", nullable = false, length = 200)
    private String tituloNorma;

    /** Ex.: 7.5.1. */
    @Column(name = "item", length = 30)
    private String item;

    @Column(name = "conteudo", nullable = false, columnDefinition = "text")
    private String conteudo;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    protected void aoCriar() {
        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
    }
}
