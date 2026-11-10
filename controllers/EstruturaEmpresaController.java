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
import br.com.saluris.service.EstruturaEmpresaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api")
@PreAuthorize(Perfis.CONFORMIDADE)
public class EstruturaEmpresaController {

    public record AtivoRequest(@NotNull Boolean ativo) {
    }

    private final EstruturaEmpresaService service;

    public EstruturaEmpresaController(EstruturaEmpresaService service) {
        this.service = service;
    }

    // --- Unidades ---

    @GetMapping("/unidades")
    public List<EstruturaEmpresaService.UnidadeResponse> listarUnidades(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(defaultValue = "false") boolean ativos) {
        return service.listarUnidades(empresaId, ativos);
    }

    @PostMapping("/unidades")
    @ResponseStatus(HttpStatus.CREATED)
    public EstruturaEmpresaService.UnidadeResponse criarUnidade(
            @RequestParam(required = false) Long empresaId,
            @Valid @RequestBody EstruturaEmpresaService.UnidadeForm form) {
        return service.criarUnidade(empresaId, form);
    }

    @PutMapping("/unidades/{id}")
    public EstruturaEmpresaService.UnidadeResponse atualizarUnidade(@PathVariable Long id,
            @Valid @RequestBody EstruturaEmpresaService.UnidadeForm form) {
        return service.atualizarUnidade(id, form);
    }

    @PatchMapping("/unidades/{id}/ativo")
    public EstruturaEmpresaService.UnidadeResponse alterarAtivoUnidade(@PathVariable Long id,
            @Valid @RequestBody AtivoRequest req) {
        return service.alterarAtivoUnidade(id, req.ativo());
    }

    // --- Funções ---

    @GetMapping("/funcoes")
    public List<EstruturaEmpresaService.FuncaoResponse> listarFuncoes(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(defaultValue = "false") boolean ativos) {
        return service.listarFuncoes(empresaId, ativos);
    }

    @PostMapping("/funcoes")
    @ResponseStatus(HttpStatus.CREATED)
    public EstruturaEmpresaService.FuncaoResponse criarFuncao(
            @RequestParam(required = false) Long empresaId,
            @Valid @RequestBody EstruturaEmpresaService.FuncaoForm form) {
        return service.criarFuncao(empresaId, form);
    }

    @PutMapping("/funcoes/{id}")
    public EstruturaEmpresaService.FuncaoResponse atualizarFuncao(@PathVariable Long id,
            @Valid @RequestBody EstruturaEmpresaService.FuncaoForm form) {
        return service.atualizarFuncao(id, form);
    }

    @PatchMapping("/funcoes/{id}/ativo")
    public EstruturaEmpresaService.FuncaoResponse alterarAtivoFuncao(@PathVariable Long id,
            @Valid @RequestBody AtivoRequest req) {
        return service.alterarAtivoFuncao(id, req.ativo());
    }

    // --- Setores ---

    @GetMapping("/setores")
    public List<EstruturaEmpresaService.SetorResponse> listarSetores(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(defaultValue = "false") boolean ativos) {
        return service.listarSetores(empresaId, ativos);
    }

    @PostMapping("/setores")
    @ResponseStatus(HttpStatus.CREATED)
    public EstruturaEmpresaService.SetorResponse criarSetor(
            @RequestParam(required = false) Long empresaId,
            @Valid @RequestBody EstruturaEmpresaService.SetorForm form) {
        return service.criarSetor(empresaId, form);
    }

    @PutMapping("/setores/{id}")
    public EstruturaEmpresaService.SetorResponse atualizarSetor(@PathVariable Long id,
            @Valid @RequestBody EstruturaEmpresaService.SetorForm form) {
        return service.atualizarSetor(id, form);
    }

    @PatchMapping("/setores/{id}/ativo")
    public EstruturaEmpresaService.SetorResponse alterarAtivoSetor(@PathVariable Long id,
            @Valid @RequestBody AtivoRequest req) {
        return service.alterarAtivoSetor(id, req.ativo());
    }
}
