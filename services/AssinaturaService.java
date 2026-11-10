package br.com.saluris.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.Exame;
import br.com.saluris.domain.entity.Laudo;
import br.com.saluris.domain.entity.MatrizExame;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.domain.enums.StatusFluxoExame;
import br.com.saluris.domain.enums.StatusLaudo;
import br.com.saluris.domain.enums.TipoNotificacao;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.repository.ExameRepository;
import br.com.saluris.repository.LaudoRepository;
import br.com.saluris.repository.MatrizExameRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.shared.util.ValidadeCalculator;
import br.com.saluris.tenant.TenantContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Fila de assinaturas médicas de laudos e exames (Quadro 11 / RN05 / RF18-RF19).
 * <p>Ao homologar um exame, gera automaticamente o registro PENDENTE do próximo ciclo periódico.
 */
@Service
public class AssinaturaService {

    public record RecusaRequest(
            @NotBlank(message = "Informe a justificativa da recusa")
            @Size(max = 1000)
            String justificativa) {
    }

    public record FilaAssinaturasResponse(
            List<LaudoService.LaudoResponse> laudos,
            List<ExameService.ExameResponse> exames) {
    }

    private final LaudoRepository laudoRepository;
    private final ExameRepository exameRepository;
    private final UsuarioRepository usuarioRepository;
    private final MatrizExameRepository matrizRepository;
    private final NotificacaoService notificacaoService;
    private final AuditoriaService auditoriaService;

