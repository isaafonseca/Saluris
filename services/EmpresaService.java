package br.com.saluris.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.Empresa;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.domain.enums.PorteEmpresa;
import br.com.saluris.exception.ConflitoException;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.exception.RegraNegocioException;
import br.com.saluris.repository.EmpresaRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.shared.validation.Cnpj;
import br.com.saluris.shared.validation.DocumentosBr;
import br.com.saluris.tenant.TenantContext;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Service
public class EmpresaService {

    public record EmpresaForm(
            @NotBlank(message = "Informe o CNPJ") @Cnpj String cnpj,
            @NotBlank(message = "Informe a razão social") @Size(max = 200) String razaoSocial,
            @Size(max = 200) String nomeFantasia,
            @NotBlank(message = "Informe o setor de atuação") @Size(max = 150) String setorAtuacao,
            @NotNull(message = "Informe o porte da empresa") PorteEmpresa porte,
            @Size(max = 150) String contatoNome,
            @NotBlank(message = "Informe o e-mail de contato") @Email @Size(max = 255) String emailContato,
            @Size(max = 20) String telefone,
            @Email(message = "E-mail do administrador da empresa inválido") @Size(max = 255) String adminEmail,
            @Size(max = 150) String adminNome) {

        public EmpresaForm(String cnpj, String razaoSocial, String nomeFantasia, String setorAtuacao,
                PorteEmpresa porte, String contatoNome, String emailContato, String telefone) {
            this(cnpj, razaoSocial, nomeFantasia, setorAtuacao, porte, contatoNome, emailContato, telefone, null, null);
        }
    }

    public record EmpresaResponse(
            Long id,
            String cnpj,
            String razaoSocial,
            String nomeFantasia,
            String setorAtuacao,
            PorteEmpresa porte,
            String contatoNome,
            String emailContato,
            String telefone,
            boolean ativa,
            String adminEmail,
            String adminNome,
            String adminSenhaTemporaria) {

        public EmpresaResponse(Long id, String cnpj, String razaoSocial, String nomeFantasia, String setorAtuacao,
                PorteEmpresa porte, String contatoNome, String emailContato, String telefone, boolean ativa) {
            this(id, cnpj, razaoSocial, nomeFantasia, setorAtuacao, porte, contatoNome, emailContato, telefone, ativa,
                    null, null, null);
        }
    }

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;

