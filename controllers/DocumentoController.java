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

import br.com.saluris.security.Perfis;
import br.com.saluris.service.ArquivoStorageService;
import br.com.saluris.service.DocumentoService;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping
    @PreAuthorize(Perfis.CONFORMIDADE)
    public List<DocumentoService.DocumentoResponse> listar(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(defaultValue = "false") boolean somenteVigentes) {
        return documentoService.listar(empresaId, somenteVigentes);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Perfis.ESPECIALISTA_OU_RH)
    public DocumentoService.DocumentoResponse criar(
            @RequestParam Long tipoDocumentoId,
            @RequestParam String titulo,
            @RequestParam(required = false) String descricao,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataEmissao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataValidade,
            @RequestParam(required = false) Long documentoBaseId,
            @RequestParam("arquivo") MultipartFile arquivo) {
        return documentoService.criar(tipoDocumentoId, titulo, descricao, dataEmissao, dataValidade,
                documentoBaseId, arquivo);
    }

    @GetMapping("/{id}/arquivo")
    @PreAuthorize(Perfis.CONFORMIDADE)
    public ResponseEntity<byte[]> baixarArquivo(@PathVariable Long id) {
        ArquivoStorageService.PdfCarregado pdf = documentoService.baixarArquivo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(pdf.nomeOriginal()).build().toString())
                .body(pdf.bytes());
    }
}
