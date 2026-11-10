package br.com.saluris.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.saluris.domain.entity.ArquivoPdf;
import br.com.saluris.domain.entity.AssinaturaMedica;
import br.com.saluris.domain.entity.Colaborador;
import br.com.saluris.domain.entity.Exame;
import br.com.saluris.domain.entity.MatrizExame;
import br.com.saluris.domain.entity.TipoExame;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.domain.enums.StatusFluxoExame;
import br.com.saluris.domain.enums.StatusValidade;
import br.com.saluris.domain.enums.TipoNotificacao;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.exception.RegraNegocioException;
import br.com.saluris.repository.ExameRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.shared.dto.PageResponse;
import br.com.saluris.shared.util.ValidadeCalculator;
import br.com.saluris.tenant.TenantContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Gestão de exames ocupacionais: listagem, agendamento (Quadro 14 / RF25), envio de resultado em PDF
 * e criação de ciclos de exames.
 */
@Service
public class ExameService {

    public static final Set<StatusFluxoExame> STATUS_EM_ABERTO = Set.of(
            StatusFluxoExame.PENDENTE,
            StatusFluxoExame.AGENDADO,
            StatusFluxoExame.REALIZADO,
            StatusFluxoExame.AGUARDANDO_ASSINATURA,
            StatusFluxoExame.REVISADO_PENDENTE);

    public record AgendarExameRequest(
            @NotNull(message = "Informe a data agendada") LocalDate dataAgendada,
            @NotBlank(message = "Informe o local ou clínica") @Size(max = 200) String localClinica,
            @Size(max = 1000) String observacoes) {
    }

    public record ExameResponse(
            Long id,
            Long colaboradorId,
            String colaboradorNome,
            Long tipoExameId,
            String tipoExameNome,
            Integer periodicidadeMeses,
            LocalDate dataAgendada,
            String localClinica,
            String observacoes,
            LocalDate dataRealizacao,
            LocalDate dataVencimento,
            long diasParaVencer,
            StatusValidade statusValidade,
            StatusFluxoExame statusFluxo,
            boolean possuiArquivo,
            String medicoNome,
            String crmAssinatura,
            String crmUfAssinatura,
            OffsetDateTime assinadoEm,
            String justificativaRecusa) {
    }

    private final ExameRepository exameRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArquivoStorageService arquivoStorageService;
    private final NotificacaoService notificacaoService;
    private final EmailService emailService;