    public EmpresaService(EmpresaRepository empresaRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            AuditoriaService auditoriaService) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponse> listar() {
        List<Empresa> lista = empresaRepository.findAll(Sort.by(Sort.Direction.ASC, "razaoSocial"));
        Map<Long, Usuario> admins = carregarAdminsPrincipaisVisiveis();
        return lista.stream()
                .map(e -> paraResposta(e, admins.get(e.getId()), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public EmpresaResponse buscar(Long id) {
        Empresa e = buscarEntidade(id);
        Usuario admin = podeVerContaPrincipal()
                ? usuarioRepository.findByPerfil(Perfil.ADMIN_EMPRESA).stream()
                        .filter(u -> e.getId().equals(u.getEmpresaId()))
                        .findFirst()
                        .orElse(null)
                : null;
        return paraResposta(e, admin, null);
    }

    @Transactional
    public EmpresaResponse criar(EmpresaForm form) {
        String cnpj = DocumentosBr.somenteDigitos(form.cnpj());
        if (empresaRepository.existsByCnpj(cnpj)) {
            throw new ConflitoException("Já existe uma empresa cadastrada com este CNPJ.");
        }
        if (form.adminEmail() == null || form.adminEmail().isBlank()) {
            throw new RegraNegocioException(
                    "Informe o e-mail da Conta Principal (Administrador da Empresa) vinculada a esta empresa.");
        }
        String adminEmail = form.adminEmail().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(adminEmail)) {
            throw new ConflitoException("Já existe um usuário cadastrado com este e-mail de administrador.");
        }

        Empresa e = new Empresa();
        preencher(e, form, cnpj);
        Empresa salva = empresaRepository.save(e);

        String adminNome = resolverNomeAdminEmpresa(form.adminNome(), salva);
        String senhaTemporaria = UsuarioService.gerarSenhaTemporaria();
        Usuario adminEmpresa = new Usuario();
        adminEmpresa.setEmpresaId(salva.getId());
        adminEmpresa.setNome(adminNome);
        adminEmpresa.setEmail(adminEmail);
        adminEmpresa.setPerfil(Perfil.ADMIN_EMPRESA);
        adminEmpresa.setSenhaHash(passwordEncoder.encode(senhaTemporaria));
        adminEmpresa.setDeveTrocarSenha(true);
        adminEmpresa.setAtivo(true);
        Usuario adminSalvo = usuarioRepository.save(adminEmpresa);

        auditoriaService.registrar("CRIAR_EMPRESA", "empresas", salva.getId(), salva.getId(),
                Map.of("cnpj", cnpj, "razaoSocial", salva.getRazaoSocial(), "adminEmail", adminEmail));

        String corpoEmail = "Olá, " + adminSalvo.getNome() + "!\n\n"
                + "A empresa " + salva.getRazaoSocial() + " foi cadastrada no sistema Saluris IA e vinculada ao seu e-mail como Conta Principal da Empresa.\n"
                + "Apenas esta conta principal tem acesso para criar e gerenciar os usuários internos da empresa (RH, Médico do Trabalho e Especialista SST).\n\n"
                + "E-mail de acesso: " + adminSalvo.getEmail() + "\n"
                + "Senha temporária: " + senhaTemporaria + "\n\n"
                + "No primeiro acesso você deverá definir uma senha definitiva.";
        emailService.enfileirar(null, adminSalvo.getEmail(), "[Saluris IA] Conta Principal da Empresa criada",
                corpoEmail, true);

        return paraResposta(salva, adminSalvo, senhaTemporaria);
    }

    @Transactional
    public EmpresaResponse atualizar(Long id, EmpresaForm form) {
        Empresa e = buscarEntidade(id);
        String cnpj = DocumentosBr.somenteDigitos(form.cnpj());
        empresaRepository.findByCnpj(cnpj)
                .filter(outra -> !outra.getId().equals(id))
                .ifPresent(outra -> {
                    throw new ConflitoException("Já existe outra empresa cadastrada com este CNPJ.");
                });
        preencher(e, form, cnpj);
        auditoriaService.registrar("ATUALIZAR_EMPRESA", "empresas", e.getId(), e.getId(),
                Map.of("cnpj", cnpj, "razaoSocial", e.getRazaoSocial()));
        return buscar(e.getId());
    }

    @Transactional
    public EmpresaResponse alterarAtiva(Long id, boolean ativa) {
        Empresa e = buscarEntidade(id);
        e.setAtiva(ativa);
        auditoriaService.registrar(ativa ? "ATIVAR_EMPRESA" : "DESATIVAR_EMPRESA", "empresas", e.getId(), e.getId(),
                Map.of("ativa", ativa));
        return buscar(e.getId());
    }

    @Transactional(readOnly = true)
    public Empresa buscarEntidade(Long id) {
        List<Empresa> visiveis = empresaRepository.findAll();
        return visiveis.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada."));
    }

    private Map<Long, Usuario> carregarAdminsPrincipaisVisiveis() {
        if (!podeVerContaPrincipal()) {
            return Map.of();
        }
        return usuarioRepository.findByPerfil(Perfil.ADMIN_EMPRESA).stream()
                .filter(u -> u.getEmpresaId() != null)
                .collect(Collectors.toMap(Usuario::getEmpresaId, u -> u, (a, b) -> a));
    }

    private static boolean podeVerContaPrincipal() {
        return TenantContext.isAdministrador() || TenantContext.isAdminEmpresa();
    }

    private static String resolverNomeAdminEmpresa(String informado, Empresa empresa) {
        String base = (informado != null && !informado.isBlank())
                ? informado.trim()
                : "Admin — " + (empresa.getNomeFantasia() != null && !empresa.getNomeFantasia().isBlank()
                        ? empresa.getNomeFantasia()
                        : empresa.getRazaoSocial());
        return base.length() <= 150 ? base : base.substring(0, 150);
    }

    private static void preencher(Empresa e, EmpresaForm form, String cnpj) {
        e.setCnpj(cnpj);
        e.setRazaoSocial(form.razaoSocial().trim());
        e.setNomeFantasia(form.nomeFantasia() != null && !form.nomeFantasia().isBlank() ? form.nomeFantasia().trim() : null);
        e.setSetorAtuacao(form.setorAtuacao().trim());
        e.setPorte(form.porte());
        e.setContatoNome(form.contatoNome() != null && !form.contatoNome().isBlank() ? form.contatoNome().trim() : null);
        e.setEmailContato(form.emailContato().trim().toLowerCase());
        e.setTelefone(form.telefone() != null && !form.telefone().isBlank() ? form.telefone().trim() : null);
    }

    public static EmpresaResponse paraResposta(Empresa e) {
        return paraResposta(e, null, null);
    }

    public static EmpresaResponse paraResposta(Empresa e, Usuario adminPrincipal, String senhaTemporaria) {
        return new EmpresaResponse(
                e.getId(),
                e.getCnpj(),
                e.getRazaoSocial(),
                e.getNomeFantasia(),
                e.getSetorAtuacao(),
                e.getPorte(),
                e.getContatoNome(),
                e.getEmailContato(),
                e.getTelefone(),
                e.isAtiva(),
                adminPrincipal != null ? adminPrincipal.getEmail() : null,
                adminPrincipal != null ? adminPrincipal.getNome() : null,
                senhaTemporaria);
    }
}
