package br.com.saluris.domain.entity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.com.saluris.domain.enums.ClassificacaoResposta;
import br.com.saluris.domain.enums.PapelMensagem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mensagem de uma conversa. O isolamento é herdado de {@link IaConversa} (sempre acessada via conversa). */
@Entity
@Table(name = "ia_mensagens")
@Getter
@Setter
@NoArgsConstructor
public class IaMensagem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversa_id", nullable = false)
    private IaConversa conversa;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 15)
    private PapelMensagem papel;

    @Column(name = "conteudo", nullable = false, columnDefinition = "text")
    private String conteudo;

    /** Somente para mensagens do ASSISTENTE. */
    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao", length = 30)
    private ClassificacaoResposta classificacao;

    /** Fontes normativas citadas: [{trecho_id, norma, item}]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fontes")
    private List<Map<String, Object>> fontes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    protected void aoCriar() {
        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
    }
}
