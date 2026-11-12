package br.com.saluris.config;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.saluris.domain.entity.ArquivoPdf;
import br.com.saluris.domain.entity.Colaborador;
import br.com.saluris.domain.entity.Documento;
import br.com.saluris.domain.entity.Empresa;
import br.com.saluris.domain.entity.EnvioEmail;
import br.com.saluris.domain.entity.Exame;
import br.com.saluris.domain.entity.Funcao;
import br.com.saluris.domain.entity.IaConversa;
import br.com.saluris.domain.entity.IaMensagem;
import br.com.saluris.domain.entity.Laudo;
import br.com.saluris.domain.entity.LogAuditoria;
import br.com.saluris.domain.entity.MatrizExame;
import br.com.saluris.domain.entity.Notificacao;
import br.com.saluris.domain.entity.Setor;
import br.com.saluris.domain.entity.TipoDocumento;
import br.com.saluris.domain.entity.TipoExame;
import br.com.saluris.domain.entity.TipoLaudo;
import br.com.saluris.domain.entity.Unidade;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.ClassificacaoResposta;
import br.com.saluris.domain.enums.PapelMensagem;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.domain.enums.PorteEmpresa;
import br.com.saluris.domain.enums.StatusEnvioEmail;
import br.com.saluris.domain.enums.StatusFluxoExame;
import br.com.saluris.domain.enums.StatusLaudo;
import br.com.saluris.domain.enums.TipoNotificacao;
import br.com.saluris.repository.ColaboradorRepository;
import br.com.saluris.repository.DocumentoRepository;
import br.com.saluris.repository.EmpresaRepository;
import br.com.saluris.repository.EnvioEmailRepository;
import br.com.saluris.repository.ExameRepository;
import br.com.saluris.repository.FuncaoRepository;
import br.com.saluris.repository.IaConversaRepository;
import br.com.saluris.repository.IaMensagemRepository;
import br.com.saluris.repository.LaudoRepository;
import br.com.saluris.repository.LogAuditoriaRepository;
import br.com.saluris.repository.MatrizExameRepository;
import br.com.saluris.repository.NotificacaoRepository;
import br.com.saluris.repository.SetorRepository;
import br.com.saluris.repository.TipoDocumentoRepository;
import br.com.saluris.repository.TipoExameRepository;
import br.com.saluris.repository.TipoLaudoRepository;
import br.com.saluris.repository.UnidadeRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.service.ArquivoStorageService;
import br.com.saluris.shared.util.ValidadeCalculator;
import java.nio.charset.StandardCharsets;

/**
 * Popula dados de demonstração no perfil {@code local} (uma empresa cliente, usuários dos 4 perfis,
 * estrutura organizacional, matriz de periodicidade, colaboradores, exames, documentos PGR/PCMSO e laudo),
 * permitindo testar imediatamente todas as telas do sistema.
 */
