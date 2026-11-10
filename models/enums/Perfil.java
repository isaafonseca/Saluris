package br.com.saluris.domain.enums;

/**
 * Perfis de acesso (RBAC). {@link #ADMINISTRADOR} é o T.I. da plataforma e é o único sem empresa;
 * os demais pertencem a uma empresa e só enxergam os dados dela.
 */
public enum Perfil {
    ADMINISTRADOR("Administrador"),
    ESPECIALISTA_SST("Especialista SST"),
    MEDICO_TRABALHO("Médico do Trabalho"),
    RH_EMPRESA("RH / Empresa");

    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Nome da authority no Spring Security (ex.: {@code ROLE_RH_EMPRESA}). */
    public String role() {
        return "ROLE_" + name();
    }

    public boolean pertenceAEmpresa() {
        return this != ADMINISTRADOR;
    }
}
