package br.com.saluris.controller;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.saluris.domain.enums.StatusFluxoExame;
import br.com.saluris.domain.enums.StatusValidade;
import br.com.saluris.security.Perfis;
import br.com.saluris.service.ArquivoStorageService;
import br.com.saluris.service.ExameService;
import br.com.saluris.shared.dto.PageResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exames")
public class ExameController {

    private final ExameService exameService;

    public ExameController(ExameService exameService) {
        this.exameService = exameService;
    }

    @GetMapping
    @PreAuthorize(Perfis.CONFORMIDADE)
    public PageResponse<ExameService.ExameResponse> listar(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) Long colaboradorId,
            @RequestParam(required = false) Long tipoExameId,
            @RequestParam(required = false) StatusValidade statusValidade,
            @RequestParam(required = false) StatusFluxoExame statusFluxo,
            @RequestParam(defaultValue = "false") boolean incluirHistorico,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return exameService.listar(empresaId, colaboradorId, tipoExameId, statusValidade, statusFluxo,
                incluirHistorico,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("dataVencimento").ascending()));
    }

    @PostMapping("/{id}/agendar")
    @PreAuthorize(Perfis.RH)
    public ExameService.ExameResponse agendar(@PathVariable Long id,
            @Valid @RequestBody ExameService.AgendarExameRequest req) {
        return exameService.agendar(id, req);
    }

    @PostMapping(value = "/{id}/resultado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Perfis.RH)
    public ExameService.ExameResponse registrarResultado(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataRealizacao,
            @RequestParam("arquivo") MultipartFile arquivo) {
        return exameService.registrarResultado(id, dataRealizacao, arquivo);
    }

    @GetMapping("/{id}/arquivo")
    @PreAuthorize(Perfis.OPERACIONAL)
    public ResponseEntity<byte[]> baixarArquivo(@PathVariable Long id) {
        ArquivoStorageService.PdfCarregado pdf = exameService.baixarArquivo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(pdf.nomeOriginal()).build().toString())
                .body(pdf.bytes());
    }
}
