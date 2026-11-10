-- =============================================================================
-- SALURIS IA - Gestão inteligente de documentos e prazos de SST
-- FASE 1: Modelagem de Dados (DDL) - PostgreSQL 14+          (revisão 2)
-- =============================================================================
-- Convenções:
--   * Nomes de tabelas/colunas em português, snake_case, tabelas no plural.
--   * Enumerações como VARCHAR + CHECK (compatível com @Enumerated(STRING) do JPA).
--   * PKs BIGINT GENERATED ALWAYS AS IDENTITY.
--   * Multitenancy lógico: toda tabela de dados de cliente carrega empresa_id e
--     as FKs filhas usam chave composta (id, empresa_id), impedindo no próprio
--     banco que um registro de uma empresa aponte para dados de outra
--     (inclusive usuários: criador/assinante/agendador sempre da mesma empresa).
--   * Apenas o ADMINISTRADOR (T.I. da plataforma) não pertence a uma empresa.
--   * CPF/CNPJ armazenados apenas com dígitos (11/14) e validados por dígito
--     verificador via CHECK.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 0. FUNÇÕES UTILITÁRIAS
-- -----------------------------------------------------------------------------

-- Atualiza automaticamente a coluna atualizado_em.
CREATE OR REPLACE FUNCTION fn_set_atualizado_em()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Valida CNPJ (somente dígitos, 14 posições, dígitos verificadores).
CREATE OR REPLACE FUNCTION fn_valida_cnpj(p_cnpj TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    pesos1 INT[] := ARRAY[5,4,3,2,9,8,7,6,5,4,3,2];
    pesos2 INT[] := ARRAY[6,5,4,3,2,9,8,7,6,5,4,3,2];
    soma   INT;
    dv1    INT;
    dv2    INT;
    i      INT;
BEGIN
    IF p_cnpj IS NULL OR p_cnpj !~ '^[0-9]{14}$' THEN
        RETURN FALSE;
    END IF;
    IF p_cnpj ~ '^(.)\1{13}$' THEN          -- todos os dígitos iguais
        RETURN FALSE;
    END IF;

    soma := 0;
    FOR i IN 1..12 LOOP
        soma := soma + SUBSTRING(p_cnpj FROM i FOR 1)::INT * pesos1[i];
    END LOOP;
    dv1 := CASE WHEN soma % 11 < 2 THEN 0 ELSE 11 - (soma % 11) END;

    soma := 0;
    FOR i IN 1..13 LOOP
        soma := soma + SUBSTRING(p_cnpj FROM i FOR 1)::INT * pesos2[i];
    END LOOP;
    dv2 := CASE WHEN soma % 11 < 2 THEN 0 ELSE 11 - (soma % 11) END;

    RETURN dv1 = SUBSTRING(p_cnpj FROM 13 FOR 1)::INT
       AND dv2 = SUBSTRING(p_cnpj FROM 14 FOR 1)::INT;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Valida CPF (somente dígitos, 11 posições, dígitos verificadores).
CREATE OR REPLACE FUNCTION fn_valida_cpf(p_cpf TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    soma INT;
    dv1  INT;
    dv2  INT;
    i    INT;
BEGIN
    IF p_cpf IS NULL OR p_cpf !~ '^[0-9]{11}$' THEN
        RETURN FALSE;
    END IF;
    IF p_cpf ~ '^(.)\1{10}$' THEN
        RETURN FALSE;
    END IF;

    soma := 0;
    FOR i IN 1..9 LOOP
        soma := soma + SUBSTRING(p_cpf FROM i FOR 1)::INT * (11 - i);
    END LOOP;
    dv1 := (soma * 10) % 11;
    IF dv1 = 10 THEN dv1 := 0; END IF;

    soma := 0;
    FOR i IN 1..10 LOOP
        soma := soma + SUBSTRING(p_cpf FROM i FOR 1)::INT * (12 - i);
    END LOOP;
    dv2 := (soma * 10) % 11;
    IF dv2 = 10 THEN dv2 := 0; END IF;

    RETURN dv1 = SUBSTRING(p_cpf FROM 10 FOR 1)::INT
       AND dv2 = SUBSTRING(p_cpf FROM 11 FOR 1)::INT;
END;
$$ LANGUAGE plpgsql IMMUTABLE;


-- -----------------------------------------------------------------------------
-- 1. EMPRESAS (tenant) - Quadro 05 / RF06
-- -----------------------------------------------------------------------------
CREATE TABLE empresas (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cnpj           CHAR(14)     NOT NULL,
    razao_social   VARCHAR(200) NOT NULL,
    nome_fantasia  VARCHAR(200),
    setor_atuacao  VARCHAR(150) NOT NULL,              -- ramo de atividade
    porte          VARCHAR(10)  NOT NULL,
    contato_nome   VARCHAR(150),
    email_contato  VARCHAR(255) NOT NULL,
    telefone       VARCHAR(20),
    ativa          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_empresas_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_empresas_cnpj CHECK (fn_valida_cnpj(cnpj)),
    CONSTRAINT ck_empresas_porte CHECK (porte IN ('MEI', 'ME', 'EPP', 'MEDIO', 'GRANDE')),
    CONSTRAINT ck_empresas_email CHECK (email_contato ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$')
);

-- -----------------------------------------------------------------------------
-- 2. UNIDADES de trabalho da empresa (colaborador = função + setor + unidade)
-- -----------------------------------------------------------------------------
CREATE TABLE unidades (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id     BIGINT       NOT NULL,
    nome           VARCHAR(150) NOT NULL,
    cidade         VARCHAR(100),
    uf             CHAR(2),
    ativa          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_unidades_empresa FOREIGN KEY (empresa_id)
        REFERENCES empresas (id) ON DELETE RESTRICT,
    CONSTRAINT uk_unidades_id_empresa UNIQUE (id, empresa_id),
    CONSTRAINT uk_unidades_empresa_nome UNIQUE (empresa_id, nome)
);

-- -----------------------------------------------------------------------------
-- 3. USUARIOS (RBAC) - Quadro 01 / RF01
--    ADMINISTRADOR (T.I. da plataforma): sem empresa (empresa_id NULL).
--    ESPECIALISTA_SST, MEDICO_TRABALHO, RH_EMPRESA: sempre vinculados a uma
--      empresa, que delimita todo o acesso a dados.
--    MEDICO_TRABALHO: obrigatoriamente com CRM + UF (usado na assinatura).
-- -----------------------------------------------------------------------------
CREATE TABLE usuarios (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id        BIGINT,
    nome              VARCHAR(150) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    senha_hash        VARCHAR(100) NOT NULL,           -- BCrypt
    deve_trocar_senha BOOLEAN      NOT NULL DEFAULT TRUE,  -- senha temporária (1º acesso)
    perfil            VARCHAR(20)  NOT NULL,
    crm               VARCHAR(20),
    crm_uf            CHAR(2),
    ativo             BOOLEAN      NOT NULL DEFAULT TRUE,
    ultimo_acesso     TIMESTAMPTZ,
    criado_em         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_usuarios_empresa FOREIGN KEY (empresa_id)
        REFERENCES empresas (id) ON DELETE RESTRICT,
    -- Permite FKs compostas (usuário sempre da mesma empresa do registro)
    CONSTRAINT uk_usuarios_id_empresa UNIQUE (id, empresa_id),
    CONSTRAINT ck_usuarios_perfil CHECK (perfil IN
        ('ADMINISTRADOR', 'ESPECIALISTA_SST', 'MEDICO_TRABALHO', 'RH_EMPRESA')),
    CONSTRAINT ck_usuarios_email CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    CONSTRAINT ck_usuarios_empresa_perfil CHECK (
        (perfil = 'ADMINISTRADOR' AND empresa_id IS NULL)
        OR (perfil <> 'ADMINISTRADOR' AND empresa_id IS NOT NULL)
    ),
    CONSTRAINT ck_usuarios_crm_medico CHECK (
        (perfil = 'MEDICO_TRABALHO' AND crm IS NOT NULL AND crm_uf IS NOT NULL)
        OR (perfil <> 'MEDICO_TRABALHO' AND crm IS NULL AND crm_uf IS NULL)
    )
);
CREATE UNIQUE INDEX uk_usuarios_email_lower ON usuarios (LOWER(email));
CREATE UNIQUE INDEX uk_usuarios_empresa_crm ON usuarios (empresa_id, crm_uf, crm)
    WHERE crm IS NOT NULL;
CREATE INDEX ix_usuarios_empresa ON usuarios (empresa_id);
CREATE INDEX ix_usuarios_perfil ON usuarios (perfil);

-- -----------------------------------------------------------------------------
-- 4. CATÁLOGOS ADMINISTRADOS PELO ADMINISTRADOR (globais) - Quadros 02, 03, 04
-- -----------------------------------------------------------------------------
CREATE TABLE tipos_exame (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                 VARCHAR(120) NOT NULL,         -- ex.: Audiometria
    descricao            VARCHAR(500),
    observacoes_tecnicas VARCHAR(1000),
    ativo                BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_tipos_exame_nome UNIQUE (nome)
);

CREATE TABLE tipos_documento (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sigla                     VARCHAR(20)  NOT NULL,    -- PGR, PCMSO, LTCAT
    nome                      VARCHAR(150) NOT NULL,
    descricao                 VARCHAR(500),
    periodicidade_padrao_meses SMALLINT    NOT NULL,    -- RN03: anual (12) ou bienal (24)
    ativo                     BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em                 TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_tipos_documento_sigla UNIQUE (sigla),
    CONSTRAINT uk_tipos_documento_nome UNIQUE (nome),
    CONSTRAINT ck_tipos_documento_periodicidade CHECK (periodicidade_padrao_meses IN (12, 24))
);

CREATE TABLE tipos_laudo (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome           VARCHAR(150) NOT NULL,
    descricao      VARCHAR(500),
    observacoes    VARCHAR(1000),
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_tipos_laudo_nome UNIQUE (nome)
);

-- -----------------------------------------------------------------------------
-- 5. FUNCOES e SETORES (por empresa)
--    UNIQUE (id, empresa_id) viabiliza FKs compostas que garantem o isolamento.
-- -----------------------------------------------------------------------------
CREATE TABLE funcoes (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id     BIGINT       NOT NULL,
    nome           VARCHAR(120) NOT NULL,
    descricao      VARCHAR(500),
    ativa          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_funcoes_empresa FOREIGN KEY (empresa_id)
        REFERENCES empresas (id) ON DELETE RESTRICT,
    CONSTRAINT uk_funcoes_id_empresa UNIQUE (id, empresa_id),
    CONSTRAINT uk_funcoes_empresa_nome UNIQUE (empresa_id, nome)
);

CREATE TABLE setores (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id     BIGINT       NOT NULL,
    nome           VARCHAR(120) NOT NULL,
    descricao      VARCHAR(500),
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_setores_empresa FOREIGN KEY (empresa_id)
        REFERENCES empresas (id) ON DELETE RESTRICT,
    CONSTRAINT uk_setores_id_empresa UNIQUE (id, empresa_id),
    CONSTRAINT uk_setores_empresa_nome UNIQUE (empresa_id, nome)
);

-- -----------------------------------------------------------------------------
-- 6. MATRIZ DE EXAMES (periodicidade por FUNÇÃO + SETOR + TIPO DE EXAME)
--    Quadro 06 / RN01 / RF07. Configurada pelo Especialista SST da empresa.
--    Periodicidade entre 6 e 24 meses (NR-07).
-- -----------------------------------------------------------------------------
CREATE TABLE matriz_exames (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id           BIGINT   NOT NULL,
    funcao_id            BIGINT   NOT NULL,
    setor_id             BIGINT   NOT NULL,
    tipo_exame_id        BIGINT   NOT NULL,
    periodicidade_meses  SMALLINT NOT NULL,
    obrigatorio          BOOLEAN  NOT NULL DEFAULT TRUE,
    configurado_por      BIGINT   NOT NULL,             -- especialista SST (mesma empresa)
    criado_em            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_matriz_funcao FOREIGN KEY (funcao_id, empresa_id)
        REFERENCES funcoes (id, empresa_id) ON DELETE CASCADE,
    CONSTRAINT fk_matriz_setor FOREIGN KEY (setor_id, empresa_id)
        REFERENCES setores (id, empresa_id) ON DELETE CASCADE,
    CONSTRAINT fk_matriz_tipo_exame FOREIGN KEY (tipo_exame_id)
        REFERENCES tipos_exame (id) ON DELETE RESTRICT,
    CONSTRAINT fk_matriz_configurado_por FOREIGN KEY (configurado_por, empresa_id)
        REFERENCES usuarios (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT uk_matriz_funcao_setor_exame UNIQUE (funcao_id, setor_id, tipo_exame_id),
    CONSTRAINT ck_matriz_periodicidade CHECK (periodicidade_meses BETWEEN 6 AND 24)
);
CREATE INDEX ix_matriz_lookup ON matriz_exames (empresa_id, funcao_id, setor_id);

-- -----------------------------------------------------------------------------
-- 7. COLABORADORES - Quadro 13 / RN02 / RF22
--    Uma única função por colaborador (funcao_id escalar); exigências
--    diferenciadas são tratadas por função + setor.
-- -----------------------------------------------------------------------------
CREATE TABLE colaboradores (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id      BIGINT       NOT NULL,
    unidade_id      BIGINT       NOT NULL,
    funcao_id       BIGINT       NOT NULL,
    setor_id        BIGINT       NOT NULL,
    nome            VARCHAR(150) NOT NULL,
    cpf             CHAR(11)     NOT NULL,
    data_nascimento DATE,
    data_admissao   DATE         NOT NULL,
    data_demissao   DATE,
    email           VARCHAR(255),                       -- notificação de agendamento
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_por      BIGINT       NOT NULL,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_colab_empresa FOREIGN KEY (empresa_id)
        REFERENCES empresas (id) ON DELETE RESTRICT,
    CONSTRAINT fk_colab_unidade FOREIGN KEY (unidade_id, empresa_id)
        REFERENCES unidades (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT fk_colab_funcao FOREIGN KEY (funcao_id, empresa_id)
        REFERENCES funcoes (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT fk_colab_setor FOREIGN KEY (setor_id, empresa_id)
        REFERENCES setores (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT fk_colab_criado_por FOREIGN KEY (criado_por, empresa_id)
        REFERENCES usuarios (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT uk_colab_id_empresa UNIQUE (id, empresa_id),
    CONSTRAINT uk_colab_empresa_cpf UNIQUE (empresa_id, cpf),
    CONSTRAINT ck_colab_cpf CHECK (fn_valida_cpf(cpf)),
    CONSTRAINT ck_colab_datas CHECK (data_demissao IS NULL OR data_demissao >= data_admissao)
);
CREATE INDEX ix_colab_empresa ON colaboradores (empresa_id);
CREATE INDEX ix_colab_funcao_setor ON colaboradores (funcao_id, setor_id);

-- -----------------------------------------------------------------------------
-- 8. EXAMES (exames ocupacionais de cada colaborador) - Quadros 11, 13, 14, 17
--    Gerados automaticamente a partir da matriz (função + setor) no cadastro.
--    - periodicidade_meses é um SNAPSHOT da matriz no momento da geração.
--    - data_vencimento = (data_realizacao OU data prevista) + periodicidade.
--    - status_validade é recalculado pelo CRON diário (08h00).
--    - fluxo: ... AGUARDANDO_ASSINATURA -> HOMOLOGADO | REVISADO_PENDENTE.
-- -----------------------------------------------------------------------------
CREATE TABLE exames (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id          BIGINT      NOT NULL,
    colaborador_id      BIGINT      NOT NULL,
    tipo_exame_id       BIGINT      NOT NULL,
    matriz_exame_id     BIGINT,                          -- origem da regra (pode ser removida)
    periodicidade_meses SMALLINT    NOT NULL,
    -- agendamento
    data_agendada       DATE,
    local_clinica       VARCHAR(200),
    observacoes         VARCHAR(1000),
    agendado_por        BIGINT,
    -- realização / validade
    data_realizacao     DATE,
    data_vencimento     DATE        NOT NULL,
    status_validade     VARCHAR(25) NOT NULL DEFAULT 'EM_DIA',
    status_fluxo        VARCHAR(25) NOT NULL DEFAULT 'PENDENTE',
    -- arquivo do resultado (somente PDF)
    arquivo_nome        VARCHAR(255),
    arquivo_path        VARCHAR(500),
    arquivo_mime_type   VARCHAR(100),
    arquivo_tamanho     BIGINT,
    arquivo_sha256      CHAR(64),
    -- assinatura / homologação (Médico do Trabalho)
    medico_id           BIGINT,
    crm_assinatura      VARCHAR(20),
    crm_uf_assinatura   CHAR(2),
    assinado_em         TIMESTAMPTZ,
    justificativa_recusa VARCHAR(1000),
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_exames_colaborador FOREIGN KEY (colaborador_id, empresa_id)
        REFERENCES colaboradores (id, empresa_id) ON DELETE CASCADE,
    CONSTRAINT fk_exames_tipo FOREIGN KEY (tipo_exame_id)
        REFERENCES tipos_exame (id) ON DELETE RESTRICT,
    CONSTRAINT fk_exames_matriz FOREIGN KEY (matriz_exame_id)
        REFERENCES matriz_exames (id) ON DELETE SET NULL,
    CONSTRAINT fk_exames_medico FOREIGN KEY (medico_id, empresa_id)
        REFERENCES usuarios (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT fk_exames_agendado_por FOREIGN KEY (agendado_por, empresa_id)
        REFERENCES usuarios (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT uk_exames_id_empresa UNIQUE (id, empresa_id),
    CONSTRAINT ck_exames_periodicidade CHECK (periodicidade_meses BETWEEN 6 AND 24),
    CONSTRAINT ck_exames_status_validade CHECK (status_validade IN
        ('EM_DIA', 'PROXIMO_VENCIMENTO', 'VENCIDO')),
    CONSTRAINT ck_exames_status_fluxo CHECK (status_fluxo IN
        ('PENDENTE', 'AGENDADO', 'REALIZADO', 'AGUARDANDO_ASSINATURA',
         'HOMOLOGADO', 'REVISADO_PENDENTE')),
    CONSTRAINT ck_exames_pdf CHECK (
        arquivo_path IS NULL OR arquivo_mime_type = 'application/pdf'),
    CONSTRAINT ck_exames_pdf_ext CHECK (
        arquivo_nome IS NULL OR LOWER(arquivo_nome) LIKE '%.pdf'),
    CONSTRAINT ck_exames_agendado CHECK (
        status_fluxo <> 'AGENDADO' OR data_agendada IS NOT NULL),
    CONSTRAINT ck_exames_realizacao CHECK (
        status_fluxo NOT IN ('REALIZADO', 'AGUARDANDO_ASSINATURA', 'HOMOLOGADO')
        OR data_realizacao IS NOT NULL),
    CONSTRAINT ck_exames_homologado CHECK (
        status_fluxo <> 'HOMOLOGADO'
        OR (medico_id IS NOT NULL AND crm_assinatura IS NOT NULL
            AND crm_uf_assinatura IS NOT NULL AND assinado_em IS NOT NULL)),
    CONSTRAINT ck_exames_revisado CHECK (
        status_fluxo <> 'REVISADO_PENDENTE'
        OR (medico_id IS NOT NULL AND justificativa_recusa IS NOT NULL
            AND LENGTH(TRIM(justificativa_recusa)) > 0))
);
CREATE INDEX ix_exames_empresa_vencimento ON exames (empresa_id, data_vencimento);
CREATE INDEX ix_exames_colaborador ON exames (colaborador_id);
CREATE INDEX ix_exames_status_validade ON exames (status_validade);
CREATE INDEX ix_exames_fila_assinatura ON exames (empresa_id)
    WHERE status_fluxo = 'AGUARDANDO_ASSINATURA';
-- Impede duplicar a obrigação "em aberto" do mesmo exame para o mesmo colaborador
CREATE UNIQUE INDEX uk_exames_aberto ON exames (colaborador_id, tipo_exame_id)
    WHERE status_fluxo IN ('PENDENTE', 'AGENDADO');

-- -----------------------------------------------------------------------------
-- 9. DOCUMENTOS SST (PGR, PCMSO, LTCAT...) - Quadros 08 e 12 / RF11, RF21
--    Upload exclusivamente PDF. Data de vencimento obrigatória (RF11).
--    documento_base_id: PCMSO elaborado com base em um PGR (RN04).
-- -----------------------------------------------------------------------------
CREATE TABLE documentos (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id         BIGINT       NOT NULL,
    tipo_documento_id  BIGINT       NOT NULL,
    documento_base_id  BIGINT,
    titulo             VARCHAR(200) NOT NULL,
    descricao          VARCHAR(500),
    versao             INTEGER      NOT NULL DEFAULT 1,
    data_emissao       DATE         NOT NULL,
    data_validade      DATE         NOT NULL,
    status_validade    VARCHAR(25)  NOT NULL DEFAULT 'EM_DIA',
    arquivo_nome       VARCHAR(255) NOT NULL,
    arquivo_path       VARCHAR(500) NOT NULL,
    arquivo_mime_type  VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    arquivo_tamanho    BIGINT       NOT NULL,
    arquivo_sha256     CHAR(64)     NOT NULL,
    enviado_por        BIGINT       NOT NULL,             -- RH ou Especialista (mesma empresa)
    criado_em          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_documentos_empresa FOREIGN KEY (empresa_id)
        REFERENCES empresas (id) ON DELETE RESTRICT,
    CONSTRAINT fk_documentos_tipo FOREIGN KEY (tipo_documento_id)
        REFERENCES tipos_documento (id) ON DELETE RESTRICT,
    CONSTRAINT fk_documentos_enviado_por FOREIGN KEY (enviado_por, empresa_id)
        REFERENCES usuarios (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT uk_documentos_id_empresa UNIQUE (id, empresa_id),
    CONSTRAINT fk_documentos_base FOREIGN KEY (documento_base_id, empresa_id)
        REFERENCES documentos (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT uk_documentos_versao UNIQUE (empresa_id, tipo_documento_id, versao),
    CONSTRAINT ck_documentos_pdf CHECK (arquivo_mime_type = 'application/pdf'),
    CONSTRAINT ck_documentos_pdf_ext CHECK (LOWER(arquivo_nome) LIKE '%.pdf'),
    CONSTRAINT ck_documentos_tamanho CHECK (arquivo_tamanho > 0),
    CONSTRAINT ck_documentos_versao CHECK (versao >= 1),
    CONSTRAINT ck_documentos_validade CHECK (data_validade >= data_emissao),
    CONSTRAINT ck_documentos_status CHECK (status_validade IN
        ('EM_DIA', 'PROXIMO_VENCIMENTO', 'VENCIDO'))
);
CREATE INDEX ix_documentos_empresa_tipo ON documentos (empresa_id, tipo_documento_id);
CREATE INDEX ix_documentos_validade ON documentos (data_validade);

-- -----------------------------------------------------------------------------
-- 10. LAUDOS - Quadros 07 e 11 / RN05 / RF09, RF10, RF18-RF20
--     Criados pelo Especialista SST (já nascem AGUARDANDO_ASSINATURA, notificando
--     o Médico). AGUARDANDO_ASSINATURA -> HOMOLOGADO | REVISADO_PENDENTE.
--     Assinatura registra CRM, UF, data e hora; recusa exige justificativa.
-- -----------------------------------------------------------------------------
CREATE TABLE laudos (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id           BIGINT       NOT NULL,
    tipo_laudo_id        BIGINT       NOT NULL,
    colaborador_id       BIGINT,                          -- laudo pode ser da empresa toda
    exame_id             BIGINT,                          -- ou vinculado a um exame
    titulo               VARCHAR(200) NOT NULL,
    descricao            VARCHAR(1000),
    status               VARCHAR(25)  NOT NULL DEFAULT 'AGUARDANDO_ASSINATURA',
    data_emissao         DATE         NOT NULL DEFAULT CURRENT_DATE,
    data_validade        DATE,
    arquivo_nome         VARCHAR(255) NOT NULL,
    arquivo_path         VARCHAR(500) NOT NULL,
    arquivo_mime_type    VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    arquivo_tamanho      BIGINT       NOT NULL,
    arquivo_sha256       CHAR(64)     NOT NULL,           -- integridade do que foi assinado
    criado_por           BIGINT       NOT NULL,           -- especialista SST (mesma empresa)
    medico_id            BIGINT,
    crm_assinatura       VARCHAR(20),
    crm_uf_assinatura    CHAR(2),
    assinado_em          TIMESTAMPTZ,
    justificativa_recusa VARCHAR(1000),
    criado_em            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_laudos_empresa FOREIGN KEY (empresa_id)
        REFERENCES empresas (id) ON DELETE RESTRICT,
    CONSTRAINT fk_laudos_tipo FOREIGN KEY (tipo_laudo_id)
        REFERENCES tipos_laudo (id) ON DELETE RESTRICT,
    CONSTRAINT fk_laudos_colaborador FOREIGN KEY (colaborador_id, empresa_id)
        REFERENCES colaboradores (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT fk_laudos_exame FOREIGN KEY (exame_id, empresa_id)
        REFERENCES exames (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT fk_laudos_criado_por FOREIGN KEY (criado_por, empresa_id)
        REFERENCES usuarios (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT fk_laudos_medico FOREIGN KEY (medico_id, empresa_id)
        REFERENCES usuarios (id, empresa_id) ON DELETE RESTRICT,
    CONSTRAINT uk_laudos_id_empresa UNIQUE (id, empresa_id),
    CONSTRAINT ck_laudos_status CHECK (status IN
        ('AGUARDANDO_ASSINATURA', 'HOMOLOGADO', 'REVISADO_PENDENTE')),
    CONSTRAINT ck_laudos_pdf CHECK (arquivo_mime_type = 'application/pdf'),
    CONSTRAINT ck_laudos_pdf_ext CHECK (LOWER(arquivo_nome) LIKE '%.pdf'),
    CONSTRAINT ck_laudos_tamanho CHECK (arquivo_tamanho > 0),
    CONSTRAINT ck_laudos_validade CHECK (data_validade IS NULL OR data_validade >= data_emissao),
    CONSTRAINT ck_laudos_homologado CHECK (
        status <> 'HOMOLOGADO'
        OR (medico_id IS NOT NULL AND crm_assinatura IS NOT NULL
            AND crm_uf_assinatura IS NOT NULL AND assinado_em IS NOT NULL)),
    CONSTRAINT ck_laudos_revisado CHECK (
        status <> 'REVISADO_PENDENTE'
        OR (medico_id IS NOT NULL AND justificativa_recusa IS NOT NULL
            AND LENGTH(TRIM(justificativa_recusa)) > 0))
);
CREATE INDEX ix_laudos_empresa ON laudos (empresa_id);
CREATE INDEX ix_laudos_fila_assinatura ON laudos (empresa_id)
    WHERE status = 'AGUARDANDO_ASSINATURA';

-- -----------------------------------------------------------------------------
-- 11. NOTIFICAÇÕES in-app e FILA DE E-MAIL - Quadros 13, 14, 16, 17
--     Alertas de vencimento: PREVENTIVO (30/15/7 dias) e URGENCIA (vencido = 0).
--     Eventos: laudo pendente (-> médico), homologado/revisado (-> especialista),
--     periodicidade não configurada (-> especialista), exame agendado.
--     Alerta de vencimento é único por (usuário, item, marco, DIA) - RF31.
-- -----------------------------------------------------------------------------
CREATE TABLE notificacoes (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id      BIGINT       NOT NULL,
    usuario_id      BIGINT       NOT NULL,
    exame_id        BIGINT,
    documento_id    BIGINT,
    laudo_id        BIGINT,
    colaborador_id  BIGINT,
    tipo            VARCHAR(40)  NOT NULL,
    marco_dias      SMALLINT,
    data_referencia DATE         NOT NULL DEFAULT CURRENT_DATE,
    titulo          VARCHAR(200) NOT NULL,
    mensagem        VARCHAR(1000) NOT NULL,
    lida            BOOLEAN      NOT NULL DEFAULT FALSE,
    lida_em         TIMESTAMPTZ,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_notif_usuario FOREIGN KEY (usuario_id, empresa_id)
        REFERENCES usuarios (id, empresa_id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_exame FOREIGN KEY (exame_id, empresa_id)
        REFERENCES exames (id, empresa_id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_documento FOREIGN KEY (documento_id, empresa_id)
        REFERENCES documentos (id, empresa_id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_laudo FOREIGN KEY (laudo_id, empresa_id)
        REFERENCES laudos (id, empresa_id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_colaborador FOREIGN KEY (colaborador_id, empresa_id)
        REFERENCES colaboradores (id, empresa_id) ON DELETE CASCADE,
    CONSTRAINT ck_notif_tipo CHECK (tipo IN
        ('PREVENTIVO', 'URGENCIA', 'LAUDO_PENDENTE_ASSINATURA', 'LAUDO_HOMOLOGADO',
         'LAUDO_REVISADO_PENDENTE', 'EXAME_PENDENTE_ASSINATURA', 'EXAME_HOMOLOGADO',
         'EXAME_REVISADO_PENDENTE', 'PERIODICIDADE_NAO_CONFIGURADA', 'EXAME_AGENDADO')),
    CONSTRAINT ck_notif_marco CHECK (
        (tipo = 'PREVENTIVO' AND marco_dias IN (30, 15, 7))
        OR (tipo = 'URGENCIA' AND marco_dias = 0)
        OR (tipo NOT IN ('PREVENTIVO', 'URGENCIA') AND marco_dias IS NULL)),
    CONSTRAINT ck_notif_um_alvo CHECK (
        (exame_id IS NOT NULL)::INT + (documento_id IS NOT NULL)::INT
        + (laudo_id IS NOT NULL)::INT + (colaborador_id IS NOT NULL)::INT = 1)
);
CREATE UNIQUE INDEX uk_notif_exame_dia ON notificacoes (usuario_id, exame_id, marco_dias, data_referencia)
    WHERE exame_id IS NOT NULL AND marco_dias IS NOT NULL;
CREATE UNIQUE INDEX uk_notif_documento_dia ON notificacoes (usuario_id, documento_id, marco_dias, data_referencia)
    WHERE documento_id IS NOT NULL AND marco_dias IS NOT NULL;
CREATE INDEX ix_notif_usuario_nao_lidas ON notificacoes (usuario_id, criado_em DESC)
    WHERE lida = FALSE;

-- Fila de envio de e-mails. Falha no SMTP => registra ultimo_erro, incrementa
-- tentativas e reagenda para a próxima execução da rotina (proxima_tentativa).
-- notificacao_id é NULL quando o destinatário não é usuário (ex.: colaborador).
CREATE TABLE envios_email (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    notificacao_id    BIGINT,
    destinatario      VARCHAR(255) NOT NULL,
    assunto           VARCHAR(250) NOT NULL,
    corpo             TEXT         NOT NULL,
    sensivel          BOOLEAN      NOT NULL DEFAULT FALSE,   -- corpo apagado após o envio (ex.: senha temporária)
    status            VARCHAR(15)  NOT NULL DEFAULT 'PENDENTE',
    tentativas        SMALLINT     NOT NULL DEFAULT 0,
    max_tentativas    SMALLINT     NOT NULL DEFAULT 5,
    proxima_tentativa TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ultimo_erro       VARCHAR(1000),
    enviado_em        TIMESTAMPTZ,
    criado_em         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_envios_notificacao FOREIGN KEY (notificacao_id)
        REFERENCES notificacoes (id) ON DELETE SET NULL,
    CONSTRAINT ck_envios_status CHECK (status IN ('PENDENTE', 'ENVIADO', 'FALHA_DEFINITIVA')),
    CONSTRAINT ck_envios_tentativas CHECK (tentativas >= 0 AND tentativas <= max_tentativas),
    CONSTRAINT ck_envios_enviado CHECK (status <> 'ENVIADO' OR enviado_em IS NOT NULL)
);
CREATE UNIQUE INDEX uk_envios_notificacao ON envios_email (notificacao_id)
    WHERE notificacao_id IS NOT NULL;
CREATE INDEX ix_envios_pendentes ON envios_email (proxima_tentativa)
    WHERE status = 'PENDENTE';

-- -----------------------------------------------------------------------------
-- 12. LOG DE AUDITORIA - RNF04 / RF17 (cadastro de usuários, relatórios,
--     assinaturas etc.). Registro append-only; empresa_id NULL para ações
--     do Administrador em nível de plataforma.
-- -----------------------------------------------------------------------------
CREATE TABLE logs_auditoria (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id     BIGINT,
    usuario_id     BIGINT,
    acao           VARCHAR(50)  NOT NULL,               -- ex.: CRIAR_USUARIO, GERAR_RELATORIO
    entidade       VARCHAR(50)  NOT NULL,               -- ex.: usuarios, laudos
    entidade_id    BIGINT,
    detalhes       JSONB,
    ip_origem      VARCHAR(45),
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_auditoria_empresa FOREIGN KEY (empresa_id)
        REFERENCES empresas (id) ON DELETE SET NULL,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE SET NULL
);
CREATE INDEX ix_auditoria_empresa_data ON logs_auditoria (empresa_id, criado_em DESC);
CREATE INDEX ix_auditoria_usuario ON logs_auditoria (usuario_id);
CREATE INDEX ix_auditoria_acao ON logs_auditoria (acao);

-- -----------------------------------------------------------------------------
-- 13. AGENTE IA - Quadro 15 / RF27-RF29
--     nr_trechos: base de conhecimento das NRs (global, busca full-text).
--     ia_conversas / ia_mensagens: histórico por usuário.
-- -----------------------------------------------------------------------------
CREATE TABLE nr_trechos (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    norma          VARCHAR(10)  NOT NULL,                 -- ex.: 'NR-07'
    titulo_norma   VARCHAR(200) NOT NULL,
    item           VARCHAR(30),                           -- ex.: '7.5.1'
    conteudo       TEXT         NOT NULL,
    busca          TSVECTOR GENERATED ALWAYS AS
                   (to_tsvector('portuguese', COALESCE(titulo_norma, '') || ' ' || conteudo)) STORED,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_nr_trechos_norma CHECK (norma ~ '^NR-[0-9]{2}$')
);
CREATE INDEX ix_nr_trechos_busca ON nr_trechos USING GIN (busca);
CREATE INDEX ix_nr_trechos_norma ON nr_trechos (norma);

CREATE TABLE ia_conversas (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id     BIGINT       NOT NULL,
    usuario_id     BIGINT       NOT NULL,
    titulo         VARCHAR(200),
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ia_conversas_usuario FOREIGN KEY (usuario_id, empresa_id)
        REFERENCES usuarios (id, empresa_id) ON DELETE CASCADE
);
CREATE INDEX ix_ia_conversas_usuario ON ia_conversas (usuario_id, atualizado_em DESC);

CREATE TABLE ia_mensagens (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversa_id     BIGINT       NOT NULL,
    papel           VARCHAR(15)  NOT NULL,
    conteudo        TEXT         NOT NULL,
    classificacao   VARCHAR(30),                          -- só para papel = ASSISTENTE
    fontes          JSONB,                                -- [{trecho_id, norma, item}]
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ia_mensagens_conversa FOREIGN KEY (conversa_id)
        REFERENCES ia_conversas (id) ON DELETE CASCADE,
    CONSTRAINT ck_ia_mensagens_papel CHECK (papel IN ('USUARIO', 'ASSISTENTE')),
    -- RF29: respondida | fora do domínio | base insuficiente | pedido de laudo/diagnóstico
    CONSTRAINT ck_ia_mensagens_classificacao CHECK (
        (papel = 'USUARIO' AND classificacao IS NULL)
        OR (papel = 'ASSISTENTE' AND classificacao IN
            ('RESPONDIDA', 'FORA_DO_ESCOPO', 'BASE_INSUFICIENTE', 'RECUSADA_LAUDO_DIAGNOSTICO')))
);
CREATE INDEX ix_ia_mensagens_conversa ON ia_mensagens (conversa_id, criado_em);

-- -----------------------------------------------------------------------------
-- 14. TRIGGERS atualizado_em
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'empresas', 'unidades', 'usuarios', 'tipos_exame', 'tipos_documento',
        'tipos_laudo', 'funcoes', 'setores', 'matriz_exames', 'colaboradores',
        'exames', 'documentos', 'laudos', 'envios_email', 'ia_conversas'
    ] LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%1$s_atualizado_em BEFORE UPDATE ON %1$I
             FOR EACH ROW EXECUTE FUNCTION fn_set_atualizado_em()', t);
    END LOOP;
END $$;

COMMIT;
