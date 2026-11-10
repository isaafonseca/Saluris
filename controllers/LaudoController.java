package br.com.saluris.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.saluris.domain.enums.StatusLaudo;
import br.com.saluris.security.Perfis;
import br.com.saluris.service.ArquivoStorageService;
import br.com.saluris.service.LaudoService;

@RestController
@RequestMapping("/api/laudos")
public class LaudoController {

    private final LaudoService laudoService;

    public LaudoController(LaudoService laudoService) {
        this.laudoService = laudoService;
    }

    @GetMapping
    @PreAuthorize(Perfis.AUTENTICADO)
    public List<LaudoService.LaudoResponse> listar(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) StatusLaudo status) {
        return laudoService.listar(empresaId, status);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Perfis.ESPECIALISTA)
    public LaudoService.LaudoResponse criar(
            @RequestParam Long tipoLaudoId,
            @RequestParam String titulo,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataEmissao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataValidade,
            @RequestParam(required = false) Long colaboradorId,
            @RequestParam("arquivo") MultipartFile arquivo) {
        return laudoService.criar(tipoLaudoId, titulo, descricao, dataEmissao, dataValidade, colaboradorId, arquivo);
    }

    @PostMapping(value = "/{id}/reenviar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Perfis.ESPECIALISTA)
    public LaudoService.LaudoResponse reenviar(@PathVariable Long id,
            @RequestParam("arquivo") MultipartFile arquivo) {
        return laudoService.reenviar(id, arquivo);
    }

    @GetMapping("/{id}/arquivo")
    @PreAuthorize(Perfis.AUTENTICADO)
    public ResponseEntity<byte[]> baixarArquivo(@PathVariable Long id) {
        ArquivoStorageService.PdfCarregado pdf = laudoService.baixarArquivo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(pdf.nomeOriginal()).build().toString())
                .body(pdf.bytes());
    }
}
