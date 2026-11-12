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

import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.security.Perfis;
import br.com.saluris.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize(Perfis.GERENCIAR_USUARIOS)
public class UsuarioController {

    public record AtivoRequest(@NotNull Boolean ativo) {
    }

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioService.UsuarioResponse> listar(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) Perfil perfil) {
        return usuarioService.listar(empresaId, perfil);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioService.UsuarioCriadoResponse criar(
            @Valid @RequestBody UsuarioService.UsuarioCreateRequest req) {
        return usuarioService.criar(req);
    }

    @PutMapping("/{id}")
    public UsuarioService.UsuarioResponse atualizar(@PathVariable Long id,
            @Valid @RequestBody UsuarioService.UsuarioUpdateRequest req) {
        return usuarioService.atualizar(id, req);
    }

    @PatchMapping("/{id}/ativo")
    public UsuarioService.UsuarioResponse alterarAtivo(@PathVariable Long id,
            @Valid @RequestBody AtivoRequest req) {
        return usuarioService.alterarAtivo(id, req.ativo());
    }

    @PostMapping("/{id}/redefinir-senha")
    public UsuarioService.SenhaTemporariaResponse redefinirSenha(@PathVariable Long id) {
        return usuarioService.redefinirSenha(id);
    }
}
