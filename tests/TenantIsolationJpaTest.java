package br.com.saluris.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.saluris.domain.entity.Empresa;
import br.com.saluris.domain.entity.Funcao;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.domain.enums.PorteEmpresa;
import br.com.saluris.repository.FuncaoRepository;
import br.com.saluris.security.UsuarioPrincipal;

/**
 * Valida (em H2, gerando o schema a partir das entidades) que: o mapeamento JPA de todas as entidades é
 * consistente, as consultas derivadas dos repositórios são válidas e o isolamento por empresa funciona.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(TenantFilterAspect.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
class TenantIsolationJpaTest {

    @Autowired
    TestEntityManager tem;
    @Autowired
    FuncaoRepository funcaoRepository;

    Long empresaA;
    Long empresaB;
    Long funcaoDaB;

    @BeforeEach
    void dados() {
        empresaA = tem.persistAndGetId(empresa("11222333000181", "Empresa A"), Long.class);
        empresaB = tem.persistAndGetId(empresa("45997418000153", "Empresa B"), Long.class);
        tem.persist(funcao(empresaA, "Soldador"));
        funcaoDaB = tem.persistAndGetId(funcao(empresaB, "Soldador"), Long.class);
        tem.persist(funcao(empresaB, "Pintor"));
        tem.flush();
        tem.clear();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private static Empresa empresa(String cnpj, String nome) {
        Empresa e = new Empresa();
        e.setCnpj(cnpj);
        e.setRazaoSocial(nome);
        e.setSetorAtuacao("Indústria");
        e.setPorte(PorteEmpresa.MEDIO);
        e.setEmailContato("contato@" + cnpj + ".com");
        return e;
    }

    private static Funcao funcao(Long empresaId, String nome) {
        Funcao f = new Funcao();
        f.setEmpresaId(empresaId);
        f.setNome(nome);
        return f;
    }

    private static void autenticarComo(Perfil perfil, Long empresaId) {
        UsuarioPrincipal p = new UsuarioPrincipal(1L, "Teste", "t@t.com", "hash", perfil, empresaId, true, true, false);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities()));
    }

    @Test
    void usuarioDeEmpresaEnxergaSomenteDadosDaPropriaEmpresa() {
        autenticarComo(Perfil.RH_EMPRESA, empresaA);

        assertThat(funcaoRepository.findAll())
                .hasSize(1)
                .allSatisfy(f -> assertThat(f.getEmpresaId()).isEqualTo(empresaA));
    }

    @Test
    void empresaBNaoVeFuncoesDaEmpresaA() {
        autenticarComo(Perfil.ESPECIALISTA_SST, empresaB);

        assertThat(funcaoRepository.findAll()).extracting(Funcao::getNome).containsExactlyInAnyOrder("Soldador", "Pintor");
    }

    @Test
    void buscaPorIdComEmpresaNaoRetornaRegistroDeOutroTenant() {
        autenticarComo(Perfil.RH_EMPRESA, empresaA);

        assertThat(funcaoRepository.findByIdAndEmpresaId(funcaoDaB, empresaA)).isEmpty();
    }

    @Test
    void administradorEnxergaTodasAsEmpresas() {
        autenticarComo(Perfil.ADMINISTRADOR, null);

        assertThat(funcaoRepository.findAll()).hasSize(3);
    }

    @Test
    void processoSemUsuarioComoOCronEnxergaTudo() {
        assertThat(funcaoRepository.findAll()).hasSize(3);
    }

    @Test
    void naoPermiteGravarRegistroDeOutraEmpresa() {
        autenticarComo(Perfil.RH_EMPRESA, empresaA);

        assertThatThrownBy(() -> funcaoRepository.saveAndFlush(funcao(empresaB, "Intruso")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void empresaDoRegistroEHerdadaDoUsuarioQuandoNaoInformada() {
        autenticarComo(Perfil.RH_EMPRESA, empresaA);

        Funcao salva = funcaoRepository.saveAndFlush(funcao(null, "Eletricista"));

        assertThat(salva.getEmpresaId()).isEqualTo(empresaA);
    }
}
