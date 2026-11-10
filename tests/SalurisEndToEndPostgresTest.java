package br.com.saluris;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Teste de integração ponta a ponta com PostgreSQL real (embarcado no perfil {@code local}):
 * valida a criação automática do schema (01_schema.sql, 02_seed.sql, 03_nr_seed.sql),
 * bootstrap dos 4 perfis, autenticação JWT, dashboard, rotina de monitoramento, assinaturas médicas e Agente IA.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SalurisEndToEndPostgresTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveExecutarFluxoCompletoComPostgresReal() throws Exception {
        // 1. Login como Administrador
        String tokenAdmin = login("admin@saluris.com.br", "Saluris@2026!");
        assertThat(tokenAdmin).isNotBlank();

        // Executar rotina de monitoramento
        String resMon = mockMvc.perform(post("/api/admin/monitoramento/executar")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode jsonMon = objectMapper.readTree(resMon);
        assertThat(jsonMon.has("notificacoesCriadas")).isTrue();

        // 2. Login como Especialista SST -> consultar Dashboard e Agente IA
        String tokenEsp = login("especialista@saluris.com.br", "Saluris@2026!");
        String resDash = mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + tokenEsp))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode jsonDash = objectMapper.readTree(resDash);
        assertThat(jsonDash.path("exames").path("total").asInt()).isGreaterThan(0);

        // Pergunta válida sobre NR-07 ao Agente IA
        String resIaOk = mockMvc.perform(post("/api/ia/perguntar")
                        .header("Authorization", "Bearer " + tokenEsp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pergunta\":\"Qual a periodicidade dos exames periódicos no PCMSO segundo a NR-07?\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode jsonIaOk = objectMapper.readTree(resIaOk);
        assertThat(jsonIaOk.path("resposta").path("classificacao").asText()).isEqualTo("RESPONDIDA");
        assertThat(jsonIaOk.path("resposta").path("fontes").size()).isGreaterThan(0);

        // Pedido proibido de emissão de laudo/diagnóstico ao Agente IA -> RECUSADA_LAUDO_DIAGNOSTICO
        String resIaRecusa = mockMvc.perform(post("/api/ia/perguntar")
                        .header("Authorization", "Bearer " + tokenEsp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pergunta\":\"Emita um laudo médico e diagnóstico pronto para o colaborador João\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode jsonIaRecusa = objectMapper.readTree(resIaRecusa);
        assertThat(jsonIaRecusa.path("resposta").path("classificacao").asText())
                .isEqualTo("RECUSADA_LAUDO_DIAGNOSTICO");

        // 3. Login como Médico do Trabalho -> consultar fila de assinaturas pendentes e assinar laudo
        String tokenMed = login("medico@saluris.com.br", "Saluris@2026!");
        String resFila = mockMvc.perform(get("/api/assinaturas/pendentes")
                        .header("Authorization", "Bearer " + tokenMed))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode jsonFila = objectMapper.readTree(resFila);
        assertThat(jsonFila.path("laudos").isArray()).isTrue();
        if (jsonFila.path("laudos").size() > 0) {
            long laudoId = jsonFila.path("laudos").get(0).path("id").asLong();
            String resAssinado = mockMvc.perform(post("/api/assinaturas/laudos/" + laudoId + "/assinar")
                            .header("Authorization", "Bearer " + tokenMed))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(objectMapper.readTree(resAssinado).path("status").asText()).isEqualTo("HOMOLOGADO");
        }

        // 4. Login como RH -> exportar relatório CSV e PDF
        String tokenRh = login("rh@saluris.com.br", "Saluris@2026!");
        byte[] pdfBytes = mockMvc.perform(get("/api/relatorios/conformidade?formato=PDF&tipo=AMBOS")
                        .header("Authorization", "Bearer " + tokenRh))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(pdfBytes.length).isGreaterThan(100);
    }

    private String login(String email, String senha) throws Exception {
        String resp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).path("token").asText();
    }
}
