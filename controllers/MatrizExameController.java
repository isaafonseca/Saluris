package br.com.saluris.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import br.com.saluris.service.MatrizExameService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/matriz-exames")
public class MatrizExameController {

    private final MatrizExameService matrizExameService;

    public MatrizExameController(MatrizExameService matrizExameService) {
        this.matrizExameService = matrizExameService;
    }

    @GetMapping
    @PreAuthorize(Perfis.CONFORMIDADE)
    public List<MatrizExameService.MatrizExameResponse> listar(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) Long funcaoId,
            @RequestParam(required = false) Long setorId) {
        return matrizExameService.listar(empresaId, funcaoId, setorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Perfis.ESPECIALISTA)
    public MatrizExameService.ResultadoMatrizResponse criar(
            @Valid @RequestBody MatrizExameService.MatrizCreateRequest req) {
        return matrizExameService.criar(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Perfis.ESPECIALISTA)
    public MatrizExameService.ResultadoMatrizResponse atualizar(@PathVariable Long id,
            @Valid @RequestBody MatrizExameService.MatrizUpdateRequest req) {
        return matrizExameService.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Perfis.ESPECIALISTA)
    public void excluir(@PathVariable Long id) {
        matrizExameService.excluir(id);
    }
}
