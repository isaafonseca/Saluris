package br.com.saluris.tenant;

import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;

import br.com.saluris.security.UsuarioPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * Isolamento de leitura por empresa (multitenancy lógico).
 * <p>Antes de qualquer chamada a um repositório Spring Data, habilita o filtro Hibernate
 * {@code tenantFilter} com o {@code empresaId} do usuário logado. Todas as consultas JPQL/derivadas
 * passam a receber {@code AND empresa_id = :empresaId} automaticamente.
 *
 * <ul>
 *   <li>ADMINISTRADOR e processos sem usuário (CRON, login) não recebem filtro.</li>
 *   <li><b>Fail-closed</b>: se houver usuário de empresa mas nenhum EntityManager vinculado à thread
 *       (sem transação e sem OSIV), a chamada é abortada em vez de rodar sem filtro.</li>
 *   <li>Limitação do Hibernate: o filtro <u>não</u> se aplica a {@code findById}/{@code getReferenceById};
 *       para buscas por id use {@code findByIdAndEmpresaId} (presente nos repositórios de tenant).</li>
 * </ul>
 */
@Aspect
@Component
public class TenantFilterAspect {

    private final EntityManagerFactory entityManagerFactory;

    public TenantFilterAspect(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Before("execution(* org.springframework.data.repository.Repository+.*(..))")
    public void habilitarFiltroDeTenant() {
        UsuarioPrincipal usuario = TenantContext.usuarioAtual().orElse(null);
        if (usuario == null || usuario.isAdministrador()) {
            return;
        }
        EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
        if (em == null) {
            throw new IllegalStateException(
                    "Acesso a repositório sem EntityManager vinculado: o filtro de empresa não pode ser aplicado.");
        }
        em.unwrap(Session.class)
                .enableFilter(TenantFilter.NAME)
                .setParameter(TenantFilter.PARAM, usuario.getEmpresaId());
    }
}
