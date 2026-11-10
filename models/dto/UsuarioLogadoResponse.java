package br.com.saluris.auth.dto;

import br.com.saluris.domain.enums.Perfil;

/** Dados do usuário logado (sem informações sensíveis). */
public record UsuarioLogadoResponse(
        Long id,
        String nome,
        String email,
        Perfil perfil,
        Long empresaId,
        String empresaNome,
        boolean deveTrocarSenha,
        String crm,
        String crmUf) {
}
