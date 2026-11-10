package br.com.saluris.domain.enums;

public enum StatusLaudo {
    AGUARDANDO_ASSINATURA("Aguardando Assinatura"),
    HOMOLOGADO("Homologado"),
    REVISADO_PENDENTE("Revisado Pendente");

    private final String rotulo;

    StatusLaudo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
