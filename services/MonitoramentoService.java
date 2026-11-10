package br.com.saluris.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.Documento;
import br.com.saluris.domain.entity.Exame;
import br.com.saluris.domain.entity.MatrizExame;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.domain.enums.StatusFluxoExame;
import br.com.saluris.domain.enums.StatusValidade;
import br.com.saluris.domain.enums.TipoNotificacao;
import br.com.saluris.repository.DocumentoRepository;
import br.com.saluris.repository.ExameRepository;
import br.com.saluris.repository.MatrizExameRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.shared.util.ValidadeCalculator;

/**
 * Rotina diária (CRON às 08h00) de monitoramento de prazos, atualização de status de validade
 * e envio de alertas automáticos em 30, 15, 7 dias e urgência para vencidos (Quadros 16, 17 / RN08 / RF30-RF34).
 */
@Service
public class MonitoramentoService {

    private static final Logger log = LoggerFactory.getLogger(MonitoramentoService.class);

    public record ResultadoMonitoramentoResponse(
            int examesAtualizados,
            int documentosAtualizados,
            int notificacoesCriadas,
            int emailsEnfileirados) {
    }

    private final ExameRepository exameRepository;
    private final DocumentoRepository documentoRepository;
    private final MatrizExameRepository matrizRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoService notificacaoService;
    private final AuditoriaService auditoriaService;

