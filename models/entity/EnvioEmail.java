package br.com.saluris.domain.entity;

import java.time.OffsetDateTime;

import br.com.saluris.domain.enums.StatusEnvioEmail;
import jakarta.persistence.Column;
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
 * Fila de envio de e-mails (Quadro 16 / RNF10).
 * <p>Em caso de falha no SMTP, registra o erro e reagenda para a próxima execução da rotina.
 */
@Entity
@Table(name = "envios_email")
@Getter
@Setter
@NoArgsConstructor
public class EnvioEmail extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notificacao_id")
    private Notificacao notificacao;

    @Column(name = "destinatario", nullable = false, length = 255)
    private String destinatario;

    @Column(name = "assunto", nullable = false, length = 250)
    private String assunto;

    @Column(name = "corpo", nullable = false, columnDefinition = "text")
    private String corpo;

    @Column(name = "sensivel", nullable = false)
    private boolean sensivel = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private StatusEnvioEmail status = StatusEnvioEmail.PENDENTE;

    @Column(name = "tentativas", nullable = false)
    private Integer tentativas = 0;

    @Column(name = "max_tentativas", nullable = false)
    private Integer maxTentativas = 5;

    @Column(name = "proxima_tentativa", nullable = false)
    private OffsetDateTime proximaTentativa = OffsetDateTime.now();

    @Column(name = "ultimo_erro", length = 1000)
    private String ultimoErro;

    @Column(name = "enviado_em")
    private OffsetDateTime enviadoEm;

    public void registrarEnvio() {
        this.status = StatusEnvioEmail.ENVIADO;
        this.enviadoEm = OffsetDateTime.now();
        this.ultimoErro = null;
        if (this.sensivel) {
            this.corpo = "[conteúdo sensível removido após envio]";
        }
    }

    public void registrarFalha(String erro, OffsetDateTime proximaTentativa) {
        this.tentativas = tentativas + 1;
        this.ultimoErro = erro == null ? null : erro.substring(0, Math.min(erro.length(), 1000));
        if (tentativas >= maxTentativas) {
            this.status = StatusEnvioEmail.FALHA_DEFINITIVA;
        } else {
            this.proximaTentativa = proximaTentativa;
        }
    }
}
