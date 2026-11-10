package br.com.saluris.service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.Empresa;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.exception.ConflitoException;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.exception.RegraNegocioException;
import br.com.saluris.repository.EmpresaRepository;
import br.com.saluris.repository.UsuarioRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Gestão de usuários e perfis (Quadro 01 / RF01-RF02).
 * <p>Gera senha temporária segura, envia instruções de acesso por e-mail e registra log de auditoria.
 */
@Service
public class UsuarioService {

    private static final String LETRAS_MAIUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LETRAS_MINUSCULAS = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITOS = "23456789";
    private static final String SIMBOLOS = "@#$!";
    private static final String TODOS = LETRAS_MAIUSCULAS + LETRAS_MINUSCULAS + DIGITOS + SIMBOLOS;
    private static final SecureRandom RANDOM = new SecureRandom();

    public record UsuarioCreateRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150) String nome,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 255) String email,
            @NotNull(message = "Informe o perfil") Perfil perfil,
            Long empresaId,
            @Size(max = 20) String crm,
            @Size(max = 2) String crmUf) {
    }

    public record UsuarioUpdateRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150) String nome,
            @NotNull(message = "Informe o perfil") Perfil perfil,
            Long empresaId,
            @Size(max = 20) String crm,
            @Size(max = 2) String crmUf) {
    }

    public record UsuarioResponse(
            Long id,
            String nome,
            String email,
            Perfil perfil,
            Long empresaId,
            String empresaNome,
            String crm,
            String crmUf,
            boolean ativo,
            boolean deveTrocarSenha,
            OffsetDateTime ultimoAcesso) {
    }

    public record UsuarioCriadoResponse(
            UsuarioResponse usuario,
            String senhaTemporaria) {
    }

    public record SenhaTemporariaResponse(String senhaTemporaria) {
    }

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Long empresaId, Perfil perfil) {
        return usuarioRepository.filtrar(empresaId, perfil)
                .stream()
                .map(this::paraResposta)
                .toList();
    }

    @Transactional
    public UsuarioCriadoResponse criar(UsuarioCreateRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe um usuário cadastrado com este e-mail.");
        }
        validarPerfilEEmpresa(req.perfil(), req.empresaId(), req.crm(), req.crmUf());

        String senhaTemporaria = gerarSenhaTemporaria();
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setPerfil(req.perfil());
        u.setEmpresaId(req.perfil() == Perfil.ADMINISTRADOR ? null : req.empresaId());
        preencherCrm(u, req.perfil(), req.crm(), req.crmUf());
        u.setSenhaHash(passwordEncoder.encode(senhaTemporaria));
        u.setDeveTrocarSenha(true);
        u.setAtivo(true);

        Usuario salvo = usuarioRepository.save(u);

        auditoriaService.registrar("CRIAR_USUARIO", "usuarios", salvo.getId(), salvo.getEmpresaId(),
                Map.of("email", salvo.getEmail(), "perfil", salvo.getPerfil().name()));

        String corpoEmail = "Olá, " + salvo.getNome() + "!\n\n"
                + "Sua conta no sistema Saluris IA foi criada com o perfil " + salvo.getPerfil().getRotulo() + ".\n"
                + "E-mail de acesso: " + salvo.getEmail() + "\n"
                + "Senha temporária: " + senhaTemporaria + "\n\n"
                + "No primeiro acesso você deverá definir uma senha definitiva.";
        emailService.enfileirar(null, salvo.getEmail(), "[Saluris IA] Credenciais de acesso", corpoEmail, true);

        return new UsuarioCriadoResponse(paraResposta(salvo), senhaTemporaria);
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioUpdateRequest req) {
        Usuario u = buscarEntidade(id);
        validarPerfilEEmpresa(req.perfil(), req.empresaId(), req.crm(), req.crmUf());
        Long novoEmpresaId = req.perfil() == Perfil.ADMINISTRADOR ? null : req.empresaId();
        if (u.getId() != null && !java.util.Objects.equals(u.getEmpresaId(), novoEmpresaId)) {
            throw new RegraNegocioException("Não é permitido alterar o vínculo de empresa de um usuário já existente.");
        }
        u.setNome(req.nome().trim());
        u.setPerfil(req.perfil());
        preencherCrm(u, req.perfil(), req.crm(), req.crmUf());

        auditoriaService.registrar("ATUALIZAR_USUARIO", "usuarios", u.getId(), u.getEmpresaId(),
                Map.of("email", u.getEmail(), "perfil", u.getPerfil().name()));
        return paraResposta(u);
    }

    @Transactional
    public UsuarioResponse alterarAtivo(Long id, boolean ativo) {
        Usuario u = buscarEntidade(id);
        u.setAtivo(ativo);
        auditoriaService.registrar(ativo ? "ATIVAR_USUARIO" : "DESATIVAR_USUARIO", "usuarios", u.getId(),
                u.getEmpresaId(), Map.of("ativo", ativo));
        return paraResposta(u);
    }

    @Transactional
    public SenhaTemporariaResponse redefinirSenha(Long id) {
        Usuario u = buscarEntidade(id);
        String senhaTemporaria = gerarSenhaTemporaria();
        u.setSenhaHash(passwordEncoder.encode(senhaTemporaria));
        u.setDeveTrocarSenha(true);

        auditoriaService.registrar("REDEFINIR_SENHA", "usuarios", u.getId(), u.getEmpresaId(),
                Map.of("email", u.getEmail()));

        String corpoEmail = "Olá, " + u.getNome() + "!\n\n"
                + "Sua senha no sistema Saluris IA foi redefinida pelo Administrador.\n"
                + "Nova senha temporária: " + senhaTemporaria + "\n\n"
                + "Ao entrar no sistema, será solicitada a definição de uma nova senha.";
        emailService.enfileirar(null, u.getEmail(), "[Saluris IA] Redefinição de senha", corpoEmail, true);
        return new SenhaTemporariaResponse(senhaTemporaria);
    }

    @Transactional(readOnly = true)
    public Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private void validarPerfilEEmpresa(Perfil perfil, Long empresaId, String crm, String crmUf) {
        if (perfil == Perfil.ADMINISTRADOR) {
            if (empresaId != null) {
                throw new RegraNegocioException("O perfil Administrador (T.I.) não deve ser vinculado a uma empresa.");
            }
        } else {
            if (empresaId == null) {
                throw new RegraNegocioException("Informe a empresa para o perfil " + perfil.getRotulo() + ".");
            }
            if (!empresaRepository.existsById(empresaId)) {
                throw new RegraNegocioException("Empresa informada não existe.");
            }
        }
        if (perfil == Perfil.MEDICO_TRABALHO) {
            if (crm == null || crm.isBlank() || crmUf == null || crmUf.trim().length() != 2) {
                throw new RegraNegocioException("Médico do Trabalho exige CRM e UF do CRM (2 letras).");
            }
        }
    }

    private static void preencherCrm(Usuario u, Perfil perfil, String crm, String crmUf) {
        if (perfil == Perfil.MEDICO_TRABALHO) {
            u.setCrm(crm.trim());
            u.setCrmUf(crmUf.trim().toUpperCase());
        } else {
            u.setCrm(null);
            u.setCrmUf(null);
        }
    }

    private static String gerarSenhaTemporaria() {
        StringBuilder sb = new StringBuilder(12);
        sb.append(LETRAS_MAIUSCULAS.charAt(RANDOM.nextInt(LETRAS_MAIUSCULAS.length())));
        sb.append(LETRAS_MINUSCULAS.charAt(RANDOM.nextInt(LETRAS_MINUSCULAS.length())));
        sb.append(DIGITOS.charAt(RANDOM.nextInt(DIGITOS.length())));
        sb.append(SIMBOLOS.charAt(RANDOM.nextInt(SIMBOLOS.length())));
        for (int i = 4; i < 12; i++) {
            sb.append(TODOS.charAt(RANDOM.nextInt(TODOS.length())));
        }
        return sb.toString();
    }

    private UsuarioResponse paraResposta(Usuario u) {
        String empresaNome = u.getEmpresaId() == null ? null
                : empresaRepository.findById(u.getEmpresaId())
                        .map(Empresa::getRazaoSocial)
                        .orElse(null);
        return new UsuarioResponse(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getPerfil(),
                u.getEmpresaId(),
                empresaNome,
                u.getCrm(),
                u.getCrmUf(),
                u.isAtivo(),
                u.isDeveTrocarSenha(),
                u.getUltimoAcesso());
    }
}
