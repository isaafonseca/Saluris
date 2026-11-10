package br.com.saluris.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.saluris.security.Perfis;
import br.com.saluris.service.CatalogoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api")
public class CatalogoController {

    public record AtivoRequest(@NotNull Boolean ativo) {
    }

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    // --- Tipos de Exame ---

    @GetMapping("/tipos-exame")
    public List<CatalogoService.TipoExameResponse> listarTiposExame(
            @RequestParam(defaultValue = "false") boolean ativos) {
        return catalogoService.listarTiposExame(ativos);
    }

    @PostMapping("/tipos-exame")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Perfis.ADMIN)
    public CatalogoService.TipoExameResponse criarTipoExame(
            @Valid @RequestBody CatalogoService.TipoExameForm form) {
        return catalogoService.criarTipoExame(form);
    }

    @PutMapping("/tipos-exame/{id}")
    @PreAuthorize(Perfis.ADMIN)
    public CatalogoService.TipoExameResponse atualizarTipoExame(@PathVariable Long id,
            @Valid @RequestBody CatalogoService.TipoExameForm form) {
        return catalogoService.atualizarTipoExame(id, form);
    }

    @PatchMapping("/tipos-exame/{id}/ativo")
    @PreAuthorize(Perfis.ADMIN)
    public CatalogoService.TipoExameResponse alterarAtivoTipoExame(@PathVariable Long id,
            @Valid @RequestBody AtivoRequest req) {
        return catalogoService.alterarAtivoTipoExame(id, req.ativo());
    }

    // --- Tipos de Documento ---

    @GetMapping("/tipos-documento")
    public List<CatalogoService.TipoDocumentoResponse> listarTiposDocumento(
            @RequestParam(defaultValue = "false") boolean ativos) {
        return catalogoService.listarTiposDocumento(ativos);
    }

    @PostMapping("/tipos-documento")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Perfis.ADMIN)
    public CatalogoService.TipoDocumentoResponse criarTipoDocumento(
            @Valid @RequestBody CatalogoService.TipoDocumentoForm form) {
        return catalogoService.criarTipoDocumento(form);
    }

    @PutMapping("/tipos-documento/{id}")
    @PreAuthorize(Perfis.ADMIN)
    public CatalogoService.TipoDocumentoResponse atualizarTipoDocumento(@PathVariable Long id,
            @Valid @RequestBody CatalogoService.TipoDocumentoForm form) {
        return catalogoService.atualizarTipoDocumento(id, form);
    }

    @PatchMapping("/tipos-documento/{id}/ativo")
    @PreAuthorize(Perfis.ADMIN)
    public CatalogoService.TipoDocumentoResponse alterarAtivoTipoDocumento(@PathVariable Long id,
            @Valid @RequestBody AtivoRequest req) {
        return catalogoService.alterarAtivoTipoDocumento(id, req.ativo());
    }

    // --- Tipos de Laudo ---

    @GetMapping("/tipos-laudo")
    public List<CatalogoService.TipoLaudoResponse> listarTiposLaudo(
            @RequestParam(defaultValue = "false") boolean ativos) {
        return catalogoService.listarTiposLaudo(ativos);
    }

    @PostMapping("/tipos-laudo")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Perfis.ADMIN)
    public CatalogoService.TipoLaudoResponse criarTipoLaudo(
            @Valid @RequestBody CatalogoService.TipoLaudoForm form) {
        return catalogoService.criarTipoLaudo(form);
    }

    @PutMapping("/tipos-laudo/{id}")
    @PreAuthorize(Perfis.ADMIN)
    public CatalogoService.TipoLaudoResponse atualizarTipoLaudo(@PathVariable Long id,
            @Valid @RequestBody CatalogoService.TipoLaudoForm form) {
        return catalogoService.atualizarTipoLaudo(id, form);
    }

    @PatchMapping("/tipos-laudo/{id}/ativo")
    @PreAuthorize(Perfis.ADMIN)
    public CatalogoService.TipoLaudoResponse alterarAtivoTipoLaudo(@PathVariable Long id,
            @Valid @RequestBody AtivoRequest req) {
        return catalogoService.alterarAtivoTipoLaudo(id, req.ativo());
    }
}