@Component
@ConditionalOnProperty(name = "saluris.demo.seed", havingValue = "true")
@Order(3)
public class LocalDemoSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDemoSeedRunner.class);
    private static final String SENHA_PADRAO = "Saluris@2026!";

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final FuncaoRepository funcaoRepository;
    private final SetorRepository setorRepository;
    private final TipoExameRepository tipoExameRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final TipoLaudoRepository tipoLaudoRepository;
    private final MatrizExameRepository matrizRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final ExameRepository exameRepository;
    private final DocumentoRepository documentoRepository;
    private final LaudoRepository laudoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final IaConversaRepository iaConversaRepository;
    private final IaMensagemRepository iaMensagemRepository;
    private final LogAuditoriaRepository logAuditoriaRepository;
    private final EnvioEmailRepository envioEmailRepository;
    private final PasswordEncoder passwordEncoder;
    private final ArquivoStorageService arquivoStorageService;

    public LocalDemoSeedRunner(EmpresaRepository empresaRepository,
            UsuarioRepository usuarioRepository,
            UnidadeRepository unidadeRepository,
            FuncaoRepository funcaoRepository,
            SetorRepository setorRepository,
            TipoExameRepository tipoExameRepository,
            TipoDocumentoRepository tipoDocumentoRepository,
            TipoLaudoRepository tipoLaudoRepository,
            MatrizExameRepository matrizRepository,
            ColaboradorRepository colaboradorRepository,
            ExameRepository exameRepository,
            DocumentoRepository documentoRepository,
            LaudoRepository laudoRepository,
            NotificacaoRepository notificacaoRepository,
            IaConversaRepository iaConversaRepository,
            IaMensagemRepository iaMensagemRepository,
            LogAuditoriaRepository logAuditoriaRepository,
            EnvioEmailRepository envioEmailRepository,
            PasswordEncoder passwordEncoder,
            ArquivoStorageService arquivoStorageService) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.funcaoRepository = funcaoRepository;
        this.setorRepository = setorRepository;
        this.tipoExameRepository = tipoExameRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.tipoLaudoRepository = tipoLaudoRepository;
        this.matrizRepository = matrizRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.exameRepository = exameRepository;
        this.documentoRepository = documentoRepository;
        this.laudoRepository = laudoRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.iaConversaRepository = iaConversaRepository;
        this.iaMensagemRepository = iaMensagemRepository;
        this.logAuditoriaRepository = logAuditoriaRepository;
        this.envioEmailRepository = envioEmailRepository;
        this.passwordEncoder = passwordEncoder;
        this.arquivoStorageService = arquivoStorageService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (empresaRepository.findByCnpj("11222333000181").isEmpty()
                && usuarioRepository.findByEmailIgnoreCase("especialista@saluris.com.br").isEmpty()) {
            seedBaseDemoData();
        }
        seedComplementarSeVazio();
    }

    private void seedBaseDemoData() {
        log.info("Criando dados iniciais de demonstração...");
        String hash = passwordEncoder.encode(SENHA_PADRAO);

        Empresa emp = new Empresa();
        emp.setCnpj("11222333000181");
        emp.setRazaoSocial("Porto Logística e Operações Portuárias S.A.");
        emp.setNomeFantasia("PortoLog Santos");
        emp.setSetorAtuacao("Operações Portuárias e Logística Industrial");
        emp.setPorte(PorteEmpresa.MEDIO);
        emp.setContatoNome("Mariana Costa");
        emp.setEmailContato("contato@portolog.com.br");
        emp.setTelefone("(13) 3222-4500");
        emp.setAtiva(true);
        emp = empresaRepository.save(emp);

        Usuario especialista = criarUsuario("Carlos Eduardo (Especialista SST)", "especialista@saluris.com.br",
                hash, Perfil.ESPECIALISTA_SST, emp.getId(), null, null);
        Usuario medico = criarUsuario("Dra. Helena Prado (Médica do Trabalho)", "medico@saluris.com.br",
                hash, Perfil.MEDICO_TRABALHO, emp.getId(), "148920", "SP");
        Usuario rh = criarUsuario("Fernanda Almeida (RH)", "rh@saluris.com.br",
                hash, Perfil.RH_EMPRESA, emp.getId(), null, null);

        Unidade uSantos = criarUnidade(emp.getId(), "Terminal Margem Direita - Santos", "Santos", "SP");
        Unidade uGuaruja = criarUnidade(emp.getId(), "Pátio Logístico - Guarujá", "Guarujá", "SP");

        Funcao fOperador = criarFuncao(emp.getId(), "Operador de Empilhadeira",
                "Operação de empilhadeira elétrica e a combustão na movimentação de cargas.");
        Funcao fEletricista = criarFuncao(emp.getId(), "Eletricista de Manutenção",
                "Manutenção preventiva e corretiva em painéis e instalações elétricas industriais (NR-10 / NR-35).");
        Funcao fAnalista = criarFuncao(emp.getId(), "Analista Administrativo",
                "Atividades administrativas de escritório e controle documental.");

        Setor sOperacoes = criarSetor(emp.getId(), "Operações de Pátio",
                "Área operacional com movimentação de cargas e ruído intermitente.");
        Setor sManutencao = criarSetor(emp.getId(), "Manutenção Industrial",
                "Oficina elétrica e mecânica.");
        Setor sAdm = criarSetor(emp.getId(), "Administrativo",
                "Escritórios climatizados.");

        List<TipoExame> tiposExame = tipoExameRepository.findByAtivoTrueOrderByNomeAsc();
        TipoExame exClinico = buscarExame(tiposExame, "Clínico");
        TipoExame exAudio = buscarExame(tiposExame, "Audiometria");
        TipoExame exAcuidade = buscarExame(tiposExame, "Acuidade");
        TipoExame exEcg = buscarExame(tiposExame, "Eletrocardiograma");

        MatrizExame m1 = criarMatriz(emp.getId(), fOperador, sOperacoes, exClinico, 12, especialista);
        MatrizExame m2 = criarMatriz(emp.getId(), fOperador, sOperacoes, exAudio, 6, especialista);
        MatrizExame m3 = criarMatriz(emp.getId(), fOperador, sOperacoes, exAcuidade, 12, especialista);
        MatrizExame m4 = criarMatriz(emp.getId(), fEletricista, sManutencao, exClinico, 12, especialista);
        MatrizExame m5 = criarMatriz(emp.getId(), fEletricista, sManutencao, exEcg, 12, especialista);
        MatrizExame m6 = criarMatriz(emp.getId(), fAnalista, sAdm, exClinico, 24, especialista);

        LocalDate hoje = LocalDate.now();

        Colaborador c1 = criarColaborador(emp.getId(), "João Marcos da Silva", "52998224725",
                LocalDate.of(1989, 5, 14), hoje.minusMonths(11).minusDays(20), "joao.silva@portolog.com.br",
                uSantos, fOperador, sOperacoes, rh);
        criarExame(c1, m1, hoje.plusDays(15), StatusFluxoExame.PENDENTE, null, hoje);
        criarExame(c1, m2, hoje.minusDays(5), StatusFluxoExame.PENDENTE, null, hoje);
        ArquivoPdf pdfExame = gerarPdfDemo(emp.getId(), "aso-acuidade-joao.pdf", "Resultado Exame Acuidade Visual - Joao Marcos");
        criarExame(c1, m3, hoje.plusMonths(12), StatusFluxoExame.AGUARDANDO_ASSINATURA, pdfExame, hoje);

        Colaborador c2 = criarColaborador(emp.getId(), "Beatriz Santos Oliveira", "11144477735",
                LocalDate.of(1994, 8, 22), hoje.minusMonths(6), "beatriz.oliveira@portolog.com.br",
                uGuaruja, fEletricista, sManutencao, rh);
        criarExame(c2, m4, hoje.plusMonths(6), StatusFluxoExame.PENDENTE, null, hoje);
        criarExame(c2, m5, hoje.plusDays(7), StatusFluxoExame.AGENDADO, null, hoje);

        Colaborador c3 = criarColaborador(emp.getId(), "Rafael Pereira Souza", "98765432100",
                LocalDate.of(1997, 2, 10), hoje.minusMonths(3), "rafael.souza@portolog.com.br",
                uSantos, fAnalista, sAdm, rh);
        criarExame(c3, m6, hoje.plusMonths(21), StatusFluxoExame.PENDENTE, null, hoje);

        TipoDocumento tipoPgr = tipoDocumentoRepository.findBySiglaIgnoreCase("PGR").orElse(null);
        TipoDocumento tipoPcmso = tipoDocumentoRepository.findBySiglaIgnoreCase("PCMSO").orElse(null);
        if (tipoPgr != null && tipoPcmso != null) {
            Documento pgr = new Documento();
            pgr.setEmpresaId(emp.getId());
            pgr.setTipoDocumento(tipoPgr);
            pgr.setTitulo("PGR - Terminal Margem Direita Santos 2025/2027");
            pgr.setDescricao("Inventário de Riscos e Plano de Ação conforme NR-01.");
            pgr.setVersao(1);
            pgr.setDataEmissao(hoje.minusMonths(6));
            pgr.setDataValidade(hoje.plusMonths(18));
            pgr.setStatusValidade(ValidadeCalculator.classificar(pgr.getDataValidade(), hoje));
            pgr.setArquivo(gerarPdfDemo(emp.getId(), "PGR-PortoLog-2026.pdf", "Programa de Gerenciamento de Riscos - NR-01"));
            pgr.setEnviadoPor(especialista);
            pgr = documentoRepository.save(pgr);

            Documento pcmso = new Documento();
            pcmso.setEmpresaId(emp.getId());
            pcmso.setTipoDocumento(tipoPcmso);
            pcmso.setDocumentoBase(pgr);
            pcmso.setTitulo("PCMSO - PortoLog Santos 2026");
            pcmso.setDescricao("Programa de Controle Médico de Saúde Ocupacional baseado no PGR v1.");
            pcmso.setVersao(1);
            pcmso.setDataEmissao(hoje.minusMonths(11));
            pcmso.setDataValidade(hoje.plusDays(20));
            pcmso.setStatusValidade(ValidadeCalculator.classificar(pcmso.getDataValidade(), hoje));
            pcmso.setArquivo(gerarPdfDemo(emp.getId(), "PCMSO-PortoLog-2026.pdf", "PCMSO - NR-07"));
            pcmso.setEnviadoPor(rh);
            documentoRepository.save(pcmso);
        }

        List<TipoLaudo> tiposLaudo = tipoLaudoRepository.findByAtivoTrueOrderByNomeAsc();
        if (!tiposLaudo.isEmpty()) {
            Laudo laudo = new Laudo();
            laudo.setEmpresaId(emp.getId());
            laudo.setTipoLaudo(tiposLaudo.get(0));
            laudo.setColaborador(c1);
            laudo.setTitulo("LTCAT - Avaliação de Ruído Operações de Pátio");
            laudo.setDescricao("Avaliação quantitativa de pressão sonora (NR-15 Anexo 1).");
            laudo.setStatus(StatusLaudo.AGUARDANDO_ASSINATURA);
            laudo.setDataEmissao(hoje.minusDays(2));
            laudo.setDataValidade(hoje.plusMonths(12));
            laudo.setArquivo(gerarPdfDemo(emp.getId(), "LTCAT-Operacoes-Patio.pdf", "Laudo Tecnico LTCAT"));
            laudo.setCriadoPor(especialista);
            laudoRepository.save(laudo);
        }

        log.info("Dados de demonstração criados com sucesso! Usuários disponíveis (senha: {}): "
                + "admin@saluris.com.br, especialista@saluris.com.br, medico@saluris.com.br, rh@saluris.com.br",
                SENHA_PADRAO);
    }

    private void seedComplementarSeVazio() {
        Empresa emp = empresaRepository.findByCnpj("11222333000181").orElse(null);
        if (emp == null) {
            return;
        }
        LocalDate hoje = LocalDate.now();
        List<Usuario> usuarios = usuarioRepository.findAll().stream()
                .filter(u -> emp.getId().equals(u.getEmpresaId()))
                .toList();
        if (usuarios.isEmpty()) {
            return;
        }
        Usuario especialista = usuarios.stream()
                .filter(u -> u.getPerfil() == Perfil.ESPECIALISTA_SST)
                .findFirst().orElse(usuarios.get(0));
        Usuario medico = usuarios.stream()
                .filter(u -> u.getPerfil() == Perfil.MEDICO_TRABALHO)
                .findFirst().orElse(especialista);
        Usuario rh = usuarios.stream()
                .filter(u -> u.getPerfil() == Perfil.RH_EMPRESA)
                .findFirst().orElse(especialista);
        List<Colaborador> colaboradores = colaboradorRepository.findAll();
        Colaborador c1 = colaboradores.isEmpty() ? null : colaboradores.get(0);
        Colaborador c2 = colaboradores.size() > 1 ? colaboradores.get(1) : c1;

        if (documentoRepository.count() < 3) {
            TipoDocumento tipoLtcat = tipoDocumentoRepository.findBySiglaIgnoreCase("LTCAT").orElse(null);
            if (tipoLtcat != null) {
                Documento ltcat = new Documento();
                ltcat.setEmpresaId(emp.getId());
                ltcat.setTipoDocumento(tipoLtcat);
                ltcat.setTitulo("LTCAT - Oficina de Manutenção Industrial Guarujá");
                ltcat.setDescricao("Laudo Técnico das Condições Ambientais do Trabalho para aposentadoria especial.");
                ltcat.setVersao(1);
                ltcat.setDataEmissao(hoje.minusMonths(14));
                ltcat.setDataValidade(hoje.minusDays(8));
                ltcat.setStatusValidade(ValidadeCalculator.classificar(ltcat.getDataValidade(), hoje));
                ltcat.setArquivo(gerarPdfDemo(emp.getId(), "LTCAT-Guarani-Manutencao.pdf", "LTCAT - Manutencao Industrial"));
                ltcat.setEnviadoPor(especialista);
                documentoRepository.save(ltcat);
            }
        }

        if (exameRepository.count() < 9 && c1 != null && c2 != null) {
            List<TipoExame> tiposExame = tipoExameRepository.findByAtivoTrueOrderByNomeAsc();
            TipoExame exEspiro = buscarExame(tiposExame, "Espirometria");
            TipoExame exHemo = buscarExame(tiposExame, "Hemograma");
            TipoExame exGlic = buscarExame(tiposExame, "Glicemia");
            List<MatrizExame> matrizes = matrizRepository.findAll();
            MatrizExame mRef = matrizes.isEmpty() ? null : matrizes.get(0);
            if (mRef != null) {
                Exame eEsp = new Exame();
                eEsp.setEmpresaId(emp.getId());
                eEsp.setColaborador(c1);
                eEsp.setTipoExame(exEspiro);
                eEsp.setMatrizExame(mRef);
                eEsp.setPeriodicidadeMeses(12);
                eEsp.setDataVencimento(hoje.plusDays(25));
                eEsp.setStatusValidade(ValidadeCalculator.classificar(eEsp.getDataVencimento(), hoje));
                eEsp.setStatusFluxo(StatusFluxoExame.PENDENTE);
                exameRepository.save(eEsp);

                Exame eHem = new Exame();
                eHem.setEmpresaId(emp.getId());
                eHem.setColaborador(c2);
                eHem.setTipoExame(exHemo);
                eHem.setMatrizExame(mRef);
                eHem.setPeriodicidadeMeses(12);
                eHem.setDataVencimento(hoje.plusMonths(8));
                eHem.setStatusValidade(ValidadeCalculator.classificar(eHem.getDataVencimento(), hoje));
                eHem.setStatusFluxo(StatusFluxoExame.PENDENTE);
                exameRepository.save(eHem);

                Exame eGli = new Exame();
                eGli.setEmpresaId(emp.getId());
                eGli.setColaborador(c2);
                eGli.setTipoExame(exGlic);
                eGli.setMatrizExame(mRef);
                eGli.setPeriodicidadeMeses(12);
                eGli.setDataVencimento(hoje.minusDays(3));
                eGli.setStatusValidade(ValidadeCalculator.classificar(eGli.getDataVencimento(), hoje));
                eGli.setStatusFluxo(StatusFluxoExame.PENDENTE);
                exameRepository.save(eGli);
            }
        }

        if (laudoRepository.count() < 3) {
            List<TipoLaudo> tiposLaudo = tipoLaudoRepository.findByAtivoTrueOrderByNomeAsc();
            if (!tiposLaudo.isEmpty()) {
                TipoLaudo tPeri = tiposLaudo.size() > 2 ? tiposLaudo.get(2) : tiposLaudo.get(0);
                Laudo lHomologado = new Laudo();
                lHomologado.setEmpresaId(emp.getId());
                lHomologado.setTipoLaudo(tPeri);
                lHomologado.setColaborador(c2);
                lHomologado.setTitulo("Laudo de Periculosidade - Eletricidade (NR-16 Anexo 4)");
                lHomologado.setDescricao("Caracterização de periculosidade por exposição a sistema elétrico de potência.");
                lHomologado.setStatus(StatusLaudo.AGUARDANDO_ASSINATURA);
                lHomologado.setDataEmissao(hoje.minusMonths(3));
                lHomologado.setDataValidade(hoje.plusMonths(9));
                lHomologado.setArquivo(gerarPdfDemo(emp.getId(), "Laudo-Periculosidade-NR16.pdf", "Laudo de Periculosidade NR-16 Homologado"));
                lHomologado.setCriadoPor(especialista);
                lHomologado.homologar(medico);
                laudoRepository.save(lHomologado);

                TipoLaudo tErgo = tiposLaudo.size() > 3 ? tiposLaudo.get(3) : tiposLaudo.get(0);
                Laudo lRevisado = new Laudo();
                lRevisado.setEmpresaId(emp.getId());
                lRevisado.setTipoLaudo(tErgo);
                lRevisado.setColaborador(c1);
                lRevisado.setTitulo("AET - Análise Ergonômica Postos Administrativos (NR-17)");
                lRevisado.setDescricao("Avaliação biomecânica, mobiliário e iluminação dos escritórios.");
                lRevisado.setStatus(StatusLaudo.AGUARDANDO_ASSINATURA);
                lRevisado.setDataEmissao(hoje.minusDays(10));
                lRevisado.setDataValidade(hoje.plusMonths(12));
                lRevisado.setArquivo(gerarPdfDemo(emp.getId(), "AET-Postos-Administrativos.pdf", "AET - Analise Ergonomica NR-17"));
                lRevisado.setCriadoPor(especialista);
                lRevisado.recusar(medico, "Solicitada inclusão das medições de iluminância (luxímetro) conforme NHO-11 antes da homologação final.");
                laudoRepository.save(lRevisado);
            }
        }

        if (notificacaoRepository.count() == 0) {
            List<Exame> exames = exameRepository.findAll();
            List<Documento> docs = documentoRepository.findAll();
            List<Laudo> laudos = laudoRepository.findAll();

            for (Usuario u : usuarios) {
                if (u.getEmpresaId() == null || !u.getEmpresaId().equals(emp.getId())) {
                    continue;
                }
                Long empId = u.getEmpresaId();
                if (!exames.isEmpty()) {
                    Notificacao n1 = new Notificacao();
                    n1.setEmpresaId(empId);
                    n1.setUsuario(u);
                    n1.setExame(exames.get(0));
                    n1.setTipo(TipoNotificacao.URGENCIA);
                    n1.setMarcoDias(0);
                    n1.setDataReferencia(hoje);
                    n1.setTitulo("URGÊNCIA: Exame Ocupacional Vencido");
                    n1.setMensagem("O exame ocupacional de João Marcos da Silva encontra-se vencido. Providencie agendamento imediato conforme NR-07.");
                    notificacaoRepository.save(n1);
                }
                if (docs.size() > 1) {
                    Notificacao n2 = new Notificacao();
                    n2.setEmpresaId(empId);
                    n2.setUsuario(u);
                    n2.setDocumento(docs.get(1));
                    n2.setTipo(TipoNotificacao.PREVENTIVO);
                    n2.setMarcoDias(30);
                    n2.setDataReferencia(hoje);
                    n2.setTitulo("Alerta Preventivo (20 dias): PCMSO 2026 próximo ao vencimento");
                    n2.setMensagem("O documento PCMSO - PortoLog Santos 2026 vencerá em 20 dias. Inicie a revisão anual junto ao Médico Coordenador.");
                    notificacaoRepository.save(n2);
                }
                if (!laudos.isEmpty()) {
                    Notificacao n3 = new Notificacao();
                    n3.setEmpresaId(empId);
                    n3.setUsuario(u);
                    n3.setLaudo(laudos.get(0));
                    n3.setTipo(TipoNotificacao.LAUDO_PENDENTE_ASSINATURA);
                    n3.setDataReferencia(hoje);
                    n3.setTitulo("Novo Laudo Aguardando Assinatura Médica");
                    n3.setMensagem("O laudo LTCAT - Avaliação de Ruído Operações de Pátio foi disponibilizado na fila de homologação médica.");
                    notificacaoRepository.save(n3);
                }
            }
        }

        if (iaConversaRepository.count() == 0) {
            for (Usuario u : usuarios) {
                if (u.getEmpresaId() == null || !u.getEmpresaId().equals(emp.getId())) {
                    continue;
                }
                Long empId = u.getEmpresaId();
                IaConversa conv = new IaConversa();
                conv.setEmpresaId(empId);
                conv.setUsuario(u);
                conv.setTitulo("Periodicidade de Audiometria e PGR x PCMSO (NR-01 / NR-07)");
                conv = iaConversaRepository.save(conv);

                IaMensagem msgUser = new IaMensagem();
                msgUser.setConversa(conv);
                msgUser.setPapel(PapelMensagem.USUARIO);
                msgUser.setConteudo("Qual é a periodicidade do exame de audiometria para operadores expostos a ruído ocupacional segundo a NR-07 e como o PCMSO se relaciona com o PGR?");
                iaMensagemRepository.save(msgUser);

                IaMensagem msgAss = new IaMensagem();
                msgAss.setConversa(conv);
                msgAss.setPapel(PapelMensagem.ASSISTENTE);
                msgAss.setClassificacao(ClassificacaoResposta.RESPONDIDA);
                msgAss.setConteudo("De acordo com a **NR-07 (item 7.5.6 e Anexo II)**, o exame audiométrico deve ser realizado na admissão, **6 (seis) meses após a admissão** e, posteriormente, **anualmente** para colaboradores expostos a níveis de pressão sonora elevados.\n\n"
                        + "Além disso, conforme a **NR-01 (item 1.5.3.1)** e a **NR-07 (item 7.5.1)**, o **PCMSO** deve ser elaborado e implementado com base nos riscos ocupacionais identificados e classificados no **PGR (Programa de Gerenciamento de Riscos)**.\n\n"
                        + "*Aviso Legal (RN11): Esta orientação tem caráter estritamente informativo e normativo, não substituindo o parecer clínico ou laudo assinado pelo Médico do Trabalho.*");
                msgAss.setFontes(List.of(
                        Map.of("trecho_id", 1, "norma", "NR-07", "item", "7.5.6 / Anexo II"),
                        Map.of("trecho_id", 2, "norma", "NR-01", "item", "1.5.3.1")
                ));
                iaMensagemRepository.save(msgAss);
            }
        }

        if (logAuditoriaRepository.count() == 0) {
            criarLogAuditoria(emp.getId(), especialista.getId(), "CONFIGURAR_MATRIZ", "matriz_exames", 1L,
                    Map.of("funcao", "Operador de Empilhadeira", "periodicidadeMeses", 6));
            criarLogAuditoria(emp.getId(), rh.getId(), "CRIAR_COLABORADOR", "colaboradores", c1 != null ? c1.getId() : 1L,
                    Map.of("nome", "João Marcos da Silva", "examesGerados", 3));
            criarLogAuditoria(emp.getId(), medico.getId(), "HOMOLOGAR_LAUDO", "laudos", 2L,
                    Map.of("status", "HOMOLOGADO", "crm", "CRM/SP 148920"));
        }

        if (envioEmailRepository.count() == 0) {
            EnvioEmail em1 = new EnvioEmail();
            em1.setDestinatario("rh@saluris.com.br");
            em1.setAssunto("[SALURIS IA] URGÊNCIA: Exame de Audiometria Vencido - João Marcos da Silva");
            em1.setCorpo("Prezado(a) Gestor(a) de RH, identificamos exame ocupacional vencido na unidade Terminal Margem Direita - Santos.");
            em1.setStatus(StatusEnvioEmail.ENVIADO);
            em1.setTentativas(1);
            em1.setEnviadoEm(OffsetDateTime.now().minusHours(2));
            envioEmailRepository.save(em1);
        }
    }

    private void criarLogAuditoria(Long empresaId, Long usuarioId, String acao, String entidade, Long entidadeId, Map<String, Object> detalhes) {
        LogAuditoria logEntry = new LogAuditoria();
        logEntry.setEmpresaId(empresaId);
        logEntry.setUsuarioId(usuarioId);
        logEntry.setAcao(acao);
        logEntry.setEntidade(entidade);
        logEntry.setEntidadeId(entidadeId);
        logEntry.setDetalhes(detalhes);
        logEntry.setIpOrigem("127.0.0.1");
        logAuditoriaRepository.save(logEntry);
    }

    private Usuario criarUsuario(String nome, String email, String hash, Perfil perfil,
            Long empresaId, String crm, String crmUf) {
        return usuarioRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setNome(nome);
            u.setEmail(email);
            u.setSenhaHash(hash);
            u.setPerfil(perfil);
            u.setEmpresaId(empresaId);
            u.setCrm(crm);
            u.setCrmUf(crmUf);
            u.setDeveTrocarSenha(false);
            u.setAtivo(true);
            return usuarioRepository.save(u);
        });
    }

    private Unidade criarUnidade(Long empresaId, String nome, String cidade, String uf) {
        Unidade u = new Unidade();
        u.setEmpresaId(empresaId);
        u.setNome(nome);
        u.setCidade(cidade);
        u.setUf(uf);
        u.setAtiva(true);
        return unidadeRepository.save(u);
    }

    private Funcao criarFuncao(Long empresaId, String nome, String desc) {
        Funcao f = new Funcao();
        f.setEmpresaId(empresaId);
        f.setNome(nome);
        f.setDescricao(desc);
        f.setAtiva(true);
        return funcaoRepository.save(f);
    }

    private Setor criarSetor(Long empresaId, String nome, String desc) {
        Setor s = new Setor();
        s.setEmpresaId(empresaId);
        s.setNome(nome);
        s.setDescricao(desc);
        s.setAtivo(true);
        return setorRepository.save(s);
    }

    private static TipoExame buscarExame(List<TipoExame> lista, String trecho) {
        return lista.stream()
                .filter(t -> t.getNome().toLowerCase().contains(trecho.toLowerCase()))
                .findFirst()
                .orElse(lista.get(0));
    }

    private MatrizExame criarMatriz(Long empresaId, Funcao f, Setor s, TipoExame t, int meses, Usuario autor) {
        MatrizExame m = new MatrizExame();
        m.setEmpresaId(empresaId);
        m.setFuncao(f);
        m.setSetor(s);
        m.setTipoExame(t);
        m.setPeriodicidadeMeses(meses);
        m.setObrigatorio(true);
        m.setConfiguradoPor(autor);
        return matrizRepository.save(m);
    }

    private Colaborador criarColaborador(Long empresaId, String nome, String cpf, LocalDate nasc,
            LocalDate adm, String email, Unidade u, Funcao f, Setor s, Usuario criador) {
        Colaborador c = new Colaborador();
        c.setEmpresaId(empresaId);
        c.setNome(nome);
        c.setCpf(cpf);
        c.setDataNascimento(nasc);
        c.setDataAdmissao(adm);
        c.setEmail(email);
        c.setUnidade(u);
        c.setFuncao(f);
        c.setSetor(s);
        c.setAtivo(true);
        c.setCriadoPor(criador);
        return colaboradorRepository.save(c);
    }

    private Exame criarExame(Colaborador c, MatrizExame m, LocalDate vencimento,
            StatusFluxoExame fluxo, ArquivoPdf arquivo, LocalDate hoje) {
        Exame e = new Exame();
        e.setEmpresaId(c.getEmpresaId());
        e.setColaborador(c);
        e.setTipoExame(m.getTipoExame());
        e.setMatrizExame(m);
        e.setPeriodicidadeMeses(m.getPeriodicidadeMeses());
        e.setDataVencimento(vencimento);
        e.setStatusValidade(ValidadeCalculator.classificar(vencimento, hoje));
        e.setStatusFluxo(fluxo);
        if (fluxo == StatusFluxoExame.AGENDADO) {
            e.setDataAgendada(hoje.plusDays(5));
            e.setLocalClinica("Clínica Ocupacional MedSantos - Av. Ana Costa, 250");
        }
        if (fluxo == StatusFluxoExame.AGUARDANDO_ASSINATURA) {
            e.setDataRealizacao(hoje);
        }
        e.setArquivo(arquivo);
        return exameRepository.save(e);
    }

    private ArquivoPdf gerarPdfDemo(Long empresaId, String nomeArquivo, String texto) {
        try (var out = new java.io.ByteArrayOutputStream()) {
            var doc = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new com.lowagie.text.Paragraph("SALURIS IA - Documento Demonstrativo"));
            doc.add(new com.lowagie.text.Paragraph(texto));
            doc.close();
            return arquivoStorageService.armazenarBytesPdf(nomeArquivo, out.toByteArray(), empresaId);
        } catch (Exception e) {
            byte[] fallback = ("%PDF-1.4\n%" + texto + "\n%%EOF").getBytes(StandardCharsets.US_ASCII);
            return arquivoStorageService.armazenarBytesPdf(nomeArquivo, fallback, empresaId);
        }
    }
}
