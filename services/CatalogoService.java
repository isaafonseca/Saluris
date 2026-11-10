package br.com.saluris.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.TipoDocumento;
import br.com.saluris.domain.entity.TipoExame;
import br.com.saluris.domain.entity.TipoLaudo;
import br.com.saluris.exception.ConflitoException;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.exception.RegraNegocioException;
import br.com.saluris.repository.TipoDocumentoRepository;
import br.com.saluris.repository.TipoExameRepository;
import br.com.saluris.repository.TipoLaudoRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Gestão dos catálogos globais de SST (Quadros 02, 03, 04 / RF03, RF04, RF05).
 */
@Service
public class CatalogoService {

    public record TipoExameForm(
            @NotBlank(message = "Informe o nome do exame") @Size(max = 120) String nome,
            @Size(max = 500) String descricao,
            @Size(max = 1000) String observacoesTecnicas) {
    }

    public record TipoExameResponse(Long id, String nome, String descricao, String observacoesTecnicas, boolean ativo) {
    }

    public record TipoDocumentoForm(
            @NotBlank(message = "Informe a sigla") @Size(max = 20) String sigla,
            @NotBlank(message = "Informe o nome do tipo de documento") @Size(max = 150) String nome,
            @Size(max = 500) String descricao,
            @NotNull(message = "Informe a periodicidade padrão (12 ou 24 meses)") Integer periodicidadePadraoMeses) {
    }

    public record TipoDocumentoResponse(
            Long id, String sigla, String nome, String descricao, Integer periodicidadePadraoMeses, boolean ativo) {
    }

    public record TipoLaudoForm(
            @NotBlank(message = "Informe o nome do tipo de laudo") @Size(max = 150) String nome,
            @Size(max = 500) String descricao,
            @Size(max = 1000) String observacoes) {
    }

    public record TipoLaudoResponse(Long id, String nome, String descricao, String observacoes, boolean ativo) {
    }

    private final TipoExameRepository tipoExameRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final TipoLaudoRepository tipoLaudoRepository;

    public CatalogoService(TipoExameRepository tipoExameRepository,
            TipoDocumentoRepository tipoDocumentoRepository,
            TipoLaudoRepository tipoLaudoRepository) {
        this.tipoExameRepository = tipoExameRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.tipoLaudoRepository = tipoLaudoRepository;
    }

    // --- Tipos de Exame ---

    @Transactional(readOnly = true)
    public List<TipoExameResponse> listarTiposExame(boolean somenteAtivos) {
        List<TipoExame> lista = somenteAtivos
                ? tipoExameRepository.findByAtivoTrueOrderByNomeAsc()
                : tipoExameRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
        return lista.stream().map(CatalogoService::paraResposta).toList();
    }

    @Transactional
    public TipoExameResponse criarTipoExame(TipoExameForm form) {
        String nome = form.nome().trim();
        if (tipoExameRepository.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Já existe um tipo de exame com este nome.");
        }
        TipoExame t = new TipoExame();
        t.setNome(nome);
        t.setDescricao(limpar(form.descricao()));
        t.setObservacoesTecnicas(limpar(form.observacoesTecnicas()));
        return paraResposta(tipoExameRepository.save(t));
    }

    @Transactional
    public TipoExameResponse atualizarTipoExame(Long id, TipoExameForm form) {
        TipoExame t = tipoExameRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de exame não encontrado."));
        String nome = form.nome().trim();
        if (!t.getNome().equalsIgnoreCase(nome) && tipoExameRepository.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Já existe um tipo de exame com este nome.");
        }
        t.setNome(nome);
        t.setDescricao(limpar(form.descricao()));
        t.setObservacoesTecnicas(limpar(form.observacoesTecnicas()));
        return paraResposta(t);
    }

    @Transactional
    public TipoExameResponse alterarAtivoTipoExame(Long id, boolean ativo) {
        TipoExame t = tipoExameRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de exame não encontrado."));
        t.setAtivo(ativo);
        return paraResposta(t);
    }

    // --- Tipos de Documento ---

    @Transactional(readOnly = true)
    public List<TipoDocumentoResponse> listarTiposDocumento(boolean somenteAtivos) {
        List<TipoDocumento> lista = somenteAtivos
                ? tipoDocumentoRepository.findByAtivoTrueOrderBySiglaAsc()
                : tipoDocumentoRepository.findAll(Sort.by(Sort.Direction.ASC, "sigla"));
        return lista.stream().map(CatalogoService::paraResposta).toList();
    }

