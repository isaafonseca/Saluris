package br.com.saluris.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.saluris.domain.entity.ArquivoPdf;
import br.com.saluris.domain.entity.Documento;
import br.com.saluris.domain.entity.TipoDocumento;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.domain.enums.StatusValidade;
import br.com.saluris.domain.enums.TipoNotificacao;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.exception.RegraNegocioException;
import br.com.saluris.repository.DocumentoRepository;
import br.com.saluris.repository.TipoDocumentoRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.shared.util.ValidadeCalculator;
import br.com.saluris.tenant.TenantContext;

/**
 * Gestão de documentos SST como PGR, PCMSO e LTCAT (Quadros 08, 12 / RN06 / RF12-RF13 / RF20-RF21).
 */
@Service
public class DocumentoService {

    public record DocumentoResponse(
            Long id,
            Long tipoDocumentoId,
            String tipoDocumentoSigla,
            String tipoDocumentoNome,
            String titulo,
            String descricao,
            Integer versao,
            LocalDate dataEmissao,
            LocalDate dataValidade,
            long diasParaVencer,
            StatusValidade statusValidade,
            Long documentoBaseId,
            String documentoBaseTitulo,
            String arquivoNome,
            String enviadoPorNome,
            OffsetDateTime criadoEm,
            boolean vigente) {
    }

    private final DocumentoRepository documentoRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArquivoStorageService arquivoStorageService;
    private final NotificacaoService notificacaoService;

    public DocumentoService(DocumentoRepository documentoRepository,
            TipoDocumentoRepository tipoDocumentoRepository,
            UsuarioRepository usuarioRepository,
            ArquivoStorageService arquivoStorageService,
            NotificacaoService notificacaoService) {
        this.documentoRepository = documentoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.arquivoStorageService = arquivoStorageService;
        this.notificacaoService = notificacaoService;
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listar(Long empresaIdParam, boolean somenteVigentes) {
        Long empresaId = TenantContext.resolverEmpresaIdOpcional(empresaIdParam);
        List<Documento> todos = empresaId != null
                ? documentoRepository.findByEmpresaIdOrderByDataValidadeAsc(empresaId)
                : documentoRepository.findAll();
        Map<Long, Integer> maiorVersaoPorTipo = calcularMaiorVersaoPorTipo(todos);
        LocalDate hoje = LocalDate.now();
        return todos.stream()
                .map(d -> paraResposta(d, hoje,
                        d.getVersao().equals(maiorVersaoPorTipo.get(d.getTipoDocumento().getId()))))
                .filter(d -> !somenteVigentes || d.vigente())
                .toList();
    }

    @Transactional
    public DocumentoResponse criar(Long tipoDocumentoId, String titulo, String descricao,
            LocalDate dataEmissao, LocalDate dataValidade, Long documentoBaseId, MultipartFile arquivo) {
        if (tipoDocumentoId == null) {
            throw new RegraNegocioException("Informe o tipo de documento SST.");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new RegraNegocioException("Informe o título do documento.");
        }
        if (dataEmissao == null) {
            throw new RegraNegocioException("Informe a data de emissão do documento.");
        }
        Long empresaId = TenantContext.resolverEmpresaId(null);

        TipoDocumento tipo = tipoDocumentoRepository.findById(tipoDocumentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de documento não encontrado."));

        LocalDate validadeFinal = dataValidade != null
                ? dataValidade
                : dataEmissao.plusMonths(tipo.getPeriodicidadePadraoMeses());
        if (!validadeFinal.isAfter(dataEmissao)) {
            throw new RegraNegocioException("A data de validade deve ser posterior à data de emissão.");
        }

        Documento docBase = null;
        if (documentoBaseId != null) {
            docBase = documentoRepository.findByIdAndEmpresaId(documentoBaseId, empresaId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Documento base não encontrado na empresa."));
        }

        Usuario autor = usuarioRepository.findById(TenantContext.exigirUsuario().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        int proximaVersao = documentoRepository
                .findTopByEmpresaIdAndTipoDocumentoIdOrderByVersaoDesc(empresaId, tipo.getId())
                .map(d -> d.getVersao() + 1)
                .orElse(1);

        ArquivoPdf pdf = arquivoStorageService.armazenarPdf(arquivo, empresaId);
        LocalDate hoje = LocalDate.now();
        StatusValidade statusVal = ValidadeCalculator.classificar(validadeFinal, hoje);

        Documento d = new Documento();
        d.setEmpresaId(empresaId);
        d.setTipoDocumento(tipo);
        d.setDocumentoBase(docBase);
        d.setTitulo(titulo.trim());
        d.setDescricao(descricao != null && !descricao.isBlank() ? descricao.trim() : null);
        d.setVersao(proximaVersao);
        d.setDataEmissao(dataEmissao);
        d.setDataValidade(validadeFinal);
        d.setStatusValidade(statusVal);
        d.setArquivo(pdf);
        d.setEnviadoPor(autor);

        Documento salvo = documentoRepository.save(d);

        if (statusVal == StatusValidade.VENCIDO) {
            String t = "Documento SST cadastrado já vencido: " + tipo.getSigla() + " - " + salvo.getTitulo();
            String msg = "O documento " + tipo.getSigla() + " (" + salvo.getTitulo()
                    + ", versão " + salvo.getVersao() + ") foi registrado com data de validade "
                    + salvo.getDataValidade() + ", encontrando-se vencido.";
            List<Usuario> responsaveis = usuarioRepository.findByEmpresaIdAndPerfilInAndAtivoTrue(
                    empresaId, Set.of(Perfil.RH_EMPRESA, Perfil.ESPECIALISTA_SST));
            for (Usuario resp : responsaveis) {
                notificacaoService.registrarAlertaDocumento(resp, salvo, 0, hoje, t, msg);
            }
        }

        return paraResposta(salvo, hoje, true);
    }

    @Transactional(readOnly = true)
    public ArquivoStorageService.PdfCarregado baixarArquivo(Long id) {
        Documento d = buscarDoTenant(id);
        return arquivoStorageService.carregarPdf(d.getArquivo());
    }

    public Documento buscarDoTenant(Long id) {
        if (TenantContext.isAdministrador()) {
            return documentoRepository.findById(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado."));
        }
        Long empresaId = TenantContext.resolverEmpresaId(null);
        return documentoRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado."));
    }

    public static Map<Long, Integer> calcularMaiorVersaoPorTipo(List<Documento> docs) {
        Map<Long, Integer> mapa = new HashMap<>();
        for (Documento d : docs) {
            Long tipoId = d.getTipoDocumento().getId();
            mapa.merge(tipoId, d.getVersao(), Math::max);
        }
        return mapa;
    }

    public static DocumentoResponse paraResposta(Documento d, LocalDate hoje, boolean vigente) {
        return new DocumentoResponse(
                d.getId(),
                d.getTipoDocumento().getId(),
                d.getTipoDocumento().getSigla(),
                d.getTipoDocumento().getNome(),
                d.getTitulo(),
                d.getDescricao(),
                d.getVersao(),
                d.getDataEmissao(),
                d.getDataValidade(),
                ValidadeCalculator.diasParaVencer(d.getDataValidade(), hoje),
                d.getStatusValidade(),
                d.getDocumentoBase() != null ? d.getDocumentoBase().getId() : null,
                d.getDocumentoBase() != null ? d.getDocumentoBase().getTitulo() : null,
                d.getArquivo() != null ? d.getArquivo().getNome() : null,
                d.getEnviadoPor() != null ? d.getEnviadoPor().getNome() : null,
                d.getCriadoEm(),
                vigente);
    }
}
