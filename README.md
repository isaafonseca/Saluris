# Saluris IA — Gestão Inteligente de Documentos e Prazos de SST

Plataforma web full-stack para centralização de informações, automação de prazos e monitoramento contínuo de obrigações legais de **Saúde e Segurança do Trabalho (SST)**, desenvolvida como Trabalho de Conclusão de Curso (FATEC Baixada Santista — Rubens Lara).

---

## 1. Arquitetura e Stack Tecnológico

| Camada | Tecnologias |
|---|---|
| **Backend** | Java 21 · Spring Boot 3.4.5 (REST API, Spring Data JPA/Hibernate, Validation, Actuator/Mail, OpenPDF) |
| **Segurança e Multitenancy** | Spring Security 6 (JWT Stateless + BCrypt) · RBAC nos 4 perfis (`@PreAuthorize`) · Isolamento lógico por empresa (`@Filter` Hibernate + `TenantFilterAspect` fail-closed + `TenantEntityListener` + FKs compostas no banco) |
| **Banco de Dados** | PostgreSQL 14+ (DDL em `database/01_schema.sql`, catálogos em `02_seed.sql`, base de NRs com Full-Text Search em português em `03_nr_seed.sql`). Inclui modo **`local` com PostgreSQL embarcado** (roda sem instalar SGBD ou Docker) |
| **Agente IA (RAG)** | Assistente especialista em Normas Regulamentadoras (NR-01 a NR-35) com busca textual (`tsvector`/`ts_rank`), guarda-corpo determinístico contra emissão de diagnósticos/laudos/ASO e integração com **OpenAI API** (`gpt-4o-mini`) + modo consultivo local (`OFFLINE`) |
| **Automação (CRON)** | `@Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")` para cálculo diário de validade (`EM_DIA`, `PROXIMO_VENCIMENTO`, `VENCIDO`) e disparo de alertas in-app e por e-mail com antecedência exata de **30, 15 e 7 dias** e alertas de urgência para vencidos, com fila de reenvio em caso de falha SMTP |
| **Frontend** | React 18 (JavaScript) + Vite · React Router v6 · Recharts · Lucide Icons · CSS responsivo próprio |

---

## 2. Estrutura do Repositório (Padrão MVC Unificado)

O projeto é organizado diretamente na raiz seguindo o padrão arquitetural **MVC (Model-View-Controller)**, sem subdivisões separadas de `backend` e `frontend`:

