# Saluris IA - Contrato da API REST (v1)

Base: `/api`. JSON UTF-8. Datas `YYYY-MM-DD`, data-hora ISO-8601 (`2026-10-18T11:49:14Z`).
Enums trafegam pelo **nome** (ex.: `RH_EMPRESA`). Autenticação: header `Authorization: Bearer <token>`.

## Convenções

- **Erros** (RFC 7807): `{ "title", "status", "detail", "erros"?: { campo: mensagem } }`
  - 400 validação (com `erros`) · 401 não autenticado · 403 sem permissão · 404 não encontrado (ou de outra empresa)
  - 409 duplicidade/conflito · 422 regra de negócio
- **Paginação** (`?page=0&size=20&sort=campo,asc`): `{ "content": [...], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }`
- **Admin e empresa**: o ADMINISTRADOR não tem empresa; nos endpoints "por empresa" ele deve enviar `?empresaId=`. Para os demais perfis o parâmetro é ignorado/validado (sempre a própria empresa).
- **Upload**: `multipart/form-data`; o arquivo vai no campo `arquivo` (somente PDF, máx. 20 MB). Demais campos como campos de formulário simples.
- **Download**: `GET .../arquivo` retorna `application/pdf` (precisa do header Authorization → no frontend baixar via `fetch` + `blob`).

## Enums

| Enum | Valores |
|---|---|
| Perfil | `ADMINISTRADOR`, `ESPECIALISTA_SST`, `MEDICO_TRABALHO`, `RH_EMPRESA` |
| PorteEmpresa | `MEI`, `ME`, `EPP`, `MEDIO`, `GRANDE` |
| StatusValidade | `EM_DIA` (Em dia), `PROXIMO_VENCIMENTO` (Próximo do Vencimento), `VENCIDO` (Vencido) |
| StatusFluxoExame | `PENDENTE`, `AGENDADO`, `REALIZADO`, `AGUARDANDO_ASSINATURA`, `HOMOLOGADO`, `REVISADO_PENDENTE` |
| StatusLaudo | `AGUARDANDO_ASSINATURA`, `HOMOLOGADO`, `REVISADO_PENDENTE` |
| TipoNotificacao | `PREVENTIVO`, `URGENCIA`, `LAUDO_PENDENTE_ASSINATURA`, `LAUDO_HOMOLOGADO`, `LAUDO_REVISADO_PENDENTE`, `EXAME_PENDENTE_ASSINATURA`, `EXAME_HOMOLOGADO`, `EXAME_REVISADO_PENDENTE`, `PERIODICIDADE_NAO_CONFIGURADA`, `EXAME_AGENDADO` |
| ClassificacaoResposta | `RESPONDIDA`, `FORA_DO_ESCOPO`, `BASE_INSUFICIENTE`, `RECUSADA_LAUDO_DIAGNOSTICO` |

## Matriz de acesso (resumo)

| Módulo | ADMIN | ESPECIALISTA | MÉDICO | RH |
|---|---|---|---|---|
| Usuários, Empresas (escrita), Tipos (escrita), Auditoria, Executar monitoramento | ✔ | | | |
| Tipos (leitura) | ✔ | ✔ | ✔ | ✔ |
| Unidades / Funções / Setores | ✔* | ✔ | | ✔ |
| Matriz de exames | leitura* | ✔ | | leitura |
| Colaboradores | leitura* | leitura | | ✔ |
| Exames (listar) | leitura* | leitura | | ✔ (agendar, resultado) |
| Documentos SST | leitura* | ✔ | | ✔ |
| Laudos | leitura* | ✔ (criar, reenviar) | leitura | leitura |
| Assinaturas | | | ✔ | |
| Dashboard / Relatórios | ✔* | ✔ | | ✔ |
| Agente IA | | ✔ | | ✔ |
| Notificações | ✔ | ✔ | ✔ | ✔ |

`*` = exige `?empresaId=`.

---

## 1. Autenticação

| Método | Rota | Corpo | Resposta |
|---|---|---|---|
| POST | `/auth/login` | `{ email, senha }` | `{ token, tipo:"Bearer", expiraEm, usuario: UsuarioLogado }` |
| GET | `/auth/me` | | `UsuarioLogado` |
| POST | `/auth/trocar-senha` | `{ senhaAtual, novaSenha }` (10-72, letras e números) | 204 |

