package br.com.saluris.service;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.saluris.config.SalurisProperties;
import br.com.saluris.domain.entity.Empresa;
import br.com.saluris.domain.entity.EnvioEmail;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;
import br.com.saluris.domain.enums.StatusEnvioEmail;
import br.com.saluris.domain.enums.TipoNotificacao;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.repository.EmpresaRepository;
import br.com.saluris.repository.EnvioEmailRepository;
import br.com.saluris.repository.TipoDocumentoRepository;
import br.com.saluris.repository.TipoExameRepository;
import br.com.saluris.repository.TipoLaudoRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.service.NotificacaoService.NotificacaoResponse;
import br.com.saluris.shared.dto.PageResponse;

/**
 * Gera e controla notificações de configurações e possíveis problemas do sistema
 * exclusivas para o perfil Administrador (T.I.), sem expor dados/alertas operacionais de empresas.
 */
@Service
public class AdminSistemaNotificacaoService {

    private record AdminAlertaDefinicao(
            long id,
            String chave,
            TipoNotificacao tipo,
            String titulo,
            String mensagem,
            OffsetDateTime criadoEm) {
    }

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnvioEmailRepository envioEmailRepository;
    private final TipoExameRepository tipoExameRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final TipoLaudoRepository tipoLaudoRepository;
    private final SalurisProperties salurisProperties;
    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final String smtpHost;

    private volatile boolean tableReady = false;

