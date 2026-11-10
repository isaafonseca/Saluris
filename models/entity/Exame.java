package br.com.saluris.domain.entity;

import java.time.LocalDate;

import org.hibernate.annotations.Filter;

import br.com.saluris.domain.enums.StatusFluxoExame;
import br.com.saluris.domain.enums.StatusValidade;
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
 * Exame ocupacional de um colaborador. {@code periodicidadeMeses} é um snapshot da matriz no momento da
 * geração. Fluxo: ... AGUARDANDO_ASSINATURA -> HOMOLOGADO | REVISADO_PENDENTE.
 */
@Entity
@Table(name = "exames")
@Filter(name = TenantFilter.NAME, condition = TenantFilter.CONDITION)
@Getter
@Setter
@NoArgsConstructor
public class Exame extends TenantAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_exame_id", nullable = false)
    private TipoExame tipoExame;

    /** Regra de origem; pode ficar nula se a configuração for removida. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matriz_exame_id")
    private MatrizExame matrizExame;

    @Column(name = "periodicidade_meses", nullable = false)
    private Integer periodicidadeMeses;

    // --- agendamento ---
    @Column(name = "data_agendada")
    private LocalDate dataAgendada;

    @Column(name = "local_clinica", length = 200)
    private String localClinica;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendado_por")
    private Usuario agendadoPor;

    // --- realização / validade ---
    @Column(name = "data_realizacao")
    private LocalDate dataRealizacao;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_validade", nullable = false, length = 25)
    private StatusValidade statusValidade = StatusValidade.EM_DIA;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_fluxo", nullable = false, length = 25)
    private StatusFluxoExame statusFluxo = StatusFluxoExame.PENDENTE;

    /** Resultado em PDF (nulo enquanto não houver arquivo). */
    @Embedded
    private ArquivoPdf arquivo;

    /** Assinatura/recusa do médico (nula enquanto não houver decisão). */
    @Embedded
    private AssinaturaMedica assinatura;

    // --- comportamento de domínio ---

    public void agendar(LocalDate data, String localClinica, String observacoes, Usuario agendadoPor) {
        if (data == null || !data.isAfter(LocalDate.now())) {
            throw new RegraNegocioException("A data de agendamento deve ser futura.");
        }
        if (statusFluxo != StatusFluxoExame.PENDENTE && statusFluxo != StatusFluxoExame.AGENDADO) {
            throw new RegraNegocioException("Somente exames pendentes ou agendados podem ser (re)agendados.");
        }
        this.dataAgendada = data;
        this.localClinica = localClinica;
        this.observacoes = observacoes;
        this.agendadoPor = agendadoPor;
        this.statusFluxo = StatusFluxoExame.AGENDADO;
    }

    /** Envia o exame realizado (com PDF) para a fila de assinatura do médico. */
    public void enviarParaAssinatura() {
        if (dataRealizacao == null) {
            throw new RegraNegocioException("Informe a data de realização do exame.");
        }
        if (arquivo == null || arquivo.getPath() == null) {
            throw new RegraNegocioException("Anexe o resultado do exame em PDF.");
        }
        this.assinatura = null;
        this.statusFluxo = StatusFluxoExame.AGUARDANDO_ASSINATURA;
    }

    public void homologar(Usuario medico) {
        exigirAguardandoAssinatura();
        this.assinatura = AssinaturaMedica.assinadaPor(medico, getEmpresaId());
        this.statusFluxo = StatusFluxoExame.HOMOLOGADO;
    }

    public void recusar(Usuario medico, String justificativa) {
        exigirAguardandoAssinatura();
        this.assinatura = AssinaturaMedica.recusadaPor(medico, getEmpresaId(), justificativa);
        this.statusFluxo = StatusFluxoExame.REVISADO_PENDENTE;
    }

    private void exigirAguardandoAssinatura() {
        if (statusFluxo != StatusFluxoExame.AGUARDANDO_ASSINATURA) {
            throw new RegraNegocioException("O exame não está aguardando assinatura.");
        }
    }
}
