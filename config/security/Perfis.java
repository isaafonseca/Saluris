package br.com.saluris.security;

/**
 * Expressões SpEL para {@code @PreAuthorize}, evitando strings soltas nos controllers.
 * Ex.: {@code @PreAuthorize(Perfis.ADMIN)}.
 */
public final class Perfis {

    public static final String ADMIN = "hasRole('ADMINISTRADOR')";
    public static final String ESPECIALISTA = "hasRole('ESPECIALISTA_SST')";
    public static final String MEDICO = "hasRole('MEDICO_TRABALHO')";
    public static final String RH = "hasRole('RH_EMPRESA')";

    /** Especialista SST ou RH (ex.: documentos SST, Agente IA). */
    public static final String ESPECIALISTA_OU_RH = "hasAnyRole('ESPECIALISTA_SST','RH_EMPRESA')";

    /** Dashboard e relatórios de conformidade (Quadros 09 e 10). */
    public static final String CONFORMIDADE = "hasAnyRole('ADMINISTRADOR','ESPECIALISTA_SST','RH_EMPRESA')";

    /** Qualquer usuário autenticado. */
    public static final String AUTENTICADO = "isAuthenticated()";

    private Perfis() {
    }
}
