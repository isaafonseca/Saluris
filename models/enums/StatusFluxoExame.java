package br.com.saluris.domain.enums;

public enum StatusFluxoExame {
    PENDENTE("Pendente"),
    AGENDADO("Agendado"),
    REALIZADO("Realizado"),
    AGUARDANDO_ASSINATURA("Aguardando Assinatura"),
    HOMOLOGADO("Homologado"),
    REVISADO_PENDENTE("Revisado Pendente");

    private final String rotulo;

    StatusFluxoExame(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
