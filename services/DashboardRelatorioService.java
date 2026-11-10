package br.com.saluris.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.Documento;
import br.com.saluris.domain.entity.Empresa;
import br.com.saluris.domain.entity.Exame;
import br.com.saluris.domain.entity.Laudo;
import br.com.saluris.domain.enums.StatusFluxoExame;
import br.com.saluris.domain.enums.StatusLaudo;
import br.com.saluris.domain.enums.StatusValidade;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.exception.RegraNegocioException;
import br.com.saluris.repository.DocumentoRepository;
import br.com.saluris.repository.EmpresaRepository;
import br.com.saluris.repository.ExameRepository;
import br.com.saluris.repository.LaudoRepository;
import br.com.saluris.shared.util.ValidadeCalculator;
import br.com.saluris.tenant.TenantContext;

/**
 * Dashboard de conformidade e geração de relatórios em PDF e CSV (Quadros 09, 10 / RF14-RF17).
 */
@Service
public class DashboardRelatorioService {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record FiltroDashboardRelatorio(
            Long empresaId,
            LocalDate dataInicio,
            LocalDate dataFim,
            Long tipoExameId,
            Long colaboradorId,
            StatusValidade statusValidade) {

        public boolean temFiltroAtivo() {
            return dataInicio != null || dataFim != null || tipoExameId != null
                    || colaboradorId != null || statusValidade != null;
        }
    }

    public record ResumoExames(long total, long emDia, long proximoVencimento, long vencido, double percentualConformidade) {
    }

    public record ResumoDocumentos(long total, long emDia, long proximoVencimento, long vencido) {
    }

    public record ResumoLaudos(long aguardandoAssinatura, long homologados, long revisadoPendente) {
    }

    public record DashboardResponse(
            ResumoExames exames,
            ResumoDocumentos documentos,
            ResumoLaudos laudos,
            List<DocumentoService.DocumentoResponse> documentosVigentes,
            List<ExameService.ExameResponse> examesCriticos) {
    }

    public record RelatorioGerado(String nomeArquivo, String contentType, byte[] bytes) {
    }

    private final ExameRepository exameRepository;
    private final DocumentoRepository documentoRepository;
    private final LaudoRepository laudoRepository;
    private final EmpresaRepository empresaRepository;
    private final AuditoriaService auditoriaService;

