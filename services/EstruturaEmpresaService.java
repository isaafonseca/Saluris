package br.com.saluris.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.Funcao;
import br.com.saluris.domain.entity.Setor;
import br.com.saluris.domain.entity.Unidade;
import br.com.saluris.exception.ConflitoException;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.repository.FuncaoRepository;
import br.com.saluris.repository.SetorRepository;
import br.com.saluris.repository.UnidadeRepository;
import br.com.saluris.tenant.TenantContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Gestão da estrutura organizacional por empresa: Unidades, Funções e Setores.
 */
@Service
public class EstruturaEmpresaService {

    public record UnidadeForm(
            @NotBlank(message = "Informe o nome da unidade") @Size(max = 150) String nome,
            @Size(max = 100) String cidade,
            @Size(max = 2) String uf) {
    }

    public record UnidadeResponse(Long id, Long empresaId, String nome, String cidade, String uf, boolean ativo) {
    }

    public record FuncaoForm(
            @NotBlank(message = "Informe o nome da função") @Size(max = 120) String nome,
            @Size(max = 500) String descricao) {
    }

    public record FuncaoResponse(Long id, Long empresaId, String nome, String descricao, boolean ativo) {
    }

    public record SetorForm(
            @NotBlank(message = "Informe o nome do setor") @Size(max = 120) String nome,
            @Size(max = 500) String descricao) {
    }

    public record SetorResponse(Long id, Long empresaId, String nome, String descricao, boolean ativo) {
    }

    private final UnidadeRepository unidadeRepository;
    private final FuncaoRepository funcaoRepository;
    private final SetorRepository setorRepository;

    public EstruturaEmpresaService(UnidadeRepository unidadeRepository,
            FuncaoRepository funcaoRepository,
            SetorRepository setorRepository) {
        this.unidadeRepository = unidadeRepository;
        this.funcaoRepository = funcaoRepository;
        this.setorRepository = setorRepository;
    }

    // --- Unidades ---

    @Transactional(readOnly = true)
    public List<UnidadeResponse> listarUnidades(Long empresaIdParam, boolean somenteAtivos) {
        Long empresaId = TenantContext.resolverEmpresaIdOpcional(empresaIdParam);
        List<Unidade> lista;
        if (empresaId != null) {
            lista = somenteAtivos
                    ? unidadeRepository.findByEmpresaIdAndAtivaTrueOrderByNomeAsc(empresaId)
                    : unidadeRepository.findByEmpresaIdOrderByNomeAsc(empresaId);
        } else {
            lista = unidadeRepository.findAll().stream()
                    .filter(u -> !somenteAtivos || u.isAtiva())
                    .toList();
        }
        return lista.stream().map(EstruturaEmpresaService::paraResposta).toList();
    }

    @Transactional
    public UnidadeResponse criarUnidade(Long empresaIdParam, UnidadeForm form) {
        Long empresaId = TenantContext.resolverEmpresaId(empresaIdParam);
        String nome = form.nome().trim();
        if (unidadeRepository.existsByEmpresaIdAndNomeIgnoreCase(empresaId, nome)) {
            throw new ConflitoException("Já existe uma unidade com este nome na empresa.");
        }
        Unidade u = new Unidade();
        u.setEmpresaId(empresaId);
        u.setNome(nome);
        u.setCidade(limpar(form.cidade()));
        u.setUf(form.uf() != null && !form.uf().isBlank() ? form.uf().trim().toUpperCase() : null);
        return paraResposta(unidadeRepository.save(u));
    }

    @Transactional
    public UnidadeResponse atualizarUnidade(Long id, UnidadeForm form) {
        Unidade u = buscarUnidade(id);
        String nome = form.nome().trim();
        if (!u.getNome().equalsIgnoreCase(nome)
                && unidadeRepository.existsByEmpresaIdAndNomeIgnoreCase(u.getEmpresaId(), nome)) {
            throw new ConflitoException("Já existe uma unidade com este nome na empresa.");
        }
        u.setNome(nome);
        u.setCidade(limpar(form.cidade()));
        u.setUf(form.uf() != null && !form.uf().isBlank() ? form.uf().trim().toUpperCase() : null);
        return paraResposta(u);
    }

    @Transactional
    public UnidadeResponse alterarAtivoUnidade(Long id, boolean ativo) {
        Unidade u = buscarUnidade(id);
        u.setAtiva(ativo);
        return paraResposta(u);
    }

