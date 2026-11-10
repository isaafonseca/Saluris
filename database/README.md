# Saluris IA — Banco de Dados (PostgreSQL 14+)

Scripts SQL da camada de persistência relacional do sistema **Saluris IA**:

1. [`01_schema.sql`](file:///c:/Users/isabe/OneDrive/Documentos/TCC/saluris-ia/database/01_schema.sql) — Criação das 19 tabelas (`empresas`, `unidades`, `usuarios`, `tipos_exame`, `tipos_documento`, `tipos_laudo`, `funcoes`, `setores`, `matriz_exames`, `colaboradores`, `exames`, `documentos`, `laudos`, `notificacoes`, `envios_email`, `logs_auditoria`, `nr_trechos`, `ia_conversas`, `ia_mensagens`), funções PL/pgSQL `fn_valida_cpf` e `fn_valida_cnpj`, chaves estrangeiras compostas `(id, empresa_id)` para isolamento físico de tenant, índices parciais e triggers de atualização de `atualizado_em`.
2. [`02_seed.sql`](file:///c:/Users/isabe/OneDrive/Documentos/TCC/saluris-ia/database/02_seed.sql) — Catálogos iniciais de tipos de exame ocupacional, tipos de documento SST (`PGR`, `PCMSO`, `LTCAT`) e tipos de laudo técnico (`LTCAT`, `Laudo de Insalubridade`, `Laudo de Periculosidade`, `AET`).
3. [`03_nr_seed.sql`](file:///c:/Users/isabe/OneDrive/Documentos/TCC/saluris-ia/database/03_nr_seed.sql) — Base de conhecimento das Normas Regulamentadoras (`NR-01` a `NR-35`) com coluna gerada `TSVECTOR` em português e índice `GIN` para busca contextual do Agente IA.

## Aplicação automática pelo Backend
Ao subir o backend Spring Boot, o componente `SchemaInitializer` verifica se o schema já existe e executa automaticamente os três scripts acima na ordem correta.

## Aplicação manual via `psql` (opcional)
```bash
psql -U postgres -d saluris -f 01_schema.sql
psql -U postgres -d saluris -f 02_seed.sql
psql -U postgres -d saluris -f 03_nr_seed.sql
```
