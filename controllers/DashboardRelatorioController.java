package br.com.saluris.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.saluris.domain.enums.StatusValidade;
import br.com.saluris.security.Perfis;
import br.com.saluris.service.DashboardRelatorioService;
import br.com.saluris.service.DashboardRelatorioService.FiltroDashboardRelatorio;

@RestController
@RequestMapping("/api")
@PreAuthorize(Perfis.CONFORMIDADE)
public class DashboardRelatorioController {

    private final DashboardRelatorioService service;

    public DashboardRelatorioController(DashboardRelatorioService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public DashboardRelatorioService.DashboardResponse consultarDashboard(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long tipoExameId,
            @RequestParam(required = false) Long colaboradorId,
            @RequestParam(required = false) StatusValidade statusValidade) {
        FiltroDashboardRelatorio filtro = construirFiltro(
                empresaId, dataInicio, dataFim, tipoExameId, colaboradorId, statusValidade);
        return service.consultarDashboard(filtro);
    }

    @GetMapping("/relatorios/conformidade")
    public ResponseEntity<byte[]> gerarRelatorio(
            @RequestParam(defaultValue = "PDF") String formato,
            @RequestParam(defaultValue = "AMBOS") String tipo,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long tipoExameId,
            @RequestParam(required = false) Long colaboradorId,
            @RequestParam(required = false) StatusValidade statusValidade) {
        FiltroDashboardRelatorio filtro = construirFiltro(
                empresaId, dataInicio, dataFim, tipoExameId, colaboradorId, statusValidade);
        var rel = service.gerarRelatorio(formato, tipo, filtro);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(rel.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(rel.nomeArquivo()).build().toString())
                .body(rel.bytes());
    }

    /**
     * Normaliza e valida os parâmetros de filtro recebidos da View para o Dashboard e Relatórios.
     * Caso o usuário informe dataInicio posterior a dataFim, inverte o intervalo automaticamente.
     */
    private FiltroDashboardRelatorio construirFiltro(
            Long empresaId,
            LocalDate dataInicio,
            LocalDate dataFim,
            Long tipoExameId,
            Long colaboradorId,
            StatusValidade statusValidade) {
        LocalDate inicio = dataInicio;
        LocalDate fim = dataFim;
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            LocalDate temp = inicio;
            inicio = fim;
            fim = temp;
        }
        Long tipoIdValido = (tipoExameId != null && tipoExameId > 0) ? tipoExameId : null;
        Long colabIdValido = (colaboradorId != null && colaboradorId > 0) ? colaboradorId : null;
        return new FiltroDashboardRelatorio(
                empresaId,
                inicio,
                fim,
                tipoIdValido,
                colabIdValido,
                statusValidade);
    }
}