    private Unidade buscarUnidade(Long id) {
        if (TenantContext.isAdministrador()) {
            return unidadeRepository.findById(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada."));
        }
        Long empresaId = TenantContext.resolverEmpresaId(null);
        return unidadeRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada."));
    }

    // --- Funções ---

    @Transactional(readOnly = true)
    public List<FuncaoResponse> listarFuncoes(Long empresaIdParam, boolean somenteAtivos) {
        Long empresaId = TenantContext.resolverEmpresaIdOpcional(empresaIdParam);
        List<Funcao> lista;
        if (empresaId != null) {
            lista = somenteAtivos
                    ? funcaoRepository.findByEmpresaIdAndAtivaTrueOrderByNomeAsc(empresaId)
                    : funcaoRepository.findByEmpresaIdOrderByNomeAsc(empresaId);
        } else {
            lista = funcaoRepository.findAll().stream()
                    .filter(f -> !somenteAtivos || f.isAtiva())
                    .toList();
        }
        return lista.stream().map(EstruturaEmpresaService::paraResposta).toList();
    }

    @Transactional
    public FuncaoResponse criarFuncao(Long empresaIdParam, FuncaoForm form) {
        Long empresaId = TenantContext.resolverEmpresaId(empresaIdParam);
        String nome = form.nome().trim();
        if (funcaoRepository.existsByEmpresaIdAndNomeIgnoreCase(empresaId, nome)) {
            throw new ConflitoException("Já existe uma função com este nome na empresa.");
        }
        Funcao f = new Funcao();
        f.setEmpresaId(empresaId);
        f.setNome(nome);
        f.setDescricao(limpar(form.descricao()));
        return paraResposta(funcaoRepository.save(f));
    }

    @Transactional
    public FuncaoResponse atualizarFuncao(Long id, FuncaoForm form) {
        Funcao f = buscarFuncao(id);
        String nome = form.nome().trim();
        if (!f.getNome().equalsIgnoreCase(nome)
                && funcaoRepository.existsByEmpresaIdAndNomeIgnoreCase(f.getEmpresaId(), nome)) {
            throw new ConflitoException("Já existe uma função com este nome na empresa.");
        }
        f.setNome(nome);
        f.setDescricao(limpar(form.descricao()));
        return paraResposta(f);
    }

    @Transactional
    public FuncaoResponse alterarAtivoFuncao(Long id, boolean ativo) {
        Funcao f = buscarFuncao(id);
        f.setAtiva(ativo);
        return paraResposta(f);
    }

    private Funcao buscarFuncao(Long id) {
        if (TenantContext.isAdministrador()) {
            return funcaoRepository.findById(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Função não encontrada."));
        }
        Long empresaId = TenantContext.resolverEmpresaId(null);
        return funcaoRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Função não encontrada."));
    }

    // --- Setores ---

    @Transactional(readOnly = true)
    public List<SetorResponse> listarSetores(Long empresaIdParam, boolean somenteAtivos) {
        Long empresaId = TenantContext.resolverEmpresaIdOpcional(empresaIdParam);
        List<Setor> lista;
        if (empresaId != null) {
            lista = somenteAtivos
                    ? setorRepository.findByEmpresaIdAndAtivoTrueOrderByNomeAsc(empresaId)
                    : setorRepository.findByEmpresaIdOrderByNomeAsc(empresaId);
        } else {
            lista = setorRepository.findAll().stream()
                    .filter(s -> !somenteAtivos || s.isAtivo())
                    .toList();
        }
        return lista.stream().map(EstruturaEmpresaService::paraResposta).toList();
    }

    @Transactional
    public SetorResponse criarSetor(Long empresaIdParam, SetorForm form) {
        Long empresaId = TenantContext.resolverEmpresaId(empresaIdParam);
        String nome = form.nome().trim();
        if (setorRepository.existsByEmpresaIdAndNomeIgnoreCase(empresaId, nome)) {
            throw new ConflitoException("Já existe um setor com este nome na empresa.");
        }
        Setor s = new Setor();
        s.setEmpresaId(empresaId);
        s.setNome(nome);
        s.setDescricao(limpar(form.descricao()));
        return paraResposta(setorRepository.save(s));
    }

    @Transactional
    public SetorResponse atualizarSetor(Long id, SetorForm form) {
        Setor s = buscarSetor(id);
        String nome = form.nome().trim();
        if (!s.getNome().equalsIgnoreCase(nome)
                && setorRepository.existsByEmpresaIdAndNomeIgnoreCase(s.getEmpresaId(), nome)) {
            throw new ConflitoException("Já existe um setor com este nome na empresa.");
        }
        s.setNome(nome);
        s.setDescricao(limpar(form.descricao()));
        return paraResposta(s);
    }

    @Transactional
    public SetorResponse alterarAtivoSetor(Long id, boolean ativo) {
        Setor s = buscarSetor(id);
        s.setAtivo(ativo);
        return paraResposta(s);
    }

    private Setor buscarSetor(Long id) {
        if (TenantContext.isAdministrador()) {
            return setorRepository.findById(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado."));
        }
        Long empresaId = TenantContext.resolverEmpresaId(null);
        return setorRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado."));
    }

    private static String limpar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static UnidadeResponse paraResposta(Unidade u) {
        return new UnidadeResponse(u.getId(), u.getEmpresaId(), u.getNome(), u.getCidade(), u.getUf(), u.isAtiva());
    }

    private static FuncaoResponse paraResposta(Funcao f) {
        return new FuncaoResponse(f.getId(), f.getEmpresaId(), f.getNome(), f.getDescricao(), f.isAtiva());
    }

    private static SetorResponse paraResposta(Setor s) {
        return new SetorResponse(s.getId(), s.getEmpresaId(), s.getNome(), s.getDescricao(), s.isAtivo());
    }
}
