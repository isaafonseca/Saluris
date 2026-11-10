package br.com.saluris.service;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util. LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.saluris.config.SalurisProperties;
import br.com.saluris.domain.entity.IaConversa;
import br.com.saluris.domain.entity.IaMensagem;
import br.com.saluris.domain.entity.NrTrecho;
import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.ClassificacaoResposta;
import br.com.saluris.domain.enums.PapelMensagem;
import br.com.saluris.exception.RecursoNaoEncontradoException;
import br.com.saluris.repository.IaConversaRepository;
import br.com.saluris.repository.IaMensagemRepository;
import br.com.saluris.repository.NrTrechoRepository;
import br.com.saluris.repository.UsuarioRepository;
import br.com.saluris.tenant.TenantContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Agente IA especialista em Normas Regulamentadoras (NRs) e SST (Quadro 15 / RN07 / RF27-RF29).
 * <ul>
 *   <li>Busca trechos normativos relevantes na base {@code nr_trechos} (RAG).</li>
 *   <li>Bloqueia solicitações de emissão de diagnósticos, pareceres médicos, atestados ou laudos técnicos.</li>
 *   <li>Recusa perguntas fora do escopo de SST/NRs.</li>
 *   <li>Opera com a API da OpenAI quando {@code OPENAI_API_KEY} está configurada, ou em modo consultivo local (OFFLINE)
 *       baseado na base normativa quando não há chave configurada.</li>
 * </ul>
 */
@Service
public class AgenteIaService {

    private static final Logger log = LoggerFactory.getLogger(AgenteIaService.class);

    private static final Pattern PADRAO_NR = Pattern.compile("\\bnr[-\\s]?0?(\\d{1,2})\\b", Pattern.CASE_INSENSITIVE);

