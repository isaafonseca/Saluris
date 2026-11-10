package br.com.saluris.domain.entity;

import java.time.LocalDate;

import org.hibernate.annotations.Filter;

import br.com.saluris.domain.enums.StatusLaudo;
import br.com.saluris.exception.RegraNegocioException;
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

/**
 * Laudo técnico em PDF criado pelo Especialista SST. Nasce AGUARDANDO_ASSINATURA e só é válido após a
 * homologação do Médico do Trabalho (RN05).
 */
@Entity
@Table(name = "laudos")
@Filter(name = TenantFilter.NAME, condition = TenantFilter.CONDITION)
@Getter
@Setter
@NoArgsConstructor
public class Laudo extends TenantAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_laudo_id", nullable = false)
    private TipoLaudo tipoLaudo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colaborador_id")
    private Colaborador colaborador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exame_id")
    private Exame exame;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "descricao", length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private StatusLaudo status = StatusLaudo.AGUARDANDO_ASSINATURA;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao = LocalDate.now();

    @Column(name = "data_validade")
    private LocalDate dataValidade;

    @Embedded
    private ArquivoPdf arquivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por", nullable = false)
    private Usuario criadoPor;

    @Embedded
    private AssinaturaMedica assinatura;

    // --- comportamento de domínio ---

    public void homologar(Usuario medico) {
        exigirAguardandoAssinatura();
        this.assinatura = AssinaturaMedica.assinadaPor(medico, getEmpresaId());
        this.status = StatusLaudo.HOMOLOGADO;
    }

    public void recusar(Usuario medico, String justificativa) {
        exigirAguardandoAssinatura();
        this.assinatura = AssinaturaMedica.recusadaPor(medico, getEmpresaId(), justificativa);
        this.status = StatusLaudo.REVISADO_PENDENTE;
    }

    /** Após a revisão, o Especialista reenvia um novo PDF e o laudo volta para a fila do médico. */
    public void reenviar(ArquivoPdf novoArquivo) {
        if (status != StatusLaudo.REVISADO_PENDENTE) {
            throw new RegraNegocioException("Somente laudos em revisão podem ser reenviados.");
        }
        this.arquivo = novoArquivo;
        this.assinatura = null;
        this.status = StatusLaudo.AGUARDANDO_ASSINATURA;
    }

    private void exigirAguardandoAssinatura() {
        if (status != StatusLaudo.AGUARDANDO_ASSINATURA) {
            throw new RegraNegocioException("O laudo não está aguardando assinatura.");
        }
    }
}
