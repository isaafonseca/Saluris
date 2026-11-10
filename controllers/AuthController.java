package br.com.saluris.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.saluris.auth.dto.LoginRequest;
import br.com.saluris.auth.dto.LoginResponse;
import br.com.saluris.auth.dto.TrocarSenhaRequest;
import br.com.saluris.auth.dto.UsuarioLogadoResponse;
import br.com.saluris.security.UsuarioPrincipal;
import br.com.saluris.service.AuthService;
import jakarta.validation.Valid;

/** Autenticação mínima necessária para o RBAC; os demais controladores vêm na Fase 4. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UsuarioLogadoResponse me(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return authService.usuarioLogado(principal);
    }

    @PostMapping("/trocar-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody TrocarSenhaRequest request) {
        authService.trocarSenha(principal, request);
    }
}
