package br.com.saluris.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.saluris.security.Perfis;
import br.com.saluris.service.AuditoriaService;
import br.com.saluris.service.MonitoramentoService;
import br.com.saluris.shared.dto.PageResponse;

@RestController
@RequestMapping("/api")
@PreAuthorize(Perfis.ADMIN)
public class AdminController {

    private final AuditoriaService auditoriaService;
    private final MonitoramentoService monitoramentoService;

    public AdminController(AuditoriaService auditoriaService, MonitoramentoService monitoramentoService) {
        this.auditoriaService = auditoriaService;
        this.monitoramentoService = monitoramentoService;
    }

    @GetMapping("/auditoria")
    @PreAuthorize("denyAll()")
    public PageResponse<AuditoriaService.LogAuditoriaResponse> listarAuditoria(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditoriaService.listar(empresaId,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
    }

    @PostMapping("/admin/monitoramento/executar")
    public MonitoramentoService.ResultadoMonitoramentoResponse executarMonitoramento() {
        return monitoramentoService.executarAgora();
    }
}
