package br.com.saluris.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.Colaborador;
import br.com.saluris.domain.entity.Documento;
import br.com.saluris.domain.entity.Exame;
import br.com.saluris.domain.entity.Laudo;
import br.com.saluris.domain.entity.Notificacao;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.domain.enums.TipoNotificacao;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.repository.NotificacaoRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.shared.dto.PageResponse;
import br.com.saluris.tenant.TenantContext;

/**
 * Notificações in-app e enfileiramento de alertas por e-mail (Quadros 13, 14, 16, 17).
 */
@Service
public class NotificacaoService {

    public record NotificacaoResponse(
            Long id,
            TipoNotificacao tipo,
            String titulo,
            String mensagem,
            Integer marcoDias,
            boolean lida,
            OffsetDateTime criadoEm,
            Long exameId,
            Long documentoId,
            Long laudoId,
            Long colaboradorId) {
    }

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final AdminSistemaNotificacaoService adminSistemaNotificacaoService;

    public NotificacaoService(NotificacaoRepository notificacaoRepository,
            UsuarioRepository usuarioRepository,
            EmailService emailService,
            AdminSistemaNotificacaoService adminSistemaNotificacaoService) {
        this.notificacaoRepository = notificacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.adminSistemaNotificacaoService = adminSistemaNotificacaoService;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificacaoResponse> listarMinhas(Pageable pageable) {
        Long usuarioId = TenantContext.exigirUsuario().getId();
        if (TenantContext.isAdministrador()) {
            return adminSistemaNotificacaoService.listar(usuarioId, pageable);
        }
        Page<Notificacao> page = notificacaoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId, pageable);
        return PageResponse.de(page, NotificacaoService::paraResposta);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas() {
        Long usuarioId = TenantContext.exigirUsuario().getId();
        if (TenantContext.isAdministrador()) {
            return adminSistemaNotificacaoService.contarNaoLidas(usuarioId);
        }
        return notificacaoRepository.countByUsuarioIdAndLidaFalse(usuarioId);
    }

    @Transactional
    public void marcarComoLida(Long id) {
        Long usuarioId = TenantContext.exigirUsuario().getId();
        if (TenantContext.isAdministrador()) {
            adminSistemaNotificacaoService.marcarComoLida(usuarioId, id);
            return;
        }
        Notificacao notif = notificacaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada."));
        notif.marcarComoLida();
    }

    @Transactional
    public void marcarTodasComoLidas() {
        Long usuarioId = TenantContext.exigirUsuario().getId();
        if (TenantContext.isAdministrador()) {
            adminSistemaNotificacaoService.marcarTodasComoLidas(usuarioId);
            return;
        }
        notificacaoRepository.marcarTodasComoLidas(usuarioId, OffsetDateTime.now());
    }

    @Transactional
    public int notificarPerfisDaEmpresa(Long empresaId, Set<Perfil> perfis, TipoNotificacao tipo,
            String titulo, String mensagem, Exame exame, Documento documento, Laudo laudo, Colaborador colaborador) {
        List<Usuario> destinatarios = usuarioRepository.findByEmpresaIdAndPerfilInAndAtivoTrue(empresaId, perfis);
        for (Usuario dest : destinatarios) {
            criarEEnviar(dest, tipo, null, LocalDate.now(), titulo, mensagem, exame, documento, laudo, colaborador);
        }
        return destinatarios.size();
    }

    @Transactional
    public void notificarUsuario(Usuario dest, TipoNotificacao tipo, String titulo, String mensagem,
            Exame exame, Documento documento, Laudo laudo, Colaborador colaborador) {
        if (dest == null || !dest.isAtivo()) {
            return;
        }
        criarEEnviar(dest, tipo, null, LocalDate.now(), titulo, mensagem, exame, documento, laudo, colaborador);
    }

    /**
     * Cria alerta de vencimento para exame garantindo idempotência no dia (RF31).
     */
    @Transactional
    public boolean registrarAlertaExame(Usuario dest, Exame exame, int marcoDias, LocalDate hoje,
            String titulo, String mensagem) {
        if (notificacaoRepository.existsByUsuarioIdAndExameIdAndMarcoDiasAndDataReferencia(
                dest.getId(), exame.getId(), marcoDias, hoje)) {
            return false;
        }
        TipoNotificacao tipo = marcoDias == 0 ? TipoNotificacao.URGENCIA : TipoNotificacao.PREVENTIVO;
        criarEEnviar(dest, tipo, marcoDias, hoje, titulo, mensagem, exame, null, null, null);
        return true;
    }

    /**
     * Cria alerta de vencimento para documento garantindo idempotência no dia (RF31).
     */
    @Transactional
    public boolean registrarAlertaDocumento(Usuario dest, Documento documento, int marcoDias, LocalDate hoje,
            String titulo, String mensagem) {
        if (notificacaoRepository.existsByUsuarioIdAndDocumentoIdAndMarcoDiasAndDataReferencia(
                dest.getId(), documento.getId(), marcoDias, hoje)) {
            return false;
        }
        TipoNotificacao tipo = marcoDias == 0 ? TipoNotificacao.URGENCIA : TipoNotificacao.PREVENTIVO;
        criarEEnviar(dest, tipo, marcoDias, hoje, titulo, mensagem, null, documento, null, null);
        return true;
    }

    private void criarEEnviar(Usuario dest, TipoNotificacao tipo, Integer marcoDias, LocalDate dataRef,
            String titulo, String mensagem, Exame exame, Documento documento, Laudo laudo, Colaborador colaborador) {
        Notificacao n = new Notificacao();
        n.setEmpresaId(dest.getEmpresaId());
        n.setUsuario(dest);
        n.setTipo(tipo);
        n.setMarcoDias(marcoDias);
        n.setDataReferencia(dataRef);
        n.setTitulo(titulo);
        n.setMensagem(mensagem);
        n.setExame(exame);
        n.setDocumento(documento);
        n.setLaudo(laudo);
        n.setColaborador(colaborador);
        Notificacao salva = notificacaoRepository.save(n);
        emailService.enfileirar(salva, dest.getEmail(), "[Saluris IA] " + titulo, mensagem, false);
    }

    public static NotificacaoResponse paraResposta(Notificacao n) {
        return new NotificacaoResponse(
                n.getId(),
                n.getTipo(),
                n.getTitulo(),
                n.getMensagem(),
                n.getMarcoDias(),
                n.isLida(),
                n.getCriadoEm(),
                n.getExame() != null ? n.getExame().getId() : null,
                n.getDocumento() != null ? n.getDocumento().getId() : null,
                n.getLaudo() != null ? n.getLaudo().getId() : null,
                n.getColaborador() != null ? n.getColaborador().getId() : null);
    }
}
