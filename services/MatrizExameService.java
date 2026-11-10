package br.com.saluris.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.Colaborador;
import br.com.saluris.domain.entity.Exame;
import br.com.saluris.domain.entity.Funcao;
import br.com.saluris.domain.entity.MatrizExame;
import br.com.saluris.domain.entity.Setor;
import br.com.saluris.domain.entity.TipoExame;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.exception.ConflitoException;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.exception.RegraNegocioException;
import br.com.saluris.repository.ColaboradorRepository;
import br.com.saluris.repository.ExameRepository;
import br.com.saluris.repository.FuncaoRepository;
import br.com.saluris.repository.MatrizExameRepository;
import br.com.saluris.repository.SetorRepository;
import br.com.saluris.repository.TipoExameRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.shared.util.ValidadeCalculator;
import br.com.saluris.tenant.TenantContext;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Matriz de periodicidade de exames por Função + Setor (Quadro 06 / RN03 / RF07-RF08).
 * <p>Ao criar ou atualizar uma regra, recalcula automaticamente os vencimentos em aberto dos colaboradores ativos
 * vinculados à combinação Função + Setor.
 */
@Service
public class MatrizExameService {

    public record MatrizCreateRequest(
            @NotNull(message = "Informe a função") Long funcaoId,
            @NotNull(message = "Informe o setor") Long setorId,
            @NotNull(message = "Informe o tipo de exame") Long tipoExameId,
            @NotNull(message = "Informe a periodicidade")
            @Min(value = 6, message = "A periodicidade mínima é de 6 meses")
            @Max(value = 24, message = "A periodicidade máxima é de 24 meses")
            Integer periodicidadeMeses,
            Boolean obrigatorio) {
    }

    public record MatrizUpdateRequest(
            @NotNull(message = "Informe a periodicidade")
            @Min(value = 6, message = "A periodicidade mínima é de 6 meses")
            @Max(value = 24, message = "A periodicidade máxima é de 24 meses")
            Integer periodicidadeMeses,
            Boolean obrigatorio) {
    }

    public record MatrizExameResponse(
            Long id,
            Long funcaoId,
            String funcaoNome,
            Long setorId,
            String setorNome,
            Long tipoExameId,
            String tipoExameNome,
            Integer periodicidadeMeses,
            boolean obrigatorio,
            String configuradoPorNome,
            OffsetDateTime atualizadoEm) {
    }

    public record ColaboradorAfetado(Long id, String nome) {
    }

    public record ResultadoMatrizResponse(
            MatrizExameResponse configuracao,
            List<ColaboradorAfetado> colaboradoresAfetados) {
    }

    private final MatrizExameRepository matrizRepository;
    private final FuncaoRepository funcaoRepository;
    private final SetorRepository setorRepository;
    private final TipoExameRepository tipoExameRepository;
    private final UsuarioRepository usuarioRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final ExameRepository exameRepository;
    private final ExameService exameService;

    public MatrizExameService(MatrizExameRepository matrizRepository,
            FuncaoRepository funcaoRepository,
            SetorRepository setorRepository,
            TipoExameRepository tipoExameRepository,
            UsuarioRepository usuarioRepository,
            ColaboradorRepository colaboradorRepository,
            ExameRepository exameRepository,
            ExameService exameService) {
        this.matrizRepository = matrizRepository;
        this.funcaoRepository = funcaoRepository;
        this.setorRepository = setorRepository;
        this.tipoExameRepository = tipoExameRepository;
        this.usuarioRepository = usuarioRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.exameRepository = exameRepository;
        this.exameService = exameService;
    }

    @Transactional(readOnly = true)
    public List<MatrizExameResponse> listar(Long empresaIdParam, Long funcaoId, Long setorId) {
        Long empresaId = TenantContext.resolverEmpresaIdOpcional(empresaIdParam);
        List<MatrizExame> base = empresaId != null
                ? matrizRepository.findByEmpresaIdOrderByFuncaoNomeAscSetorNomeAscTipoExameNomeAsc(empresaId)
                : matrizRepository.findAll();
        return base.stream()
                .filter(m -> funcaoId == null || m.getFuncao().getId().equals(funcaoId))
                .filter(m -> setorId == null || m.getSetor().getId().equals(setorId))
                .map(MatrizExameService::paraResposta)
                .toList();
    }