    public AdminSistemaNotificacaoService(
            EmpresaRepository empresaRepository,
            UsuarioRepository usuarioRepository,
            EnvioEmailRepository envioEmailRepository,
            TipoExameRepository tipoExameRepository,
            TipoDocumentoRepository tipoDocumentoRepository,
            TipoLaudoRepository tipoLaudoRepository,
            SalurisProperties salurisProperties,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            @Value("${spring.mail.host:}") String smtpHost) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.envioEmailRepository = envioEmailRepository;
        this.tipoExameRepository = tipoExameRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.tipoLaudoRepository = tipoLaudoRepository;
        this.salurisProperties = salurisProperties;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
        this.smtpHost = smtpHost;
    }

    public PageResponse<NotificacaoResponse> listar(Long usuarioId, Pageable pageable) {
        List<NotificacaoResponse> todas = montarRespostas(usuarioId);
        int page = pageable.isPaged() ? pageable.getPageNumber() : 0;
        int size = pageable.isPaged() ? pageable.getPageSize() : Math.max(1, todas.size());
        int from = Math.min(page * size, todas.size());
        int to = Math.min(from + size, todas.size());
        List<NotificacaoResponse> slice = todas.subList(from, to);
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) todas.size() / size);
        return new PageResponse<>(slice, page, size, todas.size(), totalPages);
    }

    public long contarNaoLidas(Long usuarioId) {
        return montarRespostas(usuarioId).stream()
                .filter(n -> !n.lida())
                .count();
    }

    public void marcarComoLida(Long usuarioId, Long notificacaoId) {
        List<AdminAlertaDefinicao> alertas = gerarAlertasSistema();
        AdminAlertaDefinicao alvo = alertas.stream()
                .filter(a -> Objects.equals(a.id(), notificacaoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada."));
        registrarChaveLida(usuarioId, alvo.chave());
    }

    public int marcarTodasComoLidas(Long usuarioId) {
        Set<String> jaLidas = carregarChavesLidas(usuarioId);
        int marcadas = 0;
        for (AdminAlertaDefinicao alerta : gerarAlertasSistema()) {
            if (!jaLidas.contains(alerta.chave())) {
                registrarChaveLida(usuarioId, alerta.chave());
                marcadas++;
            }
        }
        return marcadas;
    }

    private List<NotificacaoResponse> montarRespostas(Long usuarioId) {
        Set<String> chavesLidas = carregarChavesLidas(usuarioId);
        return gerarAlertasSistema().stream()
                .map(a -> new NotificacaoResponse(
                        a.id(),
                        a.tipo(),
                        a.titulo(),
                        a.mensagem(),
                        null,
                        chavesLidas.contains(a.chave()),
                        a.criadoEm(),
                        null,
                        null,
                        null,
                        null))
                .toList();
    }

    private List<AdminAlertaDefinicao> gerarAlertasSistema() {
        List<AdminAlertaDefinicao> alertas = new ArrayList<>();
        OffsetDateTime agora = OffsetDateTime.now();

        adicionarAlertasEmail(alertas);
        adicionarAlertasEmpresasEContas(alertas, agora);
        adicionarAlertasConfiguracoes(alertas, agora);

        alertas.sort(Comparator
                .comparing((AdminAlertaDefinicao a) -> a.tipo() == TipoNotificacao.URGENCIA ? 0 : 1)
                .thenComparing(AdminAlertaDefinicao::criadoEm, Comparator.reverseOrder()));
        return alertas;
    }

    private void adicionarAlertasEmail(List<AdminAlertaDefinicao> alertas) {
        List<EnvioEmail> envios = envioEmailRepository.findAll();

        List<EnvioEmail> falhasDefinitivas = envios.stream()
                .filter(e -> e.getStatus() == StatusEnvioEmail.FALHA_DEFINITIVA)
                .sorted(Comparator.comparing(EnvioEmail::getId, Comparator.reverseOrder()))
                .toList();
        if (!falhasDefinitivas.isEmpty()) {
            EnvioEmail ultimo = falhasDefinitivas.get(0);
            String erro = resumir(ultimo.getUltimoErro(), "Erro desconhecido no servidor SMTP.");
            alertas.add(criarAlerta(
                    "email_falha_definitiva_" + ultimo.getId(),
                    TipoNotificacao.URGENCIA,
                    "Problema de Envio: Falha definitiva em e-mails (" + falhasDefinitivas.size() + ")",
                    falhasDefinitivas.size() + " e-mail(s) atingiram o limite máximo de tentativas. Último destinatário: "
                            + ultimo.getDestinatario() + ". Motivo: " + erro,
                    dataSegura(ultimo.getAtualizadoEm(), ultimo.getCriadoEm())));
        }

        List<EnvioEmail> falhasTemporarias = envios.stream()
                .filter(e -> e.getStatus() == StatusEnvioEmail.PENDENTE
                        && e.getTentativas() != null && e.getTentativas() > 0
                        && e.getUltimoErro() != null && !e.getUltimoErro().isBlank())
                .sorted(Comparator.comparing(EnvioEmail::getId, Comparator.reverseOrder()))
                .toList();
        if (!falhasTemporarias.isEmpty()) {
            EnvioEmail ultimo = falhasTemporarias.get(0);
            String erro = resumir(ultimo.getUltimoErro(), "Falha temporária de conexão SMTP.");
            alertas.add(criarAlerta(
                    "email_falha_temporaria_" + ultimo.getId(),
                    TipoNotificacao.URGENCIA,
                    "Problema de Envio: E-mails aguardando reenvio (" + falhasTemporarias.size() + ")",
                    falhasTemporarias.size() + " envio(s) falharam na tentativa inicial e estão em fila de retry. Erro: " + erro,
                    dataSegura(ultimo.getAtualizadoEm(), ultimo.getCriadoEm())));
        }
    }

    private void adicionarAlertasEmpresasEContas(List<AdminAlertaDefinicao> alertas, OffsetDateTime agora) {
        List<Empresa> empresas = empresaRepository.findAll();
        List<Usuario> usuarios = usuarioRepository.findAll();

        Map<Long, List<Usuario>> usuariosAtivosPorEmpresa = usuarios.stream()
                .filter(u -> u.isAtivo() && u.getEmpresaId() != null)
                .collect(Collectors.groupingBy(Usuario::getEmpresaId));

        for (Empresa emp : empresas) {
            if (emp.isAtiva() && usuariosAtivosPorEmpresa.getOrDefault(emp.getId(), List.of()).isEmpty()) {
                String nome = emp.getNomeFantasia() != null && !emp.getNomeFantasia().isBlank()
                        ? emp.getNomeFantasia()
                        : emp.getRazaoSocial();
                alertas.add(criarAlerta(
                        "empresa_sem_usuarios_" + emp.getId(),
                        TipoNotificacao.URGENCIA,
                        "Conta Incompleta: Empresa sem usuários ativos (" + nome + ")",
                        "A empresa " + emp.getRazaoSocial()
                                + " está ativa, mas não possui nenhuma conta de acesso ativa vinculada. Acesse Usuários para criar os perfis.",
                        dataSegura(emp.getCriadoEm(), agora)));
            }
        }

        List<Empresa> inativas = empresas.stream().filter(e -> !e.isAtiva()).toList();
        if (!inativas.isEmpty()) {
            String nomes = inativas.stream()
                    .limit(3)
                    .map(e -> e.getNomeFantasia() != null ? e.getNomeFantasia() : e.getRazaoSocial())
                    .collect(Collectors.joining(", "));
            if (inativas.size() > 3) {
                nomes += " e mais " + (inativas.size() - 3);
            }
            alertas.add(criarAlerta(
                    "empresas_inativas_" + inativas.size(),
                    TipoNotificacao.PREVENTIVO,
                    "Configuração de Contas: " + inativas.size() + " empresa(s) inativa(s)",
                    "Empresas atualmente desativadas no sistema: " + nomes
                            + ". Verifique na tela Empresas caso alguma conta deva ser reativada.",
                    agora));
        }

        List<Usuario> trocaPendente = usuarios.stream()
                .filter(u -> u.isAtivo() && u.getPerfil() != Perfil.ADMINISTRADOR && u.isDeveTrocarSenha())
                .toList();
        if (!trocaPendente.isEmpty()) {
            String emails = trocaPendente.stream()
                    .limit(3)
                    .map(Usuario::getEmail)
                    .collect(Collectors.joining(", "));
            if (trocaPendente.size() > 3) {
                emails += " e mais " + (trocaPendente.size() - 3);
            }
            alertas.add(criarAlerta(
                    "usuarios_troca_senha_pendente_" + trocaPendente.size(),
                    TipoNotificacao.PREVENTIVO,
                    "Contas Pendentes: " + trocaPendente.size() + " usuário(s) aguardando primeiro acesso",
                    "As seguintes contas ainda não concluíram a troca obrigatória de senha inicial: " + emails
                            + ". Caso o usuário não tenha recebido a senha, redefina em Usuários.",
                    agora));
        }
    }

    private void adicionarAlertasConfiguracoes(List<AdminAlertaDefinicao> alertas, OffsetDateTime agora) {
        if (smtpHost == null || smtpHost.isBlank()) {
            alertas.add(criarAlerta(
                    "config_smtp_ausente",
                    TipoNotificacao.PREVENTIVO,
                    "Configuração de Sistema: Servidor SMTP não configurado",
                    "A variável SMTP_HOST não está definida. Os e-mails de boas-vindas e senhas temporárias estão sendo apenas registrados em log simulado.",
                    agora));
        }

        String openAiKey = salurisProperties != null && salurisProperties.ia() != null
                ? salurisProperties.ia().apiKey()
                : null;
        if (openAiKey == null || openAiKey.isBlank()) {
            alertas.add(criarAlerta(
                    "config_ia_offline",
                    TipoNotificacao.PREVENTIVO,
                    "Configuração de Sistema: Integração OpenAI em modo offline",
                    "A chave OPENAI_API_KEY não está configurada nas variáveis de ambiente. O Agente IA está operando apenas com consulta local às NRs.",
                    agora));
        }

        List<String> catalogosVazios = new ArrayList<>();
        if (tipoExameRepository.findByAtivoTrueOrderByNomeAsc().isEmpty()) {
            catalogosVazios.add("Tipos de Exame");
        }
        if (tipoDocumentoRepository.findByAtivoTrueOrderBySiglaAsc().isEmpty()) {
            catalogosVazios.add("Tipos de Documento");
        }
        if (tipoLaudoRepository.findByAtivoTrueOrderByNomeAsc().isEmpty()) {
            catalogosVazios.add("Tipos de Laudo");
        }
        if (!catalogosVazios.isEmpty()) {
            alertas.add(criarAlerta(
                    "config_catalogo_vazio_" + String.join("_", catalogosVazios),
                    TipoNotificacao.URGENCIA,
                    "Problema de Configuração: Catálogo(s) global(is) de SST sem itens ativos",
                    "Os catálogos " + String.join(", ", catalogosVazios)
                            + " estão vazios. Acesse Catálogos de SST para habilitar os itens antes do uso pelas empresas.",
                    agora));
        }
    }

    private Set<String> carregarChavesLidas(Long usuarioId) {
        ensureTableReady();
        List<String> chaves = jdbcTemplate.queryForList(
                "SELECT alerta_chave FROM notificacoes_admin_lidas WHERE usuario_id = ?",
                String.class,
                usuarioId);
        return new HashSet<>(chaves);
    }

    private void registrarChaveLida(Long usuarioId, String chave) {
        ensureTableReady();
        Integer existentes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notificacoes_admin_lidas WHERE usuario_id = ? AND alerta_chave = ?",
                Integer.class,
                usuarioId,
                chave);
        if (existentes == null || existentes == 0) {
            jdbcTemplate.update(
                    "INSERT INTO notificacoes_admin_lidas (usuario_id, alerta_chave, lida_em) VALUES (?, ?, ?)",
                    usuarioId,
                    chave,
                    Timestamp.from(OffsetDateTime.now().toInstant()));
        }
    }

    private void ensureTableReady() {
        if (tableReady) {
            return;
        }
        synchronized (this) {
            if (tableReady) {
                return;
            }
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.setReadOnly(false);
            tx.executeWithoutResult(status -> jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS notificacoes_admin_lidas (
                        usuario_id BIGINT NOT NULL,
                        alerta_chave VARCHAR(160) NOT NULL,
                        lida_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (usuario_id, alerta_chave)
                    )
                    """));
            tableReady = true;
        }
    }

    private static AdminAlertaDefinicao criarAlerta(String chave, TipoNotificacao tipo, String titulo,
            String mensagem, OffsetDateTime criadoEm) {
        return new AdminAlertaDefinicao(idDeterministico(chave), chave, tipo, titulo, mensagem, criadoEm);
    }

    private static long idDeterministico(String chave) {
        long hash = 1469598103934665603L;
        for (int i = 0; i < chave.length(); i++) {
            hash ^= chave.charAt(i);
            hash *= 1099511628211L;
        }
        long positivo = (hash & 0xFFFFFFFFFFL) + 1L;
        return -positivo;
    }

    private static OffsetDateTime dataSegura(OffsetDateTime primaria, OffsetDateTime fallback) {
        if (primaria != null) {
            return primaria;
        }
        return fallback != null ? fallback : OffsetDateTime.now();
    }

    private static String resumir(String texto, String fallback) {
        if (texto == null || texto.isBlank()) {
            return fallback;
        }
        String limpo = texto.trim();
        return limpo.length() <= 180 ? limpo : limpo.substring(0, 177) + "...";
    }
}