    public AssinaturaService(LaudoRepository laudoRepository,
            ExameRepository exameRepository,
            UsuarioRepository usuarioRepository,
            MatrizExameRepository matrizRepository,
            NotificacaoService notificacaoService,
            AuditoriaService auditoriaService) {
        this.laudoRepository = laudoRepository;
        this.exameRepository = exameRepository;
        this.usuarioRepository = usuarioRepository;
        this.matrizRepository = matrizRepository;
        this.notificacaoService = notificacaoService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public FilaAssinaturasResponse listarPendentes() {
        Long empresaId = TenantContext.resolverEmpresaId(null);
        LocalDate hoje = LocalDate.now();
        List<LaudoService.LaudoResponse> laudos = laudoRepository
                .findByEmpresaIdAndStatusOrderByCriadoEmDesc(empresaId, StatusLaudo.AGUARDANDO_ASSINATURA)
                .stream()
                .map(LaudoService::paraResposta)
                .toList();
        List<ExameService.ExameResponse> exames = exameRepository
                .findByEmpresaIdAndStatusFluxoOrderByCriadoEmAsc(empresaId, StatusFluxoExame.AGUARDANDO_ASSINATURA)
                .stream()
                .map(e -> ExameService.paraResposta(e, hoje))
                .toList();
        return new FilaAssinaturasResponse(laudos, exames);
    }

    @Transactional
    public LaudoService.LaudoResponse assinarLaudo(Long id) {
        Usuario medico = medicoLogado();
        Laudo laudo = laudoRepository.findByIdAndEmpresaId(id, medico.getEmpresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laudo não encontrado."));
        laudo.homologar(medico);

        auditoriaService.registrar("ASSINAR_LAUDO", "laudos", laudo.getId(), laudo.getEmpresaId(),
                Map.of("crm", medico.getCrm() + "/" + medico.getCrmUf(), "titulo", laudo.getTitulo()));

        String titulo = "Laudo homologado: " + laudo.getTitulo();
        String msg = "O laudo '" + laudo.getTitulo() + "' foi assinado e homologado pelo Dr(a). "
                + medico.getNome() + " (CRM " + medico.getCrm() + "/" + medico.getCrmUf() + ").";
        notificacaoService.notificarUsuario(laudo.getCriadoPor(),
                TipoNotificacao.LAUDO_HOMOLOGADO, titulo, msg, null, null, laudo, null);

        return LaudoService.paraResposta(laudo);
    }

    @Transactional
    public LaudoService.LaudoResponse recusarLaudo(Long id, RecusaRequest req) {
        Usuario medico = medicoLogado();
        Laudo laudo = laudoRepository.findByIdAndEmpresaId(id, medico.getEmpresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laudo não encontrado."));
        laudo.recusar(medico, req.justificativa());

        auditoriaService.registrar("RECUSAR_LAUDO", "laudos", laudo.getId(), laudo.getEmpresaId(),
                Map.of("justificativa", req.justificativa().trim(), "titulo", laudo.getTitulo()));

        String titulo = "Laudo em revisão pendente: " + laudo.getTitulo();
        String msg = "O laudo '" + laudo.getTitulo() + "' foi devolvido para revisão pelo Dr(a). "
                + medico.getNome() + ". Justificativa: " + req.justificativa().trim();
        notificacaoService.notificarUsuario(laudo.getCriadoPor(),
                TipoNotificacao.LAUDO_REVISADO_PENDENTE, titulo, msg, null, null, laudo, null);

        return LaudoService.paraResposta(laudo);
    }

    @Transactional
    public ExameService.ExameResponse assinarExame(Long id) {
        Usuario medico = medicoLogado();
        Exame exame = exameRepository.findByIdAndEmpresaId(id, medico.getEmpresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exame não encontrado."));
        exame.homologar(medico);

        auditoriaService.registrar("ASSINAR_EXAME", "exames", exame.getId(), exame.getEmpresaId(),
                Map.of("crm", medico.getCrm() + "/" + medico.getCrmUf(),
                        "colaborador", exame.getColaborador().getNome(),
                        "tipoExame", exame.getTipoExame().getNome()));

        LocalDate hoje = LocalDate.now();
        gerarProximoCicloSeNecessario(exame, hoje);

        String titulo = "Exame homologado: " + exame.getTipoExame().getNome() + " - " + exame.getColaborador().getNome();
        String msg = "O exame " + exame.getTipoExame().getNome() + " do colaborador "
                + exame.getColaborador().getNome() + " foi homologado pelo Dr(a). " + medico.getNome()
                + " (CRM " + medico.getCrm() + "/" + medico.getCrmUf() + ").";
        notificacaoService.notificarPerfisDaEmpresa(exame.getEmpresaId(),
                Set.of(Perfil.RH_EMPRESA, Perfil.ESPECIALISTA_SST),
                TipoNotificacao.EXAME_HOMOLOGADO, titulo, msg, exame, null, null, null);

        return ExameService.paraResposta(exame, hoje);
    }

    @Transactional
    public ExameService.ExameResponse recusarExame(Long id, RecusaRequest req) {
        Usuario medico = medicoLogado();
        Exame exame = exameRepository.findByIdAndEmpresaId(id, medico.getEmpresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exame não encontrado."));
        exame.recusar(medico, req.justificativa());

        auditoriaService.registrar("RECUSAR_EXAME", "exames", exame.getId(), exame.getEmpresaId(),
                Map.of("justificativa", req.justificativa().trim(),
                        "colaborador", exame.getColaborador().getNome()));

        String titulo = "Exame devolvido para revisão: " + exame.getTipoExame().getNome();
        String msg = "O exame " + exame.getTipoExame().getNome() + " do colaborador "
                + exame.getColaborador().getNome() + " foi recusado pelo Dr(a). " + medico.getNome()
                + ". Justificativa: " + req.justificativa().trim();
        notificacaoService.notificarPerfisDaEmpresa(exame.getEmpresaId(),
                Set.of(Perfil.RH_EMPRESA, Perfil.ESPECIALISTA_SST),
                TipoNotificacao.EXAME_REVISADO_PENDENTE, titulo, msg, exame, null, null, null);

        return ExameService.paraResposta(exame, LocalDate.now());
    }

    private void gerarProximoCicloSeNecessario(Exame homologado, LocalDate hoje) {
        var colab = homologado.getColaborador();
        if (!colab.isAtivo()) {
            return;
        }
        boolean jaExisteAberto = exameRepository.existsByColaboradorIdAndTipoExameIdAndStatusFluxoIn(
                colab.getId(), homologado.getTipoExame().getId(), ExameService.STATUS_EM_ABERTO);
        if (jaExisteAberto) {
            return;
        }
        MatrizExame regraAtual = matrizRepository
                .findByFuncaoIdAndSetorIdAndTipoExameId(
                        colab.getFuncao().getId(), colab.getSetor().getId(), homologado.getTipoExame().getId())
                .orElse(homologado.getMatrizExame());
        int periodicidade = regraAtual != null ? regraAtual.getPeriodicidadeMeses() : homologado.getPeriodicidadeMeses();
        LocalDate base = homologado.getDataRealizacao() != null ? homologado.getDataRealizacao() : hoje;
        LocalDate proximoVencimento = base.plusMonths(periodicidade);

        Exame proximo = new Exame();
        proximo.setEmpresaId(homologado.getEmpresaId());
        proximo.setColaborador(colab);
        proximo.setTipoExame(homologado.getTipoExame());
        proximo.setMatrizExame(regraAtual);
        proximo.setPeriodicidadeMeses(periodicidade);
        proximo.setDataVencimento(proximoVencimento);
        proximo.setStatusValidade(ValidadeCalculator.classificar(proximoVencimento, hoje));
        proximo.setStatusFluxo(StatusFluxoExame.PENDENTE);
        exameRepository.save(proximo);
    }

    private Usuario medicoLogado() {
        Long id = TenantContext.exigirUsuario().getId();
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Médico não encontrado."));
    }
}
