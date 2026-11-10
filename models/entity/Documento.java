package br.com.saluris.domain.entity;

import java.time.LocalDate;

import org.hibernate.annotations.Filter;

import br.com.saluris.domain.enums.StatusValidade;
import br.com.saluris.tenant.TenantFilter;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Documento SST (PGR, PCMSO, LTCAT...) em PDF, com controle de validade. */
@Entity
@Table(name = "documentos")
@Filter(name = TenantFilter.NAME, condition = TenantFilter.CONDITION)
@Getter
@Setter
@NoArgsConstructor
public class Documento extends TenantAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    /** PCMSO elaborado a partir do PGR (RN04). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_base_id")
    private Documento documentoBase;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "versao", nullable = false)
    private Integer versao = 1;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_validade", nullable = false, length = 25)
    private StatusValidade statusValidade = StatusValidade.EM_DIA;

    @Embedded
    private ArquivoPdf arquivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enviado_por", nullable = false)
    private Usuario enviadoPor;
}