    public MonitoramentoService(ExameRepository exameRepository,
            DocumentoRepository documentoRepository,
            MatrizExameRepository matrizRepository,
            UsuarioRepository usuarioRepository,
            NotificacaoService notificacaoService,
            AuditoriaService auditoriaService) {
        this.exameRepository = exameRepository;
        this.documentoRepository = documentoRepository;
        this.matrizRepository = matrizRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacaoService = notificacaoService;
        this.auditoriaService = auditoriaService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void executarRotinaDiariaAgendada() {
        log.info("Iniciando rotina automática diária de monitoramento de prazos (08h00)...");
        ResultadoMonitoramentoResponse res = executarAgora();
        log.info("Rotina diária concluída: {}", res);
    }

    @Transactional
    public ResultadoMonitoramentoResponse executarAgora() {
        LocalDate hoje = LocalDate.now();
        int examesAtualizados = 0;
        int documentosAtualizados = 0;
        int notificacoesCriadas = 0;

        List<Exame> examesAbertos = exameRepository.findByStatusFluxoNot(StatusFluxoExame.HOMOLOGADO);
        for (Exame ex : examesAbertos) {
            var colab = ex.getColaborador();
            MatrizExame regra = matrizRepository.findByFuncaoIdAndSetorIdAndTipoExameId(
                    colab.getFuncao().getId(), colab.getSetor().getId(), ex.getTipoExame().getId()).orElse(null);

            // Quadro 17 (Fluxo alternativo 3a) / RF34: periodicidade não configurada para função + setor
            if (regra == null && ex.getMatrizExame() == null) {
                log.warn("Inconsistência (RF34): periodicidade não configurada para função '{}' + setor '{}' no exame ID {} (colaborador '{}'). Mantendo status anterior ({}).",
                        colab.getFuncao().getNome(), colab.getSetor().getNome(), ex.getId(), colab.getNome(), ex.getStatusValidade());
                auditoriaService.registrar("INCONSISTENCIA_PERIODICIDADE", "exames", ex.getId(), ex.getEmpresaId(),
                        Map.of("colaborador", colab.getNome(),
                                "funcao", colab.getFuncao().getNome(),
                                "setor", colab.getSetor().getNome(),
                                "tipoExame", ex.getTipoExame().getNome()));
                if (colab.isAtivo()) {
                    String tituloInc = "Periodicidade não configurada: " + colab.getFuncao().getNome() + " / " + colab.getSetor().getNome();
                    String msgInc = "O exame '" + ex.getTipoExame().getNome() + "' do colaborador " + colab.getNome()
                            + " está sem periodicidade configurada na matriz para a combinação Função '"
                            + colab.getFuncao().getNome() + "' + Setor '" + colab.getSetor().getNome() + "'.";
                    notificacaoService.notificarPerfisDaEmpresa(ex.getEmpresaId(),
                            Set.of(Perfil.ESPECIALISTA_SST),
                            TipoNotificacao.PERIODICIDADE_NAO_CONFIGURADA, tituloInc, msgInc, ex, null, null, colab);
                }
                continue;
            }

            // Quadro 17 (Passo 3) / RF33: recalcula data de vencimento (data de realização + periodicidade)
            if (regra != null) {
                ex.setPeriodicidadeMeses(regra.getPeriodicidadeMeses());
                ex.setMatrizExame(regra);
            }
            if (ex.getDataRealizacao() != null && ex.getPeriodicidadeMeses() != null) {
                LocalDate vencRecalculado = ex.getDataRealizacao().plusMonths(ex.getPeriodicidadeMeses());
                ex.setDataVencimento(vencRecalculado);
            }

            StatusValidade novoStatus = ValidadeCalculator.classificar(ex.getDataVencimento(), hoje);
            if (ex.getStatusValidade() != novoStatus) {
                ex.setStatusValidade(novoStatus);
                examesAtualizados++;
            }
            if (!colab.isAtivo()) {
                continue;
            }
            Integer marco = ValidadeCalculator.marcoAlertaHoje(ex.getDataVencimento(), hoje);
            if (marco != null) {
                long dias = ValidadeCalculator.diasParaVencer(ex.getDataVencimento(), hoje);
                String titulo = marco == 0
                        ? "URGENTE - Exame vencido: " + ex.getTipoExame().getNome() + " (" + ex.getColaborador().getNome() + ")"
                        : "Alerta (" + marco + " dias) - Exame próximo do vencimento: " + ex.getTipoExame().getNome();
                String msg = marco == 0
                        ? "O exame " + ex.getTipoExame().getNome() + " do colaborador " + ex.getColaborador().getNome()
                                + " venceu em " + ex.getDataVencimento() + " (" + (-dias) + " dia(s) em atraso)."
                        : "O exame " + ex.getTipoExame().getNome() + " do colaborador " + ex.getColaborador().getNome()
                                + " vencerá em " + ex.getDataVencimento() + " (faltam " + marco + " dias).";

                List<Usuario> responsaveis = usuarioRepository.findByEmpresaIdAndPerfilInAndAtivoTrue(
                        ex.getEmpresaId(), Set.of(Perfil.RH_EMPRESA, Perfil.ESPECIALISTA_SST));
                for (Usuario u : responsaveis) {
                    if (notificacaoService.registrarAlertaExame(u, ex, marco, hoje, titulo, msg)) {
                        notificacoesCriadas++;
                    }
                }
            }
        }

        List<Documento> todosDocs = documentoRepository.findAll();
        Map<String, Integer> maiorVersaoPorEmpresaTipo = todosDocs.stream()
                .collect(Collectors.toMap(
                        d -> d.getEmpresaId() + ":" + d.getTipoDocumento().getId(),
                        Documento::getVersao,
                        Math::max));

        for (Documento d : todosDocs) {
            StatusValidade novoStatus = ValidadeCalculator.classificar(d.getDataValidade(), hoje);
            if (d.getStatusValidade() != novoStatus) {
                d.setStatusValidade(novoStatus);
                documentosAtualizados++;
            }
            String chave = d.getEmpresaId() + ":" + d.getTipoDocumento().getId();
            boolean vigente = d.getVersao().equals(maiorVersaoPorEmpresaTipo.get(chave));
            if (!vigente) {
                continue;
            }
            Integer marco = ValidadeCalculator.marcoAlertaHoje(d.getDataValidade(), hoje);
            if (marco != null) {
                long dias = ValidadeCalculator.diasParaVencer(d.getDataValidade(), hoje);
                String sigla = d.getTipoDocumento().getSigla();
                String titulo = marco == 0
                        ? "URGENTE - Documento SST vencido: " + sigla + " - " + d.getTitulo()
                        : "Alerta (" + marco + " dias) - Documento SST próximo do vencimento: " + sigla;
                String msg = marco == 0
                        ? "O documento " + sigla + " (" + d.getTitulo() + ", v" + d.getVersao()
                                + ") venceu em " + d.getDataValidade() + " (" + (-dias) + " dia(s) em atraso)."
                        : "O documento " + sigla + " (" + d.getTitulo() + ", v" + d.getVersao()
                                + ") vencerá em " + d.getDataValidade() + " (faltam " + marco + " dias).";

                List<Usuario> responsaveis = usuarioRepository.findByEmpresaIdAndPerfilInAndAtivoTrue(
                        d.getEmpresaId(), Set.of(Perfil.RH_EMPRESA, Perfil.ESPECIALISTA_SST));
                for (Usuario u : responsaveis) {
                    if (notificacaoService.registrarAlertaDocumento(u, d, marco, hoje, titulo, msg)) {
                        notificacoesCriadas++;
                    }
                }
            }
        }

        return new ResultadoMonitoramentoResponse(
                examesAtualizados, documentosAtualizados, notificacoesCriadas, notificacoesCriadas);
    }
}
