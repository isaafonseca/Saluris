package br.com.saluris.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.Colaborador;
import br.com.saluris.domain.entity.Exame;
import br.com.saluris.domain.entity.Funcao;
import br.com.saluris.domain.entity.MatrizExame;
import br.com.saluris.domain.entity.Setor;
import br.com.saluris.domain.entity.Unidade;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.domain.enums.StatusFluxoExame;
import br.com.saluris.domain.enums.StatusValidade;
import br.com.saluris.domain.enums.TipoNotificacao;
import br.com.saluris.exception.ConflitoException;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.exception.RegraNegocioException;
import br.com.saluris.repository.ColaboradorRepository;
import br.com.saluris.repository.ExameRepository;
import br.com.saluris.repository.FuncaoRepository;
import br.com.saluris.repository.MatrizExameRepository;
import br.com.saluris.repository.SetorRepository;
import br.com.saluris.repository.UnidadeRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.shared.dto.PageResponse;
import br.com.saluris.shared.util.ValidadeCalculator;
import br.com.saluris.shared.validation.Cpf;
import br.com.saluris.shared.validation.DocumentosBr;
import br.com.saluris.tenant.TenantContext;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Gestão de colaboradores e cálculo automático de exames obrigatórios por Função + Setor
 * (Quadro 13 / RN03 / RN04 / RF22-RF24).
 */
@Service
public class ColaboradorService {

    public record ColaboradorCreateRequest(
            @NotBlank(message = "Informe o nome do colaborador") @Size(max = 150) String nome,
            @NotBlank(message = "Informe o CPF") @Cpf String cpf,
            LocalDate dataNascimento,
            @NotNull(message = "Informe a data de admissão") LocalDate dataAdmissao,
            @Email(message = "E-mail inválido") @Size(max = 255) String email,
            @NotNull(message = "Informe a unidade") Long unidadeId,
            @NotNull(message = "Informe a função") Long funcaoId,
            @NotNull(message = "Informe o setor") Long setorId) {
    }

    public record ColaboradorUpdateRequest(
            @NotBlank(message = "Informe o nome do colaborador") @Size(max = 150) String nome,
            @NotBlank(message = "Informe o CPF") @Cpf String cpf,
            LocalDate dataNascimento,
            @NotNull(message = "Informe a data de admissão") LocalDate dataAdmissao,
            LocalDate dataDemissao,
            @Email(message = "E-mail inválido") @Size(max = 255) String email,
            @NotNull(message = "Informe a unidade") Long unidadeId,
            @NotNull(message = "Informe a função") Long funcaoId,
            @NotNull(message = "Informe o setor") Long setorId,
            Boolean ativo) {
    }

    public record ColaboradorResponse(
            Long id,
            String nome,
            String cpf,
            LocalDate dataNascimento,
            LocalDate dataAdmissao,
            LocalDate dataDemissao,
            String email,
            boolean ativo,
            Long unidadeId,
            String unidadeNome,
            Long funcaoId,
            String funcaoNome,
            Long setorId,
            String setorNome,
            StatusValidade statusConformidade) {
    }

    public record ColaboradorDetalheResponse(
            ColaboradorResponse colaborador,
            List<ExameService.ExameResponse> exames) {
    }

    public record ResultadoColaboradorResponse(
            ColaboradorResponse colaborador,
            List<ExameService.ExameResponse> examesGerados,
            boolean periodicidadeConfigurada) {
    }

    private final ColaboradorRepository colaboradorRepository;
    private final UnidadeRepository unidadeRepository;
    private final FuncaoRepository funcaoRepository;
    private final SetorRepository setorRepository;
    private final MatrizExameRepository matrizRepository;
    private final ExameRepository exameRepository;
    private final UsuarioRepository usuarioRepository;
    private final ExameService exameService;
    private final NotificacaoService notificacaoService;