    @Transactional
    public ResultadoMatrizResponse criar(MatrizCreateRequest req) {
        validarPeriodicidade(req.periodicidadeMeses());
        Long empresaId = TenantContext.resolverEmpresaId(null);

        Funcao funcao = funcaoRepository.findByIdAndEmpresaId(req.funcaoId(), empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Função não encontrada na empresa."));
        Setor setor = setorRepository.findByIdAndEmpresaId(req.setorId(), empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado na empresa."));
        TipoExame tipoExame = tipoExameRepository.findById(req.tipoExameId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de exame não encontrado."));

        matrizRepository.findByFuncaoIdAndSetorIdAndTipoExameId(funcao.getId(), setor.getId(), tipoExame.getId())
                .ifPresent(existente -> {
                    throw new ConflitoException(
                            "Já existe periodicidade configurada para esta combinação de Função, Setor e Exame.",
                            Map.of("existenteId", existente.getId()));
                });

        Usuario autor = usuarioRepository.findById(TenantContext.exigirUsuario().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        MatrizExame m = new MatrizExame();
        m.setEmpresaId(empresaId);
        m.setFuncao(funcao);
        m.setSetor(setor);
        m.setTipoExame(tipoExame);
        m.setPeriodicidadeMeses(req.periodicidadeMeses());
        m.setObrigatorio(req.obrigatorio() == null || req.obrigatorio());
        m.setConfiguradoPor(autor);

        MatrizExame salva = matrizRepository.save(m);
        List<ColaboradorAfetado> afetados = sincronizarColaboradoresDaCombinacao(salva);
        return new ResultadoMatrizResponse(paraResposta(salva), afetados);
    }

    @Transactional
    public ResultadoMatrizResponse atualizar(Long id, MatrizUpdateRequest req) {
        validarPeriodicidade(req.periodicidadeMeses());
        Long empresaId = TenantContext.resolverEmpresaId(null);
        MatrizExame m = matrizRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Configuração da matriz não encontrada."));

        Usuario autor = usuarioRepository.findById(TenantContext.exigirUsuario().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        m.setPeriodicidadeMeses(req.periodicidadeMeses());
        if (req.obrigatorio() != null) {
            m.setObrigatorio(req.obrigatorio());
        }
        m.setConfiguradoPor(autor);

        List<ColaboradorAfetado> afetados = sincronizarColaboradoresDaCombinacao(m);
        return new ResultadoMatrizResponse(paraResposta(m), afetados);
    }

    @Transactional
    public void excluir(Long id) {
        Long empresaId = TenantContext.resolverEmpresaId(null);
        MatrizExame m = matrizRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Configuração da matriz não encontrada."));
        List<Colaborador> colabs = colaboradorRepository.findByEmpresaIdAndFuncaoIdAndSetorIdAndAtivoTrue(
                empresaId, m.getFuncao().getId(), m.getSetor().getId());
        for (Colaborador c : colabs) {
            List<Exame> abertos = exameRepository.findByColaboradorIdAndTipoExameIdAndStatusFluxoIn(
                    c.getId(), m.getTipoExame().getId(), ExameService.STATUS_EM_ABERTO);
            for (Exame ex : abertos) {
                ex.setMatrizExame(null);
            }
        }
        matrizRepository.delete(m);
    }

    private List<ColaboradorAfetado> sincronizarColaboradoresDaCombinacao(MatrizExame m) {
        List<Colaborador> colaboradores = colaboradorRepository.findByEmpresaIdAndFuncaoIdAndSetorIdAndAtivoTrue(
                m.getEmpresaId(), m.getFuncao().getId(), m.getSetor().getId());
        LocalDate hoje = LocalDate.now();
        List<ColaboradorAfetado> afetados = new ArrayList<>();

        for (Colaborador colab : colaboradores) {
            List<Exame> abertos = exameRepository.findByColaboradorIdAndTipoExameIdAndStatusFluxoIn(
                    colab.getId(), m.getTipoExame().getId(), ExameService.STATUS_EM_ABERTO);
            if (!abertos.isEmpty()) {
                for (Exame ex : abertos) {
                    LocalDate base = exameService.obterDataBaseRecalculo(ex);
                    LocalDate novoVencimento = base.plusMonths(m.getPeriodicidadeMeses());
                    ex.setMatrizExame(m);
                    ex.setPeriodicidadeMeses(m.getPeriodicidadeMeses());
                    ex.setDataVencimento(novoVencimento);
                    ex.setStatusValidade(ValidadeCalculator.classificar(novoVencimento, hoje));
                }
                afetados.add(new ColaboradorAfetado(colab.getId(), colab.getNome()));
            } else if (m.isObrigatorio()) {
                LocalDate base = exameService.obterDataBaseNovoExame(colab, m.getTipoExame());
                exameService.criarCicloPendente(colab, m, base, hoje);
                afetados.add(new ColaboradorAfetado(colab.getId(), colab.getNome()));
            }
        }
        return afetados;
    }

    private static void validarPeriodicidade(Integer meses) {
        if (meses == null || meses < 6 || meses > 24) {
            throw new RegraNegocioException("A periodicidade deve estar entre 6 e 24 meses.");
        }
    }

    public static MatrizExameResponse paraResposta(MatrizExame m) {
        return new MatrizExameResponse(
                m.getId(),
                m.getFuncao().getId(),
                m.getFuncao().getNome(),
                m.getSetor().getId(),
                m.getSetor().getNome(),
                m.getTipoExame().getId(),
                m.getTipoExame().getNome(),
                m.getPeriodicidadeMeses(),
                m.isObrigatorio(),
                m.getConfiguradoPor() != null ? m.getConfiguradoPor().getNome() : null,
                m.getAtualizadoEm());
    }
}
