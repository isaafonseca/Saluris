package br.com.saluris.domain.enums;

/**
 * Tipo de notificação: alertas de vencimento ({@link #PREVENTIVO} e {@link #URGENCIA}, gerados pela rotina
 * diária) e notificações de evento dos fluxos de laudo, exame e colaborador.
 */
public enum TipoNotificacao {

    /** Alerta preventivo: 30, 15 ou 7 dias antes do vencimento (RN08 / RF30). */
    PREVENTIVO,

    /** Alerta de urgência: item vencido (RF30). */
    URGENCIA,

    LAUDO_PENDENTE_ASSINATURA,
    LAUDO_HOMOLOGADO,
    LAUDO_REVISADO_PENDENTE,
    EXAME_PENDENTE_ASSINATURA,
    EXAME_HOMOLOGADO,
    EXAME_REVISADO_PENDENTE,
    PERIODICIDADE_NAO_CONFIGURADA,
    EXAME_AGENDADO;

    public boolean isAlertaDeVencimento() {
        return this == PREVENTIVO || this == URGENCIA;
    }
}
