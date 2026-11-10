package br.com.saluris.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.saluris.security.Perfis;
import br.com.saluris.service.AssinaturaService;
import br.com.saluris.service.ExameService;
import br.com.saluris.service.LaudoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/assinaturas")
@PreAuthorize(Perfis.MEDICO)
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    public AssinaturaController(AssinaturaService assinaturaService) {
        this.assinaturaService = assinaturaService;
    }

    @GetMapping("/pendentes")
    public AssinaturaService.FilaAssinaturasResponse listarPendentes() {
        return assinaturaService.listarPendentes();
    }

    @PostMapping("/laudos/{id}/assinar")
    public LaudoService.LaudoResponse assinarLaudo(@PathVariable Long id) {
        return assinaturaService.assinarLaudo(id);
    }

    @PostMapping("/laudos/{id}/recusar")
    public LaudoService.LaudoResponse recusarLaudo(@PathVariable Long id,
            @Valid @RequestBody AssinaturaService.RecusaRequest req) {
        return assinaturaService.recusarLaudo(id, req);
    }

    @PostMapping("/exames/{id}/assinar")
    public ExameService.ExameResponse assinarExame(@PathVariable Long id) {
        return assinaturaService.assinarExame(id);
    }

    @PostMapping("/exames/{id}/recusar")
    public ExameService.ExameResponse recusarExame(@PathVariable Long id,
            @Valid @RequestBody AssinaturaService.RecusaRequest req) {
        return assinaturaService.recusarExame(id, req);
    }
}