    public ExameService(ExameRepository exameRepository,
            UsuarioRepository usuarioRepository,
            ArquivoStorageService arquivoStorageService,
            NotificacaoService notificacaoService,
            EmailService emailService) {
        this.exameRepository = exameRepository;
        this.usuarioRepository = usuarioRepository;
        this.arquivoStorageService = arquivoStorageService;
        this.notificacaoService = notificacaoService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExameResponse> listar(Long empresaIdParam, Long colaboradorId, Long tipoExameId,
            StatusValidade statusValidade, StatusFluxoExame statusFluxo, boolean incluirHistorico, Pageable pageable) {
        Long empresaId = TenantContext.resolverEmpresaIdOpcional(empresaIdParam);
        LocalDate hoje = LocalDate.now();
        Page<Exame> page = exameRepository.filtrar(empresaId, colaboradorId, tipoExameId, statusValidade, statusFluxo,
                incluirHistorico, pageable);
        return PageResponse.de(page, e -> paraResposta(e, hoje));
    }

    @Transactional
    public ExameResponse agendar(Long id, AgendarExameRequest req) {
        Exame exame = buscarDoTenant(id);
        Usuario agendador = usuarioRepository.findById(TenantContext.exigirUsuario().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        exame.agendar(req.dataAgendada(), req.localClinica().trim(),
                req.observacoes() != null && !req.observacoes().isBlank() ? req.observacoes().trim() : null,
                agendador);

        Colaborador colab = exame.getColaborador();
        String titulo = "Exame agendado: " + exame.getTipoExame().getNome() + " - " + colab.getNome();
        String msg = "O exame " + exame.getTipoExame().getNome() + " do colaborador " + colab.getNome()
                + " foi agendado para " + req.dataAgendada() + " no local/clínica: " + exame.getLocalClinica() + ".";
        notificacaoService.notificarPerfisDaEmpresa(exame.getEmpresaId(),
                Set.of(Perfil.RH_EMPRESA, Perfil.ESPECIALISTA_SST),
                TipoNotificacao.EXAME_AGENDADO, titulo, msg, exame, null, null, null);

        if (colab.getEmail() != null && !colab.getEmail().isBlank()) {
            emailService.enfileirar(null, colab.getEmail(), "[Saluris IA] " + titulo, msg, false);
        }
        return paraResposta(exame, LocalDate.now());
    }

    @Transactional
    public ExameResponse registrarResultado(Long id, LocalDate dataRealizacao, MultipartFile arquivo) {
        if (dataRealizacao == null) {
            throw new RegraNegocioException("Informe a data de realização do exame.");
        }
        LocalDate hoje = LocalDate.now();
        if (dataRealizacao.isAfter(hoje)) {
            throw new RegraNegocioException("A data de realização do exame não pode ser futura.");
        }
        Exame exame = buscarDoTenant(id);
        if (exame.getStatusFluxo() == StatusFluxoExame.HOMOLOGADO) {
            throw new RegraNegocioException("Este exame já foi homologado.");
        }
        ArquivoPdf pdf = arquivoStorageService.armazenarPdf(arquivo, exame.getEmpresaId());
        exame.setDataRealizacao(dataRealizacao);
        exame.setDataVencimento(dataRealizacao.plusMonths(exame.getPeriodicidadeMeses()));
        exame.setStatusValidade(ValidadeCalculator.classificar(exame.getDataVencimento(), hoje));
        exame.setArquivo(pdf);
        exame.enviarParaAssinatura();

        String titulo = "Exame aguardando assinatura: " + exame.getTipoExame().getNome();
        String msg = "O exame " + exame.getTipoExame().getNome() + " do colaborador "
                + exame.getColaborador().getNome() + " aguarda validação e assinatura médica.";
        notificacaoService.notificarPerfisDaEmpresa(exame.getEmpresaId(),
                Set.of(Perfil.MEDICO_TRABALHO),
                TipoNotificacao.EXAME_PENDENTE_ASSINATURA, titulo, msg, exame, null, null, null);

        return paraResposta(exame, hoje);
    }

    @Transactional(readOnly = true)
    public ArquivoStorageService.PdfCarregado baixarArquivo(Long id) {
        Exame exame = buscarDoTenant(id);
        return arquivoStorageService.carregarPdf(exame.getArquivo());
    }

    /**
     * Cria um ciclo em aberto (PENDENTE) para o colaborador a partir de um item da matriz.
     */
    @Transactional
    public Exame criarCicloPendente(Colaborador colab, MatrizExame item, LocalDate dataBase, LocalDate hoje) {
        Exame e = new Exame();
        e.setEmpresaId(colab.getEmpresaId());
        e.setColaborador(colab);
        e.setTipoExame(item.getTipoExame());
        e.setMatrizExame(item);
        e.setPeriodicidadeMeses(item.getPeriodicidadeMeses());
        LocalDate venc = dataBase.plusMonths(item.getPeriodicidadeMeses());
        e.setDataVencimento(venc);
        e.setStatusValidade(ValidadeCalculator.classificar(venc, hoje));
        e.setStatusFluxo(StatusFluxoExame.PENDENTE);
        return exameRepository.save(e);
    }

    /**
     * Calcula a data base para recálculo de vencimento de um exame em aberto (RF08):
     * 1) dataRealizacao do próprio registro, se houver;
     * 2) dataRealizacao do último exame HOMOLOGADO do mesmo tipo;
     * 3) dataAdmissao do colaborador.
     */
    @Transactional(readOnly = true)
    public LocalDate obterDataBaseRecalculo(Exame exameAberto) {
        if (exameAberto.getDataRealizacao() != null) {
            return exameAberto.getDataRealizacao();
        }
        return exameRepository.findTopByColaboradorIdAndTipoExameIdAndStatusFluxoOrderByDataRealizacaoDesc(
                        exameAberto.getColaborador().getId(),
                        exameAberto.getTipoExame().getId(),
                        StatusFluxoExame.HOMOLOGADO)
                .map(Exame::getDataRealizacao)
                .filter(java.util.Objects::nonNull)
                .orElse(exameAberto.getColaborador().getDataAdmissao());
    }

    @Transactional(readOnly = true)
    public LocalDate obterDataBaseNovoExame(Colaborador colab, TipoExame tipoExame) {
        return exameRepository.findTopByColaboradorIdAndTipoExameIdAndStatusFluxoOrderByDataRealizacaoDesc(
                        colab.getId(), tipoExame.getId(), StatusFluxoExame.HOMOLOGADO)
                .map(Exame::getDataRealizacao)
                .filter(java.util.Objects::nonNull)
                .orElse(colab.getDataAdmissao());
    }

    public Exame buscarDoTenant(Long id) {
        if (TenantContext.isAdministrador()) {
            return exameRepository.findById(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Exame não encontrado."));
        }
        Long empresaId = TenantContext.resolverEmpresaId(null);
        return exameRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exame não encontrado."));
    }

    public static ExameResponse paraResposta(Exame e, LocalDate hoje) {
        AssinaturaMedica a = e.getAssinatura();
        String medicoNome = a != null && a.getMedico() != null ? a.getMedico().getNome() : null;
        return new ExameResponse(
                e.getId(),
                e.getColaborador().getId(),
                e.getColaborador().getNome(),
                e.getTipoExame().getId(),
                e.getTipoExame().getNome(),
                e.getPeriodicidadeMeses(),
                e.getDataAgendada(),
                e.getLocalClinica(),
                e.getObservacoes(),
                e.getDataRealizacao(),
                e.getDataVencimento(),
                ValidadeCalculator.diasParaVencer(e.getDataVencimento(), hoje),
                e.getStatusValidade(),
                e.getStatusFluxo(),
                e.getArquivo() != null && e.getArquivo().getPath() != null,
                medicoNome,
                a != null ? a.getCrmAssinatura() : null,
                a != null ? a.getCrmUfAssinatura() : null,
                a != null ? a.getAssinadoEm() : null,
                a != null ? a.getJustificativaRecusa() : null);
    }
}