    private static final List<Pattern> PADROES_RECUSA_LAUDO_DIAGNOSTICO = List.of(
            Pattern.compile("\\b(emit[aei]\\w*|ger[aei]\\w*| assin[aei]\\w*|fa[cç][ao]|redij[ao]|elabor[aei]\\w*)\\b.{0,40}\\b(laudo|aso|atestado|parecer\\s+m[eé]dico|diagn[oó]stico|receita)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(laudo|aso|atestado|parecer\\s+m[eé]dico|diagn[oó]stico)\\b.{0,30}\\b(pronto|oficial|assinado|para\\s+o\\s+colaborador|para\\s+o\\s+paciente)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(diagnosticar|prescrever|receitar\\s+medicamento|atestar\\s+aptid[aã]o|declarar\\s+apto|declarar\\s+inapto)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(qual\\s+o\\s+diagn[oó]stico|me\\s+d[eê]\\s+um\\s+diagn[oó]stico|d[eê]\\s+um\\s+parecer\\s+m[eé]dico)\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final Set<String> STOPWORDS = Set.of(
            "para", "como", "qual", "quais", "quando", "onde", "porque", "sobre", "entre",
            "deve", "devem", "pode", "podem", "uma", "uns", "umas", "dos", "das", "nos", "nas",
            "pelo", "pela", "pelos", "pelas", "este", "esta", "isso", "esse", "essa", "sao",
            "ser", "ter", "foi", "era", "mais", "menos", "muito", "todos", "todas", "cada"
    );

    private static final Set<String> TERMOS_DOMINIO_SST = Set.of(
            "sst", "nr", "norma", "regulamentadora", "pgr", "pcmso", "ltcat", "aso", "exame", "exames",
            "periodico", "admissional", "demissional", "audiometria", "epi", "ca", "cipa", "sesmt",
            "insalubridade", "periculosidade", "ergonomia", "aet", "aep", "risco", "riscos", "ocupacional",
            "trabalho", "trabalhador", "colaborador", "seguranca", "saude", "treinamento", "capacitacao",
            "altura", "eletricidade", "ruido", "confinado", "incendi", "esocial", "gro", "laudo", "medico"
    );

    private static final String SYSTEM_PROMPT = """
            Você é o Assistente Especialista em Saúde e Segurança do Trabalho (SST) e Normas Regulamentadoras (NRs) brasileiras do sistema Saluris IA.
            Regras obrigatórias e inegociáveis (RN07 / RF27-RF29):
            1. Responda EXCLUSIVAMENTE a dúvidas sobre Saúde e Segurança do Trabalho (SST) e Normas Regulamentadoras (NRs) do Ministério do Trabalho e Emprego.
            2. Se a pergunta for sobre qualquer assunto fora de SST/NRs (ex.: culinária, programação geral, política, esportes, finanças pessoais), classifique como "FORA_DO_ESCOPO" e recuse educadamente informando que atua apenas em temas de SST e Normas Regulamentadoras.
            3. JAMAIS emita diagnósticos médicos, pareceres clínicos individuais, prescrições, atestados (ASO) ou redija/assine laudos técnicos conclusivos. Caso o usuário peça isso, classifique como "RECUSADA_LAUDO_DIAGNOSTICO" e informe que tais atos são privativos de Médico do Trabalho ou Engenheiro de Segurança habilitado. (Você pode explicar o que a NR exige sobre um laudo ou exame, mas nunca emitir o documento ou diagnóstico).
            4. Baseie suas respostas prioritariamente nos trechos normativos fornecidos no contexto e CITE expressamente a norma e o item (ex.: "Conforme a NR-07, item 7.5.8..."). Se os trechos fornecidos não contiverem a resposta específica e você não tiver certeza normativa, classifique como "BASE_INSUFICIENTE".
            5. Responda sempre em JSON estrito no formato:
               {"classificacao": "RESPONDIDA" | "FORA_DO_ESCOPO" | "BASE_INSUFICIENTE" | "RECUSADA_LAUDO_DIAGNOSTICO", "resposta": "texto em português claro e objetivo"}
            """;

    public record ConversaResumoResponse(Long id, String titulo, OffsetDateTime atualizadoEm) {
    }

    public record FonteNormativa(Long trechoId, String norma, String item, String titulo) {
    }

    public record MensagemResponse(
            Long id,
            PapelMensagem papel,
            String conteudo,
            ClassificacaoResposta classificacao,
            List<FonteNormativa> fontes,
            OffsetDateTime criadoEm) {
    }

    public record PerguntaRequest(
            Long conversaId,
            @NotBlank(message = "Informe a pergunta")
            @Size(max = 2000, message = "A pergunta deve ter no máximo 2000 caracteres")
            String pergunta) {
    }

    public record RespostaIaResponse(
            Long conversaId,
            MensagemResponse pergunta,
            MensagemResponse resposta) {
    }

    public record StatusIaResponse(String modo, String modelo) {
    }

    private final IaConversaRepository conversaRepository;
    private final IaMensagemRepository mensagemRepository;
    private final NrTrechoRepository nrTrechoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SalurisProperties props;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public AgenteIaService(IaConversaRepository conversaRepository,
            IaMensagemRepository mensagemRepository,
            NrTrechoRepository nrTrechoRepository,
            UsuarioRepository usuarioRepository,
            SalurisProperties props,
            ObjectMapper objectMapper) {
        this.conversaRepository = conversaRepository;
        this.mensagemRepository = mensagemRepository;
        this.nrTrechoRepository = nrTrechoRepository;
        this.usuarioRepository = usuarioRepository;
        this.props = props;
        this.objectMapper = objectMapper;
        String baseUrl = props.ia() != null && props.ia().baseUrl() != null
                ? props.ia().baseUrl() : "https://api.openai.com/v1";
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public StatusIaResponse obterStatus() {
        boolean temChave = props.ia() != null && props.ia().apiKey() != null && !props.ia().apiKey().isBlank();
        String modelo = props.ia() != null && props.ia().modelo() != null ? props.ia().modelo() : "gpt-4o-mini";
        return new StatusIaResponse(temChave ? "OPENAI" : "OFFLINE", temChave ? modelo : "RAG-NR-Local");
    }

    @Transactional(readOnly = true)
    public List<ConversaResumoResponse> listarConversas() {
        Long usuarioId = TenantContext.exigirUsuario().getId();
        return conversaRepository.findByUsuarioIdOrderByAtualizadoEmDesc(usuarioId)
                .stream()
                .map(c -> new ConversaResumoResponse(c.getId(), c.getTitulo(), c.getAtualizadoEm()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MensagemResponse> listarMensagens(Long conversaId) {
        Long usuarioId = TenantContext.exigirUsuario().getId();
        IaConversa c = conversaRepository.findByIdAndUsuarioId(conversaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada."));
        return mensagemRepository.findByConversaIdOrderByCriadoEmAsc(c.getId())
                .stream()
                .map(this::paraResposta)
                .toList();
    }

    @Transactional
    public void excluirConversa(Long conversaId) {
        Long usuarioId = TenantContext.exigirUsuario().getId();
        IaConversa c = conversaRepository.findByIdAndUsuarioId(conversaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada."));
        conversaRepository.delete(c);
    }

    @Transactional
    public RespostaIaResponse perguntar(PerguntaRequest req) {
        Usuario usuario = usuarioRepository.findById(TenantContext.exigirUsuario().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        String textoPergunta = req.pergunta().trim();

        IaConversa conversa;
        if (req.conversaId() != null) {
            conversa = conversaRepository.findByIdAndUsuarioId(req.conversaId(), usuario.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada."));
        } else {
            conversa = new IaConversa();
            conversa.setEmpresaId(usuario.getEmpresaId());
            conversa.setUsuario(usuario);
            conversa.setTitulo(gerarTitulo(textoPergunta));
            conversa = conversaRepository.save(conversa);
        }

        IaMensagem msgUsuario = new IaMensagem();
        msgUsuario.setConversa(conversa);
        msgUsuario.setPapel(PapelMensagem.USUARIO);
        msgUsuario.setConteudo(textoPergunta);
        msgUsuario = mensagemRepository.save(msgUsuario);

        ResultadoGeracao geracao = processarPergunta(textoPergunta);

        IaMensagem msgAssistente = new IaMensagem();
        msgAssistente.setConversa(conversa);
        msgAssistente.setPapel(PapelMensagem.ASSISTENTE);
        msgAssistente.setConteudo(geracao.texto());
        msgAssistente.setClassificacao(geracao.classificacao());
        msgAssistente.setFontes(geracao.fontes().stream().map(AgenteIaService::fonteParaMapa).toList());
        msgAssistente = mensagemRepository.save(msgAssistente);

        conversa.setTitulo(conversa.getTitulo()); // atualiza timestamp via @PreUpdate se editado

        return new RespostaIaResponse(conversa.getId(), paraResposta(msgUsuario), paraResposta(msgAssistente));
    }

    private record ResultadoGeracao(
            ClassificacaoResposta classificacao,
            String texto,
            List<FonteNormativa> fontes) {
    }

    private ResultadoGeracao processarPergunta(String pergunta) {
        // 1. Guarda-corpo determinístico contra emissão de laudos, pareceres médicos ou diagnósticos (RN07 / RF29)
        if (solicitaLaudoOuDiagnostico(pergunta)) {
            return new ResultadoGeracao(
                    ClassificacaoResposta.RECUSADA_LAUDO_DIAGNOSTICO,
                    "Não posso emitir diagnósticos clínicos, pareceres médicos, Atestados de Saúde Ocupacional (ASO) "
                            + "ou redigir/assinar laudos técnicos. Conforme as Normas Regulamentadoras (NR-01, NR-07, NR-15 e NR-16), "
                            + "a emissão e assinatura de laudos e atestados ocupacionais são atos privativos de profissionais "
                            + "legalmente habilitados (Médico do Trabalho com CRM ou Engenheiro de Segurança do Trabalho). "
                            + "Posso, contudo, esclarecer quais são os requisitos normativos e prazos aplicáveis.",
                    List.of());
        }

        // 2. Recuperação de trechos normativos relevantes na base de NRs (RAG)
        List<NrTrecho> trechos = recuperarTrechos(pergunta);
        List<FonteNormativa> fontes = trechos.stream()
                .map(t -> new FonteNormativa(t.getId(), t.getNorma(), t.getItem(), t.getTituloNorma()))
                .toList();

        boolean temTerminologiaSst = !trechos.isEmpty() || contemTermosDeSst(pergunta);
        if (!temTerminologiaSst) {
            return new ResultadoGeracao(
                    ClassificacaoResposta.FORA_DO_ESCOPO,
                    "Atuo exclusivamente como assistente técnico de Saúde e Segurança do Trabalho (SST) e Normas "
                            + "Regulamentadoras (NRs) do Ministério do Trabalho e Emprego. Sua pergunta parece estar fora "
                            + "desse escopo. Por favor, envie uma dúvida relacionada a NRs, PGR, PCMSO, LTCAT, exames "
                            + "ocupacionais, EPIs ou gestão de SST.",
                    List.of());
        }

        // 3. Se houver chave OpenAI configurada, chama o LLM com os trechos normativos como contexto
        boolean temChave = props.ia() != null && props.ia().apiKey() != null && !props.ia().apiKey().isBlank();
        if (temChave) {
            try {
                return chamarOpenAi(pergunta, trechos, fontes);
            } catch (Exception e) {
                log.warn("Falha ao consultar API OpenAI; utilizando resposta baseada na base local de NRs: {}",
                        e.getMessage());
            }
        }

        // 4. Modo consultivo local (OFFLINE) baseado nos trechos recuperados (Quadro 15 - 6a.1 e 6a.2 / RF29)
        if (trechos.isEmpty()) {
            return new ResultadoGeracao(
                    ClassificacaoResposta.BASE_INSUFICIENTE,
                    "A base de conhecimento normativa não contém informação específica suficiente para responder com "
                            + "segurança a essa questão, que requer consulta direta a um profissional habilitado "
                            + "(Engenheiro de Segurança do Trabalho ou Médico do Trabalho). "
                            + "NRs possivelmente relacionadas ao tema para consulta complementar: "
                            + "NR-01 (Disposições Gerais e PGR), NR-06 (EPI), NR-07 (PCMSO e Exames Ocupacionais), "
                            + "NR-09 (Avaliação e Controle das Exposições Ocupacionais), NR-15 (Insalubridade), "
                            + "NR-16 (Periculosidade), NR-17 (Ergonomia), NR-33 (Espaços Confinados) e NR-35 (Trabalho em Altura).",
                    List.of());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Com base na base normativa de Saúde e Segurança do Trabalho, seguem as referências aplicáveis:\n\n");
        for (NrTrecho t : trechos) {
            sb.append("• **").append(t.getNorma());
            if (t.getItem() != null && !t.getItem().isBlank()) {
                sb.append(" (item ").append(t.getItem()).append(")");
            }
            sb.append(" — ").append(t.getTituloNorma()).append("**:\n")
                    .append(t.getConteudo()).append("\n\n");
        }
        sb.append("_Nota: Resposta orientativa fundamentada nas Normas Regulamentadoras. Não substitui parecer técnico ou médico habilitado._");
        return new ResultadoGeracao(ClassificacaoResposta.RESPONDIDA, sb.toString(), fontes);
    }

    private ResultadoGeracao chamarOpenAi(String pergunta, List<NrTrecho> trechos, List<FonteNormativa> fontes)
            throws Exception {
        StringBuilder contexto = new StringBuilder("Trechos de Normas Regulamentadoras recuperados da base:\n");
        if (trechos.isEmpty()) {
            contexto.append("(Nenhum trecho específico encontrado na busca textual.)\n");
        } else {
            for (NrTrecho t : trechos) {
                contexto.append("- [").append(t.getNorma())
                        .append(t.getItem() != null ? ", item " + t.getItem() : "")
                        .append("] ").append(t.getConteudo()).append("\n");
            }
        }
        contexto.append("\nPergunta do usuário: ").append(pergunta);

        Map<String, Object> body = Map.of(
                "model", props.ia().modelo(),
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", contexto.toString())
                )
        );

        String respJson = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + props.ia().apiKey().trim())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(respJson);
        String content = root.path("choices").path(0).path("message").path("content").asText();
        JsonNode parsed = objectMapper.readTree(content);

        String classStr = parsed.path("classificacao").asText("RESPONDIDA");
        ClassificacaoResposta classificacao;
        try {
            classificacao = ClassificacaoResposta.valueOf(classStr);
        } catch (IllegalArgumentException e) {
            classificacao = ClassificacaoResposta.RESPONDIDA;
        }
        String resposta = parsed.path("resposta").asText(content);
        List<FonteNormativa> fontesFinais = classificacao == ClassificacaoResposta.RESPONDIDA ? fontes : List.of();
        return new ResultadoGeracao(classificacao, resposta, fontesFinais);
    }

    private List<NrTrecho> recuperarTrechos(String pergunta) {
        List<NrTrecho> resultado = new ArrayList<>();
        Set<Long> ids = new LinkedHashSet<>();

        // Verifica se a pergunta cita explicitamente alguma NR (ex.: NR-7, NR 07, NR35)
        Matcher m = PADRAO_NR.matcher(pergunta);
        while (m.find()) {
            int num = Integer.parseInt(m.group(1));
            String codigo = String.format("NR-%02d", num);
            for (NrTrecho t : nrTrechoRepository.findByNormaAndAtivoTrue(codigo)) {
                if (ids.add(t.getId())) {
                    resultado.add(t);
                }
            }
        }

        // Busca full-text em português no PostgreSQL
        String tsQuery = montarTsQuery(pergunta);
        if (!tsQuery.isEmpty()) {
            try {
                for (NrTrecho t : nrTrechoRepository.buscarRelevantes(tsQuery)) {
                    if (ids.add(t.getId()) && resultado.size() < 5) {
                        resultado.add(t);
                    }
                }
            } catch (Exception e) {
                log.debug("Busca full-text falhou ({}), usando fallback em memória.", e.getMessage());
                buscarFallbackEmMemoria(pergunta, resultado, ids);
            }
        }
        return resultado.size() > 5 ? resultado.subList(0, 5) : resultado;
    }

    private void buscarFallbackEmMemoria(String pergunta, List<NrTrecho> resultado, Set<Long> ids) {
        String normPergunta = normalizar(pergunta);
        String[] termos = normPergunta.split("\\s+");
        for (NrTrecho t : nrTrechoRepository.findByAtivoTrueOrderByNormaAscItemAsc()) {
            String texto = normalizar(t.getTituloNorma() + " " + t.getConteudo());
            for (String termo : termos) {
                if (termo.length() >= 3 && !STOPWORDS.contains(termo) && texto.contains(termo)) {
                    if (ids.add(t.getId()) && resultado.size() < 5) {
                        resultado.add(t);
                    }
                    break;
                }
            }
        }
    }

    private static String montarTsQuery(String pergunta) {
        String norm = normalizar(pergunta);
        Set<String> termos = new LinkedHashSet<>();
        for (String p : norm.split("[^a-z0-9]+")) {
            if (p.length() >= 3 && !STOPWORDS.contains(p)) {
                termos.add(p);
            }
        }
        return String.join(" | ", termos);
    }

    private static boolean solicitaLaudoOuDiagnostico(String pergunta) {
        String norm = normalizar(pergunta);
        for (Pattern p : PADROES_RECUSA_LAUDO_DIAGNOSTICO) {
            if (p.matcher(pergunta).find() || p.matcher(norm).find()) {
                return true;
            }
        }
        return false;
    }

    private static boolean contemTermosDeSst(String pergunta) {
        String norm = normalizar(pergunta);
        if (PADRAO_NR.matcher(norm).find()) {
            return true;
        }
        for (String termo : TERMOS_DOMINIO_SST) {
            if (norm.contains(termo)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT);
    }

    private static String gerarTitulo(String pergunta) {
        String limpa = pergunta.replaceAll("\\s+", " ").trim();
        return limpa.length() <= 80 ? limpa : limpa.substring(0, 77) + "...";
    }

    private static Map<String, Object> fonteParaMapa(FonteNormativa f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("trechoId", f.trechoId());
        m.put("norma", f.norma());
        m.put("item", f.item());
        m.put("titulo", f.titulo());
        return m;
    }

    private MensagemResponse paraResposta(IaMensagem m) {
        List<FonteNormativa> fontes = new ArrayList<>();
        if (m.getFontes() != null) {
            for (Map<String, Object> item : m.getFontes()) {
                Object tid = item.get("trechoId");
                Long trechoId = tid instanceof Number num ? num.longValue() : null;
                fontes.add(new FonteNormativa(
                        trechoId,
                        (String) item.get("norma"),
                        (String) item.get("item"),
                        (String) item.get("titulo")));
            }
        }
        return new MensagemResponse(
                m.getId(),
                m.getPapel(),
                m.getConteudo(),
                m.getClassificacao(),
                fontes,
                m.getCriadoEm());
    }
}