`UsuarioLogado = { id, nome, email, perfil, empresaId, empresaNome, deveTrocarSenha, crm, crmUf }`

Se `deveTrocarSenha = true`, todos os endpoints fora de `/auth/**` retornam 403 até a troca.

## 2. Empresas

`Empresa = { id, cnpj (14 dígitos), razaoSocial, nomeFantasia, setorAtuacao, porte, contatoNome, emailContato, telefone, ativa }`

| Método | Rota | Acesso | Corpo / Resposta |
|---|---|---|---|
| GET | `/empresas` | todos (não-admin recebe só a própria) | `Empresa[]` |
| GET | `/empresas/{id}` | todos (própria) | `Empresa` |
| POST | `/empresas` | ADMIN | `EmpresaForm` → `Empresa` (201). CNPJ aceito com/sem máscara; 409 se CNPJ duplicado |
| PUT | `/empresas/{id}` | ADMIN | `EmpresaForm` → `Empresa` |
| PATCH | `/empresas/{id}/ativa` | ADMIN | `{ ativa: boolean }` → `Empresa` |

`EmpresaForm = { cnpj, razaoSocial, nomeFantasia?, setorAtuacao, porte, contatoNome?, emailContato, telefone? }`

## 3. Usuários (ADMIN)

`Usuario = { id, nome, email, perfil, empresaId, empresaNome, crm, crmUf, ativo, deveTrocarSenha, ultimoAcesso }`

| Método | Rota | Corpo / Resposta |
|---|---|---|
| GET | `/usuarios?empresaId=&perfil=` | `Usuario[]` |
| POST | `/usuarios` | `{ nome, email, perfil, empresaId?, crm?, crmUf? }` → `{ usuario: Usuario, senhaTemporaria }` (201). 409 e-mail duplicado; 422 se perfil ≠ ADMIN sem empresa, ou médico sem CRM |
| PUT | `/usuarios/{id}` | `{ nome, perfil, empresaId?, crm?, crmUf? }` → `Usuario` |
| PATCH | `/usuarios/{id}/ativo` | `{ ativo }` → `Usuario` |
| POST | `/usuarios/{id}/redefinir-senha` | → `{ senhaTemporaria }` |

## 4. Catálogos (leitura: todos; escrita: ADMIN)

| Recurso | Campos |
|---|---|
| `/tipos-exame` | `{ id, nome, descricao, observacoesTecnicas, ativo }` |
| `/tipos-documento` | `{ id, sigla, nome, descricao, periodicidadePadraoMeses (12 ou 24), ativo }` |
| `/tipos-laudo` | `{ id, nome, descricao, observacoes, ativo }` |

Para cada recurso: `GET /x?ativos=true` (lista), `POST /x` (201), `PUT /x/{id}`, `PATCH /x/{id}/ativo { ativo }`. 409 se nome/sigla duplicados.

## 5. Estrutura da empresa (ESPECIALISTA, RH; ADMIN com empresaId)

| Recurso | Campos |
|---|---|
| `/unidades` | `{ id, empresaId, nome, cidade, uf, ativo }` |
| `/funcoes` | `{ id, empresaId, nome, descricao, ativo }` |
| `/setores` | `{ id, empresaId, nome, descricao, ativo }` |

`GET /x?empresaId=&ativos=true`, `POST /x?empresaId=` (201), `PUT /x/{id}`, `PATCH /x/{id}/ativo { ativo }`. 409 se nome duplicado na empresa.

## 6. Matriz de exames (periodicidade por Função + Setor)

`MatrizExame = { id, funcaoId, funcaoNome, setorId, setorNome, tipoExameId, tipoExameNome, periodicidadeMeses, obrigatorio, configuradoPorNome, atualizadoEm }`

| Método | Rota | Acesso | Corpo / Resposta |
|---|---|---|---|
| GET | `/matriz-exames?empresaId=&funcaoId=&setorId=` | ESPECIALISTA, RH, ADMIN* | `MatrizExame[]` |
| POST | `/matriz-exames` | ESPECIALISTA | `{ funcaoId, setorId, tipoExameId, periodicidadeMeses (6..24), obrigatorio }` → `ResultadoMatriz` (201). **409** se a combinação já existe; o corpo do erro traz `existenteId` → o frontend pergunta se deseja sobrescrever e chama o PUT |
| PUT | `/matriz-exames/{id}` | ESPECIALISTA | `{ periodicidadeMeses, obrigatorio }` → `ResultadoMatriz` |
| DELETE | `/matriz-exames/{id}` | ESPECIALISTA | 204 |

