package br.com.saluris.domain.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.Filter;

import br.com.saluris.domain.enums.TipoNotificacao;
import br.com.saluris.tenant.TenantEntityListener;
import br.com.saluris.tenant.TenantFilter;
import br.com.saluris.tenant.TenantScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

/**
 * Notificação in-app. Exatamente um alvo deve ser informado (exame, documento, laudo ou colaborador).
 * Alertas de vencimento ({@code PREVENTIVO}: 30/15/7 dias; {@code URGENCIA}: 0) são únicos por
 * usuário, item, marco e dia.
 */
@Entity
@Table(name = "notificacoes")
@Filter(name = TenantFilter.NAME, condition = TenantFilter.CONDITION)
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Notificacao extends BaseEntity implements TenantScoped {

    @Column(name = "empresa_id", updatable = false)
    private Long empresaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exame_id")
    private Exame exame;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id")
    private Documento documento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laudo_id")
    private Laudo laudo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colaborador_id")
    private Colaborador colaborador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoNotificacao tipo;

    /** 30, 15, 7 (PREVENTIVO) ou 0 (URGENCIA); nulo nos demais tipos. */
    @Column(name = "marco_dias")
    private Integer marcoDias;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia = LocalDate.now();

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "mensagem", nullable = false, length = 1000)
    private String mensagem;

    @Column(name = "lida", nullable = false)
    private boolean lida = false;

    @Column(name = "lida_em")
    private OffsetDateTime lidaEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    protected void aoCriar() {
        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
    }

    public void marcarComoLida() {
        if (!lida) {
            lida = true;
            lidaEm = OffsetDateTime.now();
        }
    }
}
