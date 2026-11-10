package br.com.saluris.domain.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Filter;

import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.tenant.TenantFilter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuário da plataforma. {@code empresaId} é {@code null} apenas para o ADMINISTRADOR (T.I.);
 * Especialista SST, Médico do Trabalho e RH pertencem sempre a uma empresa.
 */
@Entity
@Table(name = "usuarios")
@Filter(name = TenantFilter.NAME, condition = TenantFilter.CONDITION)
@Getter
@Setter
@NoArgsConstructor
public class Usuario extends TenantAuditableEntity {

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** Hash BCrypt. Nunca expor em DTOs. */
    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(name = "deve_trocar_senha", nullable = false)
    private boolean deveTrocarSenha = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 20)
    private Perfil perfil;

    @Column(name = "crm", length = 20)
    private String crm;

    @Column(name = "crm_uf", length = 2)
    private String crmUf;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "ultimo_acesso")
    private OffsetDateTime ultimoAcesso;
}
