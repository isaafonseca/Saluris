package br.com.saluris.domain.enums;

public enum PorteEmpresa {
    MEI("Microempreendedor Individual"),
    ME("Microempresa"),
    EPP("Empresa de Pequeno Porte"),
    MEDIO("Médio porte"),
    GRANDE("Grande porte");

    private final String rotulo;

    PorteEmpresa(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