`ResultadoMatriz = { configuracao: MatrizExame, colaboradoresAfetados: [{ id, nome }] }` (vencimentos recalculados automaticamente).

## 7. Colaboradores (escrita: RH)

`Colaborador = { id, nome, cpf, dataNascimento, dataAdmissao, dataDemissao, email, ativo, unidadeId, unidadeNome, funcaoId, funcaoNome, setorId, setorNome, statusConformidade (pior status dos exames em aberto ou null) }`

| Método | Rota | Corpo / Resposta |
|---|---|---|
| GET | `/colaboradores?empresaId=&busca=&page=&size=` | `Page<Colaborador>` (busca por nome ou CPF) |
| GET | `/colaboradores/{id}` | `{ colaborador: Colaborador, exames: Exame[] }` |
| POST | `/colaboradores` | `ColaboradorForm` → `ResultadoColaborador` (201). CPF com/sem máscara; 409 CPF duplicado na empresa |
| PUT | `/colaboradores/{id}` | `ColaboradorForm + { dataDemissao?, ativo }` → `ResultadoColaborador` |

`ColaboradorForm = { nome, cpf, dataNascimento?, dataAdmissao, email?, unidadeId, funcaoId, setorId }`
`ResultadoColaborador = { colaborador, examesGerados: Exame[], periodicidadeConfigurada: boolean }` — se `false`, exibir aviso "Periodicidade não configurada para esta função + setor; o Especialista SST foi notificado".

## 8. Exames

`Exame = { id, colaboradorId, colaboradorNome, tipoExameId, tipoExameNome, periodicidadeMeses, dataAgendada, localClinica, observacoes, dataRealizacao, dataVencimento, diasParaVencer, statusValidade, statusFluxo, possuiArquivo, medicoNome, crmAssinatura, crmUfAssinatura, assinadoEm, justificativaRecusa }`

| Método | Rota | Acesso | Corpo / Resposta |
|---|---|---|---|
| GET | `/exames?empresaId=&colaboradorId=&tipoExameId=&statusValidade=&statusFluxo=&page=&size=` | RH, ESPECIALISTA, ADMIN* | `Page<Exame>` (ordenado por vencimento; padrão: só exames em aberto, `incluirHistorico=true` traz homologados) |
| POST | `/exames/{id}/agendar` | RH | `{ dataAgendada (futura), localClinica, observacoes? }` → `Exame` |
| POST | `/exames/{id}/resultado` | RH | multipart `{ dataRealizacao, arquivo }` → `Exame` (vai para `AGUARDANDO_ASSINATURA`) |
| GET | `/exames/{id}/arquivo` | RH, ESPECIALISTA, MÉDICO, ADMIN | PDF |

## 9. Documentos SST

`Documento = { id, tipoDocumentoId, tipoDocumentoSigla, tipoDocumentoNome, titulo, descricao, versao, dataEmissao, dataValidade, diasParaVencer, statusValidade, documentoBaseId, documentoBaseTitulo, arquivoNome, enviadoPorNome, criadoEm, vigente (última versão do tipo) }`

| Método | Rota | Acesso | Corpo / Resposta |
|---|---|---|---|
| GET | `/documentos?empresaId=&somenteVigentes=` | ESPECIALISTA, RH, ADMIN* | `Documento[]` |
| POST | `/documentos` | ESPECIALISTA, RH | multipart `{ tipoDocumentoId, titulo, descricao?, dataEmissao, dataValidade? (padrão: emissão + periodicidade do tipo), documentoBaseId?, arquivo }` → `Documento` (201). Se já vencido, é gravado com `VENCIDO` e os responsáveis são notificados |
| GET | `/documentos/{id}/arquivo` | idem | PDF |

## 10. Laudos

`Laudo = { id, tipoLaudoId, tipoLaudoNome, titulo, descricao, status, dataEmissao, dataValidade, colaboradorId, colaboradorNome, arquivoNome, criadoPorNome, medicoNome, crmAssinatura, crmUfAssinatura, assinadoEm, justificativaRecusa, criadoEm }`