    public DashboardRelatorioService(ExameRepository exameRepository,
            DocumentoRepository documentoRepository,
            LaudoRepository laudoRepository,
            EmpresaRepository empresaRepository,
            AuditoriaService auditoriaService) {
        this.exameRepository = exameRepository;
        this.documentoRepository = documentoRepository;
        this.laudoRepository = laudoRepository;
        this.empresaRepository = empresaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse consultarDashboard(Long empresaIdParam, LocalDate dataInicio, LocalDate dataFim,
            Long tipoExameId, Long colaboradorId) {
        return consultarDashboard(new FiltroDashboardRelatorio(
                empresaIdParam, dataInicio, dataFim, tipoExameId, colaboradorId, null));
    }

    @Transactional(readOnly = true)
    public DashboardResponse consultarDashboard(FiltroDashboardRelatorio filtro) {
        Long empresaId = TenantContext.resolverEmpresaIdOpcional(filtro.empresaId());
        LocalDate hoje = LocalDate.now();

        List<Exame> examesFiltrados = filtrarExames(
                empresaId,
                filtro.dataInicio(),
                filtro.dataFim(),
                filtro.tipoExameId(),
                filtro.colaboradorId(),
                filtro.statusValidade(),
                hoje);
        long exTotal = examesFiltrados.size();
        long exEmDia = examesFiltrados.stream()
                .filter(e -> ValidadeCalculator.classificar(e.getDataVencimento(), hoje) == StatusValidade.EM_DIA).count();
        long exProximo = examesFiltrados.stream()
                .filter(e -> ValidadeCalculator.classificar(e.getDataVencimento(), hoje) == StatusValidade.PROXIMO_VENCIMENTO).count();
        long exVencido = examesFiltrados.stream()
                .filter(e -> ValidadeCalculator.classificar(e.getDataVencimento(), hoje) == StatusValidade.VENCIDO).count();
        double percentual = exTotal == 0
                ? (filtro.temFiltroAtivo() ? 0.0 : 100.0)
                : Math.round(((double) (exEmDia + exProximo) / exTotal) * 1000.0) / 10.0;

        List<ExameService.ExameResponse> criticos = examesFiltrados.stream()
                .filter(e -> ValidadeCalculator.classificar(e.getDataVencimento(), hoje) != StatusValidade.EM_DIA)
                .limit(50)
                .map(e -> ExameService.paraResposta(e, hoje))
                .toList();

        List<DocumentoService.DocumentoResponse> docsVigentes = listarDocumentosVigentes(
                empresaId, filtro.dataInicio(), filtro.dataFim(), filtro.statusValidade(), hoje);
        long docTotal = docsVigentes.size();
        long docEmDia = docsVigentes.stream().filter(d -> d.statusValidade() == StatusValidade.EM_DIA).count();
        long docProximo = docsVigentes.stream().filter(d -> d.statusValidade() == StatusValidade.PROXIMO_VENCIMENTO).count();
        long docVencido = docsVigentes.stream().filter(d -> d.statusValidade() == StatusValidade.VENCIDO).count();

        List<Laudo> laudosEmpresa = filtrarLaudos(
                empresaId, filtro.dataInicio(), filtro.dataFim(), filtro.tipoExameId(), filtro.colaboradorId());
        long lAguardando = laudosEmpresa.stream().filter(l -> l.getStatus() == StatusLaudo.AGUARDANDO_ASSINATURA).count();
        long lHomologados = laudosEmpresa.stream().filter(l -> l.getStatus() == StatusLaudo.HOMOLOGADO).count();
        long lRevisado = laudosEmpresa.stream().filter(l -> l.getStatus() == StatusLaudo.REVISADO_PENDENTE).count();

        return new DashboardResponse(
                new ResumoExames(exTotal, exEmDia, exProximo, exVencido, percentual),
                new ResumoDocumentos(docTotal, docEmDia, docProximo, docVencido),
                new ResumoLaudos(lAguardando, lHomologados, lRevisado),
                docsVigentes,
                criticos);
    }

    @Transactional
    public RelatorioGerado gerarRelatorio(String formato, String tipo, Long empresaIdParam,
            LocalDate dataInicio, LocalDate dataFim) {
        return gerarRelatorio(formato, tipo, new FiltroDashboardRelatorio(
                empresaIdParam, dataInicio, dataFim, null, null, null));
    }

    @Transactional
    public RelatorioGerado gerarRelatorio(String formato, String tipo, FiltroDashboardRelatorio filtro) {
        Long empresaId = TenantContext.resolverEmpresaIdOpcional(filtro.empresaId());
        String fmt = formato == null ? "PDF" : formato.trim().toUpperCase();
        String tp = tipo == null ? "AMBOS" : tipo.trim().toUpperCase();
        if (!fmt.equals("PDF") && !fmt.equals("CSV")) {
            throw new RegraNegocioException("Formato inválido. Utilize PDF ou CSV.");
        }
        if (!tp.equals("EXAMES") && !tp.equals("DOCUMENTOS") && !tp.equals("AMBOS")) {
            throw new RegraNegocioException("Tipo de relatório inválido. Utilize EXAMES, DOCUMENTOS ou AMBOS.");
        }

        LocalDate hoje = LocalDate.now();
        List<Exame> exames = (tp.equals("EXAMES") || tp.equals("AMBOS"))
                ? filtrarExames(
                        empresaId,
                        filtro.dataInicio(),
                        filtro.dataFim(),
                        filtro.tipoExameId(),
                        filtro.colaboradorId(),
                        filtro.statusValidade(),
                        hoje)
                : List.of();
        List<DocumentoService.DocumentoResponse> docs = (tp.equals("DOCUMENTOS") || tp.equals("AMBOS"))
                ? listarDocumentosVigentes(
                        empresaId,
                        filtro.dataInicio(),
                        filtro.dataFim(),
                        filtro.statusValidade(),
                        hoje)
                : List.of();

        if (exames.isEmpty() && docs.isEmpty()) {
            throw new RegraNegocioException("Não há registros encontrados para os filtros selecionados.");
        }

        Empresa empresa;
        if (empresaId != null) {
            empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada."));
        } else {
            empresa = empresaRepository.findAll().stream().findFirst().orElseGet(() -> {
                Empresa todas = new Empresa();
                todas.setRazaoSocial("Todas as Empresas Cadastradas");
                todas.setCnpj("00000000000000");
                return todas;
            });
        }

        auditoriaService.registrar("GERAR_RELATORIO", "relatorios", null, empresaId,
                Map.of("formato", fmt, "tipo", tp, "exames", exames.size(), "documentos", docs.size()));

        if (fmt.equals("CSV")) {
            byte[] csv = gerarCsv(empresa, exames, docs, hoje);
            return new RelatorioGerado("relatorio-conformidade-" + hoje + ".csv", "text/csv; charset=UTF-8", csv);
        }
        byte[] pdf = gerarPdf(empresa, exames, docs, hoje);
        return new RelatorioGerado("relatorio-conformidade-" + hoje + ".pdf", "application/pdf", pdf);
    }

    private List<Exame> filtrarExames(Long empresaId, LocalDate dataInicio, LocalDate dataFim,
            Long tipoExameId, Long colaboradorId, StatusValidade statusValidade, LocalDate hoje) {
        List<Exame> base = empresaId != null
                ? exameRepository.findByEmpresaIdAndStatusFluxoNotOrderByDataVencimentoAsc(
                        empresaId, StatusFluxoExame.HOMOLOGADO)
                : exameRepository.findByStatusFluxoNot(StatusFluxoExame.HOMOLOGADO);
        return base.stream()
                .filter(e -> e.getColaborador().isAtivo())
                .filter(e -> tipoExameId == null || e.getTipoExame().getId().equals(tipoExameId))
                .filter(e -> colaboradorId == null || e.getColaborador().getId().equals(colaboradorId))
                .filter(e -> dataInicio == null || !e.getDataVencimento().isBefore(dataInicio))
                .filter(e -> dataFim == null || !e.getDataVencimento().isAfter(dataFim))
                .filter(e -> statusValidade == null
                        || ValidadeCalculator.classificar(e.getDataVencimento(), hoje) == statusValidade)
                .toList();
    }

    private List<Laudo> filtrarLaudos(Long empresaId, LocalDate dataInicio, LocalDate dataFim,
            Long tipoExameId, Long colaboradorId) {
        List<Laudo> base = empresaId != null
                ? laudoRepository.findByEmpresaIdOrderByCriadoEmDesc(empresaId)
                : laudoRepository.findAll();
        return base.stream()
                .filter(l -> colaboradorId == null
                        || (l.getColaborador() != null && l.getColaborador().getId().equals(colaboradorId)))
                .filter(l -> tipoExameId == null
                        || (l.getExame() != null && l.getExame().getTipoExame().getId().equals(tipoExameId)))
                .filter(l -> {
                    if (dataInicio == null) {
                        return true;
                    }
                    LocalDate ref = l.getDataValidade() != null ? l.getDataValidade() : l.getDataEmissao();
                    return ref == null || !ref.isBefore(dataInicio);
                })
                .filter(l -> {
                    if (dataFim == null) {
                        return true;
                    }
                    LocalDate ref = l.getDataValidade() != null ? l.getDataValidade() : l.getDataEmissao();
                    return ref == null || !ref.isAfter(dataFim);
                })
                .toList();
    }

    private List<DocumentoService.DocumentoResponse> listarDocumentosVigentes(Long empresaId,
            LocalDate dataInicio, LocalDate dataFim, StatusValidade statusValidade, LocalDate hoje) {
        List<Documento> todos = empresaId != null
                ? documentoRepository.findByEmpresaIdOrderByDataValidadeAsc(empresaId)
                : documentoRepository.findAll();
        Map<Long, Integer> maiorVersao = DocumentoService.calcularMaiorVersaoPorTipo(todos);
        return todos.stream()
                .filter(d -> d.getVersao().equals(maiorVersao.get(d.getTipoDocumento().getId())))
                .filter(d -> dataInicio == null || !d.getDataValidade().isBefore(dataInicio))
                .filter(d -> dataFim == null || !d.getDataValidade().isAfter(dataFim))
                .filter(d -> statusValidade == null
                        || ValidadeCalculator.classificar(d.getDataValidade(), hoje) == statusValidade)
                .map(d -> DocumentoService.paraResposta(d, hoje, true))
                .toList();
    }

    private static byte[] gerarCsv(Empresa empresa, List<Exame> exames,
            List<DocumentoService.DocumentoResponse> docs, LocalDate hoje) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // BOM UTF-8 para abrir corretamente no Excel
        sb.append("Categoria;Identificacao;Tipo;Setor_Versao;Vencimento;Dias_Para_Vencer;Status_Validade;Status_Fluxo\n");
        for (Exame e : exames) {
            StatusValidade st = ValidadeCalculator.classificar(e.getDataVencimento(), hoje);
            long dias = ValidadeCalculator.diasParaVencer(e.getDataVencimento(), hoje);
            sb.append("EXAME;")
                    .append(escaparCsv(e.getColaborador().getNome())).append(';')
                    .append(escaparCsv(e.getTipoExame().getNome())).append(';')
                    .append(escaparCsv(e.getColaborador().getFuncao().getNome() + " / " + e.getColaborador().getSetor().getNome())).append(';')
                    .append(e.getDataVencimento().format(DATA_BR)).append(';')
                    .append(dias).append(';')
                    .append(st.getRotulo()).append(';')
                    .append(e.getStatusFluxo().getRotulo()).append('\n');
        }
        for (var d : docs) {
            sb.append("DOCUMENTO_SST;")
                    .append(escaparCsv(d.titulo())).append(';')
                    .append(escaparCsv(d.tipoDocumentoSigla() + " - " + d.tipoDocumentoNome())).append(';')
                    .append("v").append(d.versao()).append(';')
                    .append(d.dataValidade().format(DATA_BR)).append(';')
                    .append(d.diasParaVencer()).append(';')
                    .append(d.statusValidade().getRotulo()).append(';')
                    .append("Vigente").append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escaparCsv(String s) {
        if (s == null) {
            return "";
        }
        return s.replace(";", ",").replace("\r", " ").replace("\n", " ");
    }

    private static byte[] gerarPdf(Empresa empresa, List<Exame> exames,
            List<DocumentoService.DocumentoResponse> docs, LocalDate hoje) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, new Color(15, 118, 110));
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font secaoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font cabecalhoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font celulaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

            doc.add(new Paragraph("SALURIS IA - Relatório de Conformidade de SST", tituloFont));
            doc.add(new Paragraph("Empresa: " + empresa.getRazaoSocial() + " (CNPJ: " + empresa.getCnpj()
                    + ")  |  Emitido em: " + hoje.format(DATA_BR), subFont));
            doc.add(new Paragraph(" "));

            if (!exames.isEmpty()) {
                Paragraph pEx = new Paragraph("Exames Ocupacionais em Aberto (" + exames.size() + ")", secaoFont);
                pEx.setSpacingAfter(6f);
                doc.add(pEx);

                PdfPTable t = new PdfPTable(new float[] { 3f, 2.5f, 2.5f, 1.5f, 1.5f, 1.8f });
                t.setWidthPercentage(100);
                adicionarCabecalho(t, cabecalhoFont, "Colaborador", "Função / Setor", "Exame", "Vencimento", "Situação", "Fluxo");
                for (Exame e : exames) {
                    StatusValidade st = ValidadeCalculator.classificar(e.getDataVencimento(), hoje);
                    adicionarCelula(t, celulaFont, e.getColaborador().getNome());
                    adicionarCelula(t, celulaFont, e.getColaborador().getFuncao().getNome() + " / " + e.getColaborador().getSetor().getNome());
                    adicionarCelula(t, celulaFont, e.getTipoExame().getNome());
                    adicionarCelula(t, celulaFont, e.getDataVencimento().format(DATA_BR));
                    adicionarCelula(t, celulaFont, st.getRotulo());
                    adicionarCelula(t, celulaFont, e.getStatusFluxo().getRotulo());
                }
                doc.add(t);
                doc.add(new Paragraph(" "));
            }

            if (!docs.isEmpty()) {
                Paragraph pDoc = new Paragraph("Documentos SST Vigentes (" + docs.size() + ")", secaoFont);
                pDoc.setSpacingAfter(6f);
                doc.add(pDoc);

                PdfPTable t = new PdfPTable(new float[] { 1.5f, 3.5f, 1f, 1.5f, 1.5f, 2f });
                t.setWidthPercentage(100);
                adicionarCabecalho(t, cabecalhoFont, "Sigla", "Título", "Versão", "Emissão", "Validade", "Situação");
                for (var d : docs) {
                    adicionarCelula(t, celulaFont, d.tipoDocumentoSigla());
                    adicionarCelula(t, celulaFont, d.titulo());
                    adicionarCelula(t, celulaFont, "v" + d.versao());
                    adicionarCelula(t, celulaFont, d.dataEmissao().format(DATA_BR));
                    adicionarCelula(t, celulaFont, d.dataValidade().format(DATA_BR));
                    adicionarCelula(t, celulaFont, d.statusValidade().getRotulo());
                }
                doc.add(t);
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar relatório PDF.", e);
        }
    }

    private static void adicionarCabecalho(PdfPTable t, Font font, String... colunas) {
        Color bg = new Color(15, 118, 110);
        for (String col : colunas) {
            PdfPCell c = new PdfPCell(new Phrase(col, font));
            c.setBackgroundColor(bg);
            c.setPadding(5f);
            c.setHorizontalAlignment(Element.ALIGN_LEFT);
            t.addCell(c);
        }
    }

    private static void adicionarCelula(PdfPTable t, Font font, String valor) {
        PdfPCell c = new PdfPCell(new Phrase(valor != null ? valor : "-", font));
        c.setPadding(4f);
        t.addCell(c);
    }
}