- [`models/`](file:///c:/Users/isabe/Downloads/saluris-ia/models) — Camada de Modelo de Domínio e Dados:
  - [`models/entity/`](file:///c:/Users/isabe/Downloads/saluris-ia/models/entity) — Entidades JPA mapeadas para as tabelas do PostgreSQL (`Empresa`, `Usuario`, `Colaborador`, `Exame`, `Laudo`, `Documento`, `MatrizExame`, etc.).
  - [`models/enums/`](file:///c:/Users/isabe/Downloads/saluris-ia/models/enums) — Enumerações de domínio (`Perfil`, `StatusValidade`, `StatusFluxoExame`, `StatusLaudo`, etc.).
  - [`models/dto/`](file:///c:/Users/isabe/Downloads/saluris-ia/models/dto) — Objetos de transferência de dados (`LoginRequest`, `LoginResponse`, `PageResponse`, etc.).
- [`views/`](file:///c:/Users/isabe/Downloads/saluris-ia/views) — Camada de Apresentação / Interface do Usuário (React + Vite):
  - [`views/pages/`](file:///c:/Users/isabe/Downloads/saluris-ia/views/pages) — Telas da aplicação (`DashboardPage`, `ColaboradoresPage`, `ExamesPage`, `LaudosPage`, `AssinaturasPage`, `AgenteIAPage`, `MatrizExamesPage`, etc. e `views/pages/admin/`).
  - [`views/components/`](file:///c:/Users/isabe/Downloads/saluris-ia/views/components) — Componentes visuais reutilizáveis e layout (`AppLayout`, `Modal`, `Table`, `Badge`).
  - [`views/context/`](file:///c:/Users/isabe/Downloads/saluris-ia/views/context) — Contextos de estado global (`AuthContext`, `EmpresaContext`, `ToastContext`).
  - [`views/api/`](file:///c:/Users/isabe/Downloads/saluris-ia/views/api) e [`views/utils/`](file:///c:/Users/isabe/Downloads/saluris-ia/views/utils) — Cliente HTTP e formatadores.
- [`controllers/`](file:///c:/Users/isabe/Downloads/saluris-ia/controllers) — Controladores REST (`AuthController`, `AdminController`, `ColaboradorController`, `ExameController`, `LaudoController`, `AssinaturaController`, `AgenteIaController`, `DashboardRelatorioController`, etc.).
- [`services/`](file:///c:/Users/isabe/Downloads/saluris-ia/services) — Regras de negócio, cálculo de validade, armazenamento de PDFs, assinaturas médicas, RAG do Agente IA e monitoramento diário CRON.
- [`repositories/`](file:///c:/Users/isabe/Downloads/saluris-ia/repositories) — Interfaces Spring Data JPA de acesso ao banco de dados.
- [`config/`](file:///c:/Users/isabe/Downloads/saluris-ia/config) — Inicialização da aplicação (`SalurisApplication`), configurações (`application.yml`), segurança JWT (`config/security/`), isolamento multitenant (`config/tenant/`), tratamento global de exceções (`config/exception/`) e validações de CPF/CNPJ/validade (`config/validation/`).
- [`database/`](file:///c:/Users/isabe/Downloads/saluris-ia/database) — Scripts SQL do PostgreSQL (`01_schema.sql`, `02_seed.sql`, `03_nr_seed.sql`).
- [`tests/`](file:///c:/Users/isabe/Downloads/saluris-ia/tests) — Testes automatizados unitários e de integração ponta a ponta com PostgreSQL real.
- [`docs/API.md`](file:///c:/Users/isabe/Downloads/saluris-ia/docs/API.md) — Contrato completo da API REST.
- [`iniciar.ps1`](file:///c:/Users/isabe/Downloads/saluris-ia/iniciar.ps1) — Script PowerShell para iniciar o projeto completo na raiz.

---

## 3. Como Executar Rapidamente (Modo Local — Zero Configuração)

No perfil `local`, a aplicação inicia automaticamente um **PostgreSQL real embarcado** (dados persistidos em `~/.saluris/pgdata`), executa `01_schema.sql`, `02_seed.sql` e `03_nr_seed.sql`, e popula uma empresa de demonstração completa (**PortoLog Santos**) com usuários nos 4 perfis.

### Opção A — Um comando (PowerShell)
```powershell
cd saluris-ia
.\iniciar.ps1
```

### Opção B — Em dois terminais (direto na raiz do projeto)
**Terminal 1 (Servidor Spring Boot na porta 8080):**
```powershell
cd saluris-ia
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

**Terminal 2 (Views React + Vite na porta 5173):**
```powershell
cd saluris-ia
npm install
npm run dev
```

Acesse **`http://localhost:5173`** no navegador.

---

## 4. Credenciais de Demonstração (Perfil `local`)

Todas as contas abaixo utilizam a senha **`Saluris@2026!`**:

| Perfil | E-mail | Escopo / Principais Funcionalidades |
|---|---|---|
| **Administrador (T.I.)** | `admin@saluris.com.br` | Gestão de usuários (com geração de senha temporária), empresas clientes (validação de CNPJ), catálogos de tipos de exame/documento/laudo, trilha de auditoria e execução manual do monitoramento diário |
| **Especialista SST** | `especialista@saluris.com.br` | Unidades, funções e setores; **Matriz de Exames (Função + Setor, 6 a 24 meses)** com recálculo automático; emissão e reenvio de laudos em PDF; documentos SST; Dashboard de Conformidade; Agente IA |
| **Médico do Trabalho** | `medico@saluris.com.br` | Fila de laudos e exames com status *"Aguardando Assinatura"*; visualização de PDF; **assinatura digital (CRM 148920/SP + data/hora)** alterando para *"Homologado"* (e gerando o próximo ciclo de exame) ou recusa com justificativa (*"Revisado Pendente"*) |
| **RH / Empresa** | `rh@saluris.com.br` | Cadastro de colaboradores (validação de CPF e **geração automática de exames obrigatórios e vencimentos pela matriz Função + Setor**); agendamento de exames; envio de resultado de exames em PDF; upload de PGR/PCMSO; Dashboard e relatórios PDF/CSV; Agente IA |

> **Chave da OpenAI (opcional):** Para usar o Agente IA conectado à API da OpenAI, defina a variável de ambiente `OPENAI_API_KEY=sk-...` antes de iniciar o backend. Sem a variável, o Agente IA funciona automaticamente no modo `OFFLINE` (RAG sobre a base normativa de NRs do PostgreSQL, mantendo todas as travas de escopo e de recusa de laudos/diagnósticos).