    @Transactional
    public TipoDocumentoResponse criarTipoDocumento(TipoDocumentoForm form) {
        validarPeriodicidadeDocumento(form.periodicidadePadraoMeses());
        String sigla = form.sigla().trim().toUpperCase();
        String nome = form.nome().trim();
        if (tipoDocumentoRepository.existsBySiglaIgnoreCase(sigla)) {
            throw new ConflitoException("Já existe um tipo de documento com esta sigla.");
        }
        if (tipoDocumentoRepository.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Já existe um tipo de documento com este nome.");
        }
        TipoDocumento t = new TipoDocumento();
        t.setSigla(sigla);
        t.setNome(nome);
        t.setDescricao(limpar(form.descricao()));
        t.setPeriodicidadePadraoMeses(form.periodicidadePadraoMeses());
        return paraResposta(tipoDocumentoRepository.save(t));
    }

    @Transactional
    public TipoDocumentoResponse atualizarTipoDocumento(Long id, TipoDocumentoForm form) {
        validarPeriodicidadeDocumento(form.periodicidadePadraoMeses());
        TipoDocumento t = tipoDocumentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de documento não encontrado."));
        String sigla = form.sigla().trim().toUpperCase();
        String nome = form.nome().trim();
        if (!t.getSigla().equalsIgnoreCase(sigla) && tipoDocumentoRepository.existsBySiglaIgnoreCase(sigla)) {
            throw new ConflitoException("Já existe um tipo de documento com esta sigla.");
        }
        if (!t.getNome().equalsIgnoreCase(nome) && tipoDocumentoRepository.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Já existe um tipo de documento com este nome.");
        }
        t.setSigla(sigla);
        t.setNome(nome);
        t.setDescricao(limpar(form.descricao()));
        t.setPeriodicidadePadraoMeses(form.periodicidadePadraoMeses());
        return paraResposta(t);
    }

    @Transactional
    public TipoDocumentoResponse alterarAtivoTipoDocumento(Long id, boolean ativo) {
        TipoDocumento t = tipoDocumentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de documento não encontrado."));
        t.setAtivo(ativo);
        return paraResposta(t);
    }

    // --- Tipos de Laudo ---

    @Transactional(readOnly = true)
    public List<TipoLaudoResponse> listarTiposLaudo(boolean somenteAtivos) {
        List<TipoLaudo> lista = somenteAtivos
                ? tipoLaudoRepository.findByAtivoTrueOrderByNomeAsc()
                : tipoLaudoRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
        return lista.stream().map(CatalogoService::paraResposta).toList();
    }

    @Transactional
    public TipoLaudoResponse criarTipoLaudo(TipoLaudoForm form) {
        String nome = form.nome().trim();
        if (tipoLaudoRepository.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Já existe um tipo de laudo com este nome.");
        }
        TipoLaudo t = new TipoLaudo();
        t.setNome(nome);
        t.setDescricao(limpar(form.descricao()));
        t.setObservacoes(limpar(form.observacoes()));
        return paraResposta(tipoLaudoRepository.save(t));
    }

    @Transactional
    public TipoLaudoResponse atualizarTipoLaudo(Long id, TipoLaudoForm form) {
        TipoLaudo t = tipoLaudoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de laudo não encontrado."));
        String nome = form.nome().trim();
        if (!t.getNome().equalsIgnoreCase(nome) && tipoLaudoRepository.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Já existe um tipo de laudo com este nome.");
        }
        t.setNome(nome);
        t.setDescricao(limpar(form.descricao()));
        t.setObservacoes(limpar(form.observacoes()));
        return paraResposta(t);
    }

    @Transactional
    public TipoLaudoResponse alterarAtivoTipoLaudo(Long id, boolean ativo) {
        TipoLaudo t = tipoLaudoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de laudo não encontrado."));
        t.setAtivo(ativo);
        return paraResposta(t);
    }

    private static void validarPeriodicidadeDocumento(Integer meses) {
        if (meses == null || (meses != 12 && meses != 24)) {
            throw new RegraNegocioException("A periodicidade padrão do documento deve ser de 12 ou 24 meses.");
        }
    }

    private static String limpar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static TipoExameResponse paraResposta(TipoExame t) {
        return new TipoExameResponse(t.getId(), t.getNome(), t.getDescricao(), t.getObservacoesTecnicas(), t.isAtivo());
    }

    private static TipoDocumentoResponse paraResposta(TipoDocumento t) {
        return new TipoDocumentoResponse(
                t.getId(), t.getSigla(), t.getNome(), t.getDescricao(), t.getPeriodicidadePadraoMeses(), t.isAtivo());
    }

    private static TipoLaudoResponse paraResposta(TipoLaudo t) {
        return new TipoLaudoResponse(t.getId(), t.getNome(), t.getDescricao(), t.getObservacoes(), t.isAtivo());
    }
}
