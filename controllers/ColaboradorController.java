package br.com.saluris.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.saluris.security.Perfis;
import br.com.saluris.service.ColaboradorService;
import br.com.saluris.shared.dto.PageResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/colaboradores")
public class ColaboradorController {

    private final ColaboradorService colaboradorService;

    public ColaboradorController(ColaboradorService colaboradorService) {
        this.colaboradorService = colaboradorService;
    }

    @GetMapping
    @PreAuthorize(Perfis.CONFORMIDADE)
    public PageResponse<ColaboradorService.ColaboradorResponse> listar(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return colaboradorService.listar(empresaId, busca,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("nome").ascending()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Perfis.CONFORMIDADE)
    public ColaboradorService.ColaboradorDetalheResponse detalhar(@PathVariable Long id) {
        return colaboradorService.detalhar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Perfis.RH)
    public ColaboradorService.ResultadoColaboradorResponse criar(
            @Valid @RequestBody ColaboradorService.ColaboradorCreateRequest req) {
        return colaboradorService.criar(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Perfis.RH)
    public ColaboradorService.ResultadoColaboradorResponse atualizar(@PathVariable Long id,
            @Valid @RequestBody ColaboradorService.ColaboradorUpdateRequest req) {
        return colaboradorService.atualizar(id, req);
    }
}