    public ColaboradorService(ColaboradorRepository colaboradorRepository,
            UnidadeRepository unidadeRepository,
            FuncaoRepository funcaoRepository,
            SetorRepository setorRepository,
            MatrizExameRepository matrizRepository,
            ExameRepository exameRepository,
            UsuarioRepository usuarioRepository,
            ExameService exameService,
            NotificacaoService notificacaoService) {
        this.colaboradorRepository = colaboradorRepository;
        this.unidadeRepository = unidadeRepository;
        this.funcaoRepository = funcaoRepository;
        this.setorRepository = setorRepository;
        this.matrizRepository = matrizRepository;
        this.exameRepository = exameRepository;
        this.usuarioRepository = usuarioRepository;
        this.exameService = exameService;
        this.notificacaoService = notificacaoService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ColaboradorResponse> listar(Long empresaIdParam, String busca, Pageable pageable) {
        Long empresaId = TenantContext.resolverEmpresaIdOpcional(empresaIdParam);
        Page<Colaborador> page;
        if (busca != null && !busca.isBlank()) {
            String digitos = DocumentosBr.somenteDigitos(busca);
            page = colaboradorRepository.buscarPorEmpresa(
                    empresaId, busca.trim(), digitos.isEmpty() ? "__SEM_CPF__" : digitos, pageable);
        } else if (empresaId != null) {
            page = colaboradorRepository.findByEmpresaId(empresaId, pageable);
        } else {
            page = colaboradorRepository.findAll(pageable);
        }

        List<Long> ids = page.getContent().stream().map(Colaborador::getId).toList();
        Map<Long, StatusValidade> piorPorColab = calcularPiorStatusPorColaborador(ids);
        return PageResponse.de(page, c -> paraResposta(c, piorPorColab.get(c.getId())));
    }

    @Transactional(readOnly = true)
    public ColaboradorDetalheResponse detalhar(Long id) {
        Colaborador c = buscarDoTenant(id);
        LocalDate hoje = LocalDate.now();
        List<Exame> exames = exameRepository.findByColaboradorIdOrderByDataVencimentoAsc(c.getId());
        StatusValidade pior = calcularPiorStatus(exames);
        List<ExameService.ExameResponse> examesResp = exames.stream()
                .map(e -> ExameService.paraResposta(e, hoje))
                .toList();
        return new ColaboradorDetalheResponse(paraResposta(c, pior), examesResp);
    }

    @Transactional
    public ResultadoColaboradorResponse criar(ColaboradorCreateRequest req) {
        Long empresaId = TenantContext.resolverEmpresaId(null);
        String cpf = DocumentosBr.somenteDigitos(req.cpf());
        if (colaboradorRepository.existsByEmpresaIdAndCpf(empresaId, cpf)) {
            throw new ConflitoException("Já existe um colaborador cadastrado com este CPF na empresa.");
        }
        validarDatas(req.dataNascimento(), req.dataAdmissao(), null);

        Unidade unidade = unidadeRepository.findByIdAndEmpresaId(req.unidadeId(), empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada."));
        Funcao funcao = funcaoRepository.findByIdAndEmpresaId(req.funcaoId(), empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Função não encontrada."));
        Setor setor = setorRepository.findByIdAndEmpresaId(req.setorId(), empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado."));
        Usuario criador = usuarioRepository.findById(TenantContext.exigirUsuario().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        Colaborador c = new Colaborador();
        c.setEmpresaId(empresaId);
        c.setNome(req.nome().trim());
        c.setCpf(cpf);
        c.setDataNascimento(req.dataNascimento());
        c.setDataAdmissao(req.dataAdmissao());
        c.setEmail(req.email() != null && !req.email().isBlank() ? req.email().trim().toLowerCase() : null);
        c.setUnidade(unidade);
        c.setFuncao(funcao);
        c.setSetor(setor);
        c.setAtivo(true);
        c.setCriadoPor(criador);

        Colaborador salvo = colaboradorRepository.save(c);
        return gerarExamesDaMatriz(salvo, true);
    }

    @Transactional
    public ResultadoColaboradorResponse atualizar(Long id, ColaboradorUpdateRequest req) {
        Colaborador c = buscarDoTenant(id);
        Long empresaId = c.getEmpresaId();
        String cpf = DocumentosBr.somenteDigitos(req.cpf());
        colaboradorRepository.findByEmpresaIdAndCpf(empresaId, cpf)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new ConflitoException("Já existe outro colaborador com este CPF na empresa.");
                });
        validarDatas(req.dataNascimento(), req.dataAdmissao(), req.dataDemissao());

        Unidade unidade = unidadeRepository.findByIdAndEmpresaId(req.unidadeId(), empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada."));
        Funcao funcao = funcaoRepository.findByIdAndEmpresaId(req.funcaoId(), empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Função não encontrada."));
        Setor setor = setorRepository.findByIdAndEmpresaId(req.setorId(), empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado."));

        boolean mudouFuncaoOuSetor = !c.getFuncao().getId().equals(funcao.getId())
                || !c.getSetor().getId().equals(setor.getId());

        c.setNome(req.nome().trim());
        c.setCpf(cpf);
        c.setDataNascimento(req.dataNascimento());
        c.setDataAdmissao(req.dataAdmissao());
        c.setDataDemissao(req.dataDemissao());
        c.setEmail(req.email() != null && !req.email().isBlank() ? req.email().trim().toLowerCase() : null);
        c.setUnidade(unidade);
        c.setFuncao(funcao);
        c.setSetor(setor);
        if (req.ativo() != null) {
            c.setAtivo(req.ativo());
        }

        if (mudouFuncaoOuSetor && c.isAtivo()) {
            return sincronizarMudancaFuncaoSetor(c);
        }
        List<Exame> exames = exameRepository.findByColaboradorIdOrderByDataVencimentoAsc(c.getId());
        List<MatrizExame> matriz = matrizRepository.findByEmpresaIdAndFuncaoIdAndSetorId(
                empresaId, c.getFuncao().getId(), c.getSetor().getId());
        return new ResultadoColaboradorResponse(
                paraResposta(c, calcularPiorStatus(exames)),
                List.of(),
                !matriz.isEmpty());
    }

    private ResultadoColaboradorResponse gerarExamesDaMatriz(Colaborador c, boolean novoCadastro) {
        List<MatrizExame> itens = matrizRepository.findByEmpresaIdAndFuncaoIdAndSetorId(
                        c.getEmpresaId(), c.getFuncao().getId(), c.getSetor().getId())
                .stream()
                .filter(MatrizExame::isObrigatorio)
                .toList();

        LocalDate hoje = LocalDate.now();
        List<Exame> gerados = new ArrayList<>();

        if (itens.isEmpty()) {
            String titulo = "Periodicidade não configurada: " + c.getFuncao().getNome() + " / " + c.getSetor().getNome();
            String msg = "O colaborador " + c.getNome() + " foi vinculado à função '" + c.getFuncao().getNome()
                    + "' e setor '" + c.getSetor().getNome()
                    + "', que ainda não possuem periodicidade de exames configurada na matriz.";
            notificacaoService.notificarPerfisDaEmpresa(c.getEmpresaId(),
                    Set.of(Perfil.ESPECIALISTA_SST),
                    TipoNotificacao.PERIODICIDADE_NAO_CONFIGURADA, titulo, msg, null, null, null, c);
            return new ResultadoColaboradorResponse(paraResposta(c, null), List.of(), false);
        }

        for (MatrizExame item : itens) {
            LocalDate base = novoCadastro ? c.getDataAdmissao()
                    : exameService.obterDataBaseNovoExame(c, item.getTipoExame());
            Exame ex = exameService.criarCicloPendente(c, item, base, hoje);
            gerados.add(ex);
        }

        StatusValidade pior = calcularPiorStatus(gerados);
        List<ExameService.ExameResponse> resp = gerados.stream()
                .map(e -> ExameService.paraResposta(e, hoje))
                .toList();
        return new ResultadoColaboradorResponse(paraResposta(c, pior), resp, true);
    }

    private ResultadoColaboradorResponse sincronizarMudancaFuncaoSetor(Colaborador c) {
        List<MatrizExame> novaMatriz = matrizRepository.findByEmpresaIdAndFuncaoIdAndSetorId(
                        c.getEmpresaId(), c.getFuncao().getId(), c.getSetor().getId())
                .stream()
                .filter(MatrizExame::isObrigatorio)
                .toList();

        Map<Long, MatrizExame> porTipo = novaMatriz.stream()
                .collect(Collectors.toMap(m -> m.getTipoExame().getId(), m -> m));

        List<Exame> todosExames = exameRepository.findByColaboradorIdOrderByDataVencimentoAsc(c.getId());
        List<Exame> abertos = todosExames.stream()
                .filter(e -> ExameService.STATUS_EM_ABERTO.contains(e.getStatusFluxo()))
                .toList();

        LocalDate hoje = LocalDate.now();
        List<Exame> novosGerados = new ArrayList<>();

        for (Exame ex : abertos) {
            MatrizExame regra = porTipo.get(ex.getTipoExame().getId());
            if (regra != null) {
                LocalDate base = exameService.obterDataBaseRecalculo(ex);
                LocalDate venc = base.plusMonths(regra.getPeriodicidadeMeses());
                ex.setMatrizExame(regra);
                ex.setPeriodicidadeMeses(regra.getPeriodicidadeMeses());
                ex.setDataVencimento(venc);
                ex.setStatusValidade(ValidadeCalculator.classificar(venc, hoje));
            } else if ((ex.getStatusFluxo() == StatusFluxoExame.PENDENTE
                    || ex.getStatusFluxo() == StatusFluxoExame.AGENDADO)
                    && (ex.getArquivo() == null || ex.getArquivo().getPath() == null)) {
                exameRepository.delete(ex);
            }
        }

        Set<Long> tiposAbertos = abertos.stream()
                .map(e -> e.getTipoExame().getId())
                .collect(Collectors.toSet());

        for (MatrizExame item : novaMatriz) {
            if (!tiposAbertos.contains(item.getTipoExame().getId())) {
                LocalDate base = exameService.obterDataBaseNovoExame(c, item.getTipoExame());
                novosGerados.add(exameService.criarCicloPendente(c, item, base, hoje));
            }
        }

        if (novaMatriz.isEmpty()) {
            String titulo = "Periodicidade não configurada: " + c.getFuncao().getNome() + " / " + c.getSetor().getNome();
            String msg = "O colaborador " + c.getNome() + " foi transferido para a função '" + c.getFuncao().getNome()
                    + "' e setor '" + c.getSetor().getNome() + "', sem periodicidade configurada na matriz.";
            notificacaoService.notificarPerfisDaEmpresa(c.getEmpresaId(),
                    Set.of(Perfil.ESPECIALISTA_SST),
                    TipoNotificacao.PERIODICIDADE_NAO_CONFIGURADA, titulo, msg, null, null, null, c);
        }

        List<Exame> atualizados = exameRepository.findByColaboradorIdOrderByDataVencimentoAsc(c.getId());
        StatusValidade pior = calcularPiorStatus(atualizados);
        List<ExameService.ExameResponse> geradosResp = novosGerados.stream()
                .map(e -> ExameService.paraResposta(e, hoje))
                .toList();
        return new ResultadoColaboradorResponse(paraResposta(c, pior), geradosResp, !novaMatriz.isEmpty());
    }

    private Map<Long, StatusValidade> calcularPiorStatusPorColaborador(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Exame> abertos = exameRepository.findByColaboradorIdInAndStatusFluxoNot(ids, StatusFluxoExame.HOMOLOGADO);
        Map<Long, StatusValidade> mapa = new HashMap<>();
        for (Exame e : abertos) {
            Long cid = e.getColaborador().getId();
            StatusValidade atual = mapa.get(cid);
            if (atual == null || prioridade(e.getStatusValidade()) > prioridade(atual)) {
                mapa.put(cid, e.getStatusValidade());
            }
        }
        return mapa;
    }

    private static StatusValidade calcularPiorStatus(List<Exame> exames) {
        StatusValidade pior = null;
        for (Exame e : exames) {
            if (e.getStatusFluxo() == StatusFluxoExame.HOMOLOGADO) {
                continue;
            }
            if (pior == null || prioridade(e.getStatusValidade()) > prioridade(pior)) {
                pior = e.getStatusValidade();
            }
        }
        return pior;
    }

    private static int prioridade(StatusValidade s) {
        return switch (s) {
            case EM_DIA -> 1;
            case PROXIMO_VENCIMENTO -> 2;
            case VENCIDO -> 3;
        };
    }

    private static void validarDatas(LocalDate nascimento, LocalDate admissao, LocalDate demissao) {
        if (nascimento != null && !admissao.isAfter(nascimento)) {
            throw new RegraNegocioException("A data de admissão deve ser posterior à data de nascimento.");
        }
        if (demissao != null && demissao.isBefore(admissao)) {
            throw new RegraNegocioException("A data de demissão não pode ser anterior à data de admissão.");
        }
    }

    public Colaborador buscarDoTenant(Long id) {
        if (TenantContext.isAdministrador()) {
            return colaboradorRepository.findById(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Colaborador não encontrado."));
        }
        Long empresaId = TenantContext.resolverEmpresaId(null);
        return colaboradorRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Colaborador não encontrado."));
    }

    public static ColaboradorResponse paraResposta(Colaborador c, StatusValidade statusConformidade) {
        return new ColaboradorResponse(
                c.getId(),
                c.getNome(),
                c.getCpf(),
                c.getDataNascimento(),
                c.getDataAdmissao(),
                c.getDataDemissao(),
                c.getEmail(),
                c.isAtivo(),
                c.getUnidade().getId(),
                c.getUnidade().getNome(),
                c.getFuncao().getId(),
                c.getFuncao().getNome(),
                c.getSetor().getId(),
                c.getSetor().getNome(),
                statusConformidade);
    }
}