| Método | Rota | Acesso | Corpo / Resposta |
|---|---|---|---|
| GET | `/laudos?empresaId=&status=` | ESPECIALISTA, MÉDICO, RH, ADMIN* | `Laudo[]` |
| POST | `/laudos` | ESPECIALISTA | multipart `{ tipoLaudoId, titulo, descricao?, dataEmissao, dataValidade?, colaboradorId?, arquivo }` → `Laudo` (201, já `AGUARDANDO_ASSINATURA`; médicos notificados) |
| POST | `/laudos/{id}/reenviar` | ESPECIALISTA | multipart `{ arquivo }` → `Laudo` (de `REVISADO_PENDENTE` volta para `AGUARDANDO_ASSINATURA`) |
| GET | `/laudos/{id}/arquivo` | idem leitura | PDF |

## 11. Assinaturas (MÉDICO)

| Método | Rota | Corpo / Resposta |
|---|---|---|
| GET | `/assinaturas/pendentes` | `{ laudos: Laudo[], exames: Exame[] }` |
| POST | `/assinaturas/laudos/{id}/assinar` | → `Laudo` (HOMOLOGADO, registra CRM/UF/data-hora) |
| POST | `/assinaturas/laudos/{id}/recusar` | `{ justificativa }` → `Laudo` (REVISADO_PENDENTE) |
| POST | `/assinaturas/exames/{id}/assinar` | → `Exame` |
| POST | `/assinaturas/exames/{id}/recusar` | `{ justificativa }` → `Exame` |

## 12. Dashboard e relatórios (ESPECIALISTA, RH, ADMIN*)

`GET /dashboard?empresaId=&dataInicio=&dataFim=&tipoExameId=&colaboradorId=`

```json
{
  "exames": { "total": 0, "emDia": 0, "proximoVencimento": 0, "vencido": 0, "percentualConformidade": 100.0 },
  "documentos": { "total": 0, "emDia": 0, "proximoVencimento": 0, "vencido": 0 },
  "laudos": { "aguardandoAssinatura": 0, "homologados": 0, "revisadoPendente": 0 },
  "documentosVigentes": [ Documento ],
  "examesCriticos": [ Exame ]   // próximos do vencimento e vencidos, ordenados por vencimento (máx. 50)
}
```

`GET /relatorios/conformidade?formato=PDF|CSV&tipo=EXAMES|DOCUMENTOS|AMBOS&empresaId=&dataInicio=&dataFim=`
→ arquivo (`Content-Disposition: attachment; filename=...`). 422 se não houver registros para o filtro.

## 13. Notificações (todos)

`Notificacao = { id, tipo, titulo, mensagem, marcoDias, lida, criadoEm, exameId, documentoId, laudoId, colaboradorId }`

| Método | Rota | Resposta |
|---|---|---|
| GET | `/notificacoes?page=&size=` | `Page<Notificacao>` |
| GET | `/notificacoes/nao-lidas/contagem` | `{ total }` |
| POST | `/notificacoes/{id}/lida` | 204 |
| POST | `/notificacoes/marcar-todas-lidas` | 204 |

## 14. Agente IA (ESPECIALISTA, RH)

`Mensagem = { id, papel: USUARIO|ASSISTENTE, conteudo, classificacao, fontes: [{ trechoId, norma, item, titulo }], criadoEm }`

| Método | Rota | Corpo / Resposta |
|---|---|---|
| GET | `/ia/conversas` | `[{ id, titulo, atualizadoEm }]` |
| GET | `/ia/conversas/{id}/mensagens` | `Mensagem[]` |
| POST | `/ia/perguntar` | `{ conversaId?, pergunta (máx. 2000) }` → `{ conversaId, pergunta: Mensagem, resposta: Mensagem }` |
| DELETE | `/ia/conversas/{id}` | 204 |
| GET | `/ia/status` | `{ modo: "OPENAI" \| "OFFLINE", modelo }` |

## 15. Administração (ADMIN)

| Método | Rota | Resposta |
|---|---|---|
| GET | `/auditoria?empresaId=&page=&size=` | `Page<{ id, empresaId, usuarioId, usuarioNome, acao, entidade, entidadeId, detalhes, ipOrigem, criadoEm }>` |
| POST | `/admin/monitoramento/executar` | `{ examesAtualizados, documentosAtualizados, notificacoesCriadas, emailsEnfileirados }` (executa a rotina diária na hora) |
