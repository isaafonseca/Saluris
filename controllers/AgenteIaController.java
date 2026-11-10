package br.com.saluris.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.saluris.security.Perfis;
import br.com.saluris.service.AgenteIaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ia")
@PreAuthorize(Perfis.ESPECIALISTA_OU_RH)
public class AgenteIaController {

    private final AgenteIaService agenteIaService;

    public AgenteIaController(AgenteIaService agenteIaService) {
        this.agenteIaService = agenteIaService;
    }

    @GetMapping("/status")
    public AgenteIaService.StatusIaResponse status() {
        return agenteIaService.obterStatus();
    }

    @GetMapping("/conversas")
    public List<AgenteIaService.ConversaResumoResponse> listarConversas() {
        return agenteIaService.listarConversas();
    }

    @GetMapping("/conversas/{id}/mensagens")
    public List<AgenteIaService.MensagemResponse> listarMensagens(@PathVariable Long id) {
        return agenteIaService.listarMensagens(id);
    }

    @PostMapping("/perguntar")
    public AgenteIaService.RespostaIaResponse perguntar(
            @Valid @RequestBody AgenteIaService.PerguntaRequest req) {
        return agenteIaService.perguntar(req);
    }

    @DeleteMapping("/conversas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirConversa(@PathVariable Long id) {
        agenteIaService.excluirConversa(id);
    }
}
