package br.com.saluris.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import br.com.saluris.domain.entity.LogAuditoria;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.repository.LogAuditoriaRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.security.UsuarioPrincipal;
import br.com.saluris.shared.dto.PageResponse;
import br.com.saluris.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Registro e consulta de trilha de auditoria (RNF04 / RF17).
 */
@Service
public class AuditoriaService {

    public record LogAuditoriaResponse(
            Long id,
            Long empresaId,
            Long usuarioId,
            String usuarioNome,
            String acao,
            String entidade,
            Long entidadeId,
            Map<String, Object> detalhes,
            String ipOrigem,
            java.time.OffsetDateTime criadoEm) {
    }

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(LogAuditoriaRepository logAuditoriaRepository, UsuarioRepository usuarioRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrar(String acao, String entidade, Long entidadeId, Long empresaId, Map<String, Object> detalhes) {
        UsuarioPrincipal atual = TenantContext.usuarioAtual().orElse(null);
        LogAuditoria log = new LogAuditoria();
        log.setAcao(acao);
        log.setEntidade(entidade);
        log.setEntidadeId(entidadeId);
        log.setEmpresaId(empresaId != null ? empresaId : (atual != null ? atual.getEmpresaId() : null));
        log.setUsuarioId(atual != null ? atual.getId() : null);
        log.setDetalhes(detalhes);
        log.setIpOrigem(obterIpAtual());
        logAuditoriaRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PageResponse<LogAuditoriaResponse> listar(Long empresaId, Pageable pageable) {
        Page<LogAuditoria> page = empresaId != null
                ? logAuditoriaRepository.findByEmpresaIdOrderByCriadoEmDesc(empresaId, pageable)
                : logAuditoriaRepository.findAllByOrderByCriadoEmDesc(pageable);
        return PageResponse.de(page, l -> {
            String usuarioNome = l.getUsuarioId() == null ? null
                    : usuarioRepository.findById(l.getUsuarioId()).map(Usuario::getNome).orElse(null);
            return new LogAuditoriaResponse(l.getId(), l.getEmpresaId(), l.getUsuarioId(), usuarioNome,
                    l.getAcao(), l.getEntidade(), l.getEntidadeId(), l.getDetalhes(), l.getIpOrigem(),
                    l.getCriadoEm());
        });
    }

    private static String obterIpAtual() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        }
        return null;
    }
}
