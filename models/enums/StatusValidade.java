package br.com.saluris.domain.enums;

/** Situação de prazo de exames, documentos e laudos (calculada pelo CRON diário). */
public enum StatusValidade {
    EM_DIA("Em dia"),
    PROXIMO_VENCIMENTO("Próximo do Vencimento"),
    VENCIDO("Vencido");

    private final String rotulo;

    StatusValidade(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
