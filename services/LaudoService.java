package br.com.saluris.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.saluris.domain.entity.ArquivoPdf;
import br.com.saluris.domain.entity.AssinaturaMedica;
import br.com.saluris.domain.entity.Colaborador;
import br.com.saluris.domain.entity.Laudo;
import br.com.saluris.domain.entity.TipoLaudo;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.domain.enums.StatusLaudo;
import br.com.saluris.domain.enums.TipoNotificacao;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.exception.RegraNegocioException;
import br.com.saluris.repository.ColaboradorRepository;
import br.com.saluris.repository.LaudoRepository;
import br.com.saluris.repository.TipoLaudoRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.tenant.TenantContext;

/**
 * Gestão de laudos e documentos técnicos em PDF (Quadro 07 / RF09-RF11).
 */
@Service
public class LaudoService {

    public record LaudoResponse(
            Long id,
            Long tipoLaudoId,
            String tipoLaudoNome,
            String titulo,
            String descricao,
            StatusLaudo status,
            LocalDate dataEmissao,
            LocalDate dataValidade,
            Long colaboradorId,
            String colaboradorNome,
            String arquivoNome,
            String criadoPorNome,
            String medicoNome,
            String crmAssinatura,
            String crmUfAssinatura,
            OffsetDateTime assinadoEm,
            String justificativaRecusa,
            OffsetDateTime criadoEm) {
    }

    private final LaudoRepository laudoRepository;
    private final TipoLaudoRepository tipoLaudoRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArquivoStorageService arquivoStorageService;
    private final NotificacaoService notificacaoService;

    public LaudoService(LaudoRepository laudoRepository,
            TipoLaudoRepository tipoLaudoRepository,
            ColaboradorRepository colaboradorRepository,
            UsuarioRepository usuarioRepository,
            ArquivoStorageService arquivoStorageService,
            NotificacaoService notificacaoService) {
        this.laudoRepository = laudoRepository;
        this.tipoLaudoRepository = tipoLaudoRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.usuarioRepository = usuarioRepository;
        this.arquivoStorageService = arquivoStorageService;
        this.notificacaoService = notificacaoService;
    }

    @Transactional(readOnly = true)
    public List<LaudoResponse> listar(Long empresaIdParam, StatusLaudo status) {
        Long empresaId = TenantContext.resolverEmpresaIdOpcional(empresaIdParam);
        List<Laudo> lista;
        if (empresaId != null) {
            lista = status != null
                    ? laudoRepository.findByEmpresaIdAndStatusOrderByCriadoEmDesc(empresaId, status)
                    : laudoRepository.findByEmpresaIdOrderByCriadoEmDesc(empresaId);
        } else {
            lista = laudoRepository.findAll().stream()
                    .filter(l -> status == null || l.getStatus() == status)
                    .toList();
        }
        return lista.stream().map(LaudoService::paraResposta).toList();
    }

    @Transactional
    public LaudoResponse criar(Long tipoLaudoId, String titulo, String descricao,
            LocalDate dataEmissao, LocalDate dataValidade, Long colaboradorId, MultipartFile arquivo) {
        if (tipoLaudoId == null) {
            throw new RegraNegocioException("Informe o tipo de laudo.");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new RegraNegocioException("Informe o título do laudo.");
        }
        LocalDate emissao = dataEmissao != null ? dataEmissao : LocalDate.now();
        if (dataValidade != null && dataValidade.isBefore(emissao)) {
            throw new RegraNegocioException("A data de validade não pode ser anterior à data de emissão.");
        }

        Long empresaId = TenantContext.resolverEmpresaId(null);
        TipoLaudo tipo = tipoLaudoRepository.findById(tipoLaudoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de laudo não encontrado."));

        Colaborador colab = null;
        if (colaboradorId != null) {
            colab = colaboradorRepository.findByIdAndEmpresaId(colaboradorId, empresaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Colaborador não encontrado na empresa."));
        }

        Usuario autor = usuarioRepository.findById(TenantContext.exigirUsuario().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        ArquivoPdf pdf = arquivoStorageService.armazenarPdf(arquivo, empresaId);

        Laudo l = new Laudo();
        l.setEmpresaId(empresaId);
        l.setTipoLaudo(tipo);
        l.setColaborador(colab);
        l.setTitulo(titulo.trim());
        l.setDescricao(descricao != null && !descricao.isBlank() ? descricao.trim() : null);
        l.setStatus(StatusLaudo.AGUARDANDO_ASSINATURA);
        l.setDataEmissao(emissao);
        l.setDataValidade(dataValidade);
        l.setArquivo(pdf);
        l.setCriadoPor(autor);

        Laudo salvo = laudoRepository.save(l);

        String tituloNotif = "Novo laudo aguardando assinatura: " + salvo.getTitulo();
        String msg = "O laudo técnico '" + salvo.getTitulo() + "' (" + tipo.getNome()
                + ") foi emitido por " + autor.getNome() + " e aguarda assinatura médica.";
        notificacaoService.notificarPerfisDaEmpresa(empresaId,
                Set.of(Perfil.MEDICO_TRABALHO),
                TipoNotificacao.LAUDO_PENDENTE_ASSINATURA, tituloNotif, msg, null, null, salvo, null);

        return paraResposta(salvo);
    }

    @Transactional
    public LaudoResponse reenviar(Long id, MultipartFile arquivo) {
        Laudo l = buscarDoTenant(id);
        ArquivoPdf pdf = arquivoStorageService.armazenarPdf(arquivo, l.getEmpresaId());
        l.reenviar(pdf);

        String tituloNotif = "Laudo revisado e reenviado para assinatura: " + l.getTitulo();
        String msg = "O laudo '" + l.getTitulo() + "' foi corrigido e reenviado para validação médica.";
        notificacaoService.notificarPerfisDaEmpresa(l.getEmpresaId(),
                Set.of(Perfil.MEDICO_TRABALHO),
                TipoNotificacao.LAUDO_PENDENTE_ASSINATURA, tituloNotif, msg, null, null, l, null);

        return paraResposta(l);
    }

    @Transactional(readOnly = true)
    public ArquivoStorageService.PdfCarregado baixarArquivo(Long id) {
        Laudo l = buscarDoTenant(id);
        return arquivoStorageService.carregarPdf(l.getArquivo());
    }

    public Laudo buscarDoTenant(Long id) {
        if (TenantContext.isAdministrador()) {
            return laudoRepository.findById(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Laudo não encontrado."));
        }
        Long empresaId = TenantContext.resolverEmpresaId(null);
        return laudoRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laudo não encontrado."));
    }

    public static LaudoResponse paraResposta(Laudo l) {
        AssinaturaMedica a = l.getAssinatura();
        String medicoNome = a != null && a.getMedico() != null ? a.getMedico().getNome() : null;
        return new LaudoResponse(
                l.getId(),
                l.getTipoLaudo().getId(),
                l.getTipoLaudo().getNome(),
                l.getTitulo(),
                l.getDescricao(),
                l.getStatus(),
                l.getDataEmissao(),
                l.getDataValidade(),
                l.getColaborador() != null ? l.getColaborador().getId() : null,
                l.getColaborador() != null ? l.getColaborador().getNome() : null,
                l.getArquivo() != null ? l.getArquivo().getNome() : null,
                l.getCriadoPor() != null ? l.getCriadoPor().getNome() : null,
                medicoNome,
                a != null ? a.getCrmAssinatura() : null,
                a != null ? a.getCrmUfAssinatura() : null,
                a != null ? a.getAssinadoEm() : null,
                a != null ? a.getJustificativaRecusa() : null,
                l.getCriadoEm());
    }
}
