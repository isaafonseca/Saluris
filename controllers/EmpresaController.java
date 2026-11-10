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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.saluris.security.Perfis;
import br.com.saluris.service.EmpresaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    public record AtivaRequest(@NotNull Boolean ativa) {
    }

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public List<EmpresaService.EmpresaResponse> listar() {
        return empresaService.listar();
    }

    @GetMapping("/{id}")
    public EmpresaService.EmpresaResponse buscar(@PathVariable Long id) {
        return empresaService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Perfis.ADMIN)
    public EmpresaService.EmpresaResponse criar(@Valid @RequestBody EmpresaService.EmpresaForm form) {
        return empresaService.criar(form);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Perfis.ADMIN)
    public EmpresaService.EmpresaResponse atualizar(@PathVariable Long id,
            @Valid @RequestBody EmpresaService.EmpresaForm form) {
        return empresaService.atualizar(id, form);
    }

    @PatchMapping("/{id}/ativa")
    @PreAuthorize(Perfis.ADMIN)
    public EmpresaService.EmpresaResponse alterarAtiva(@PathVariable Long id,
            @Valid @RequestBody AtivaRequest req) {
        return empresaService.alterarAtiva(id, req.ativa());
    }
}
