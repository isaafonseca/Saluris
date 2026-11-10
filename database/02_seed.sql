-- =============================================================================
-- SALURIS IA - Dados iniciais (catálogos globais). Executar após 01_schema.sql.
-- O primeiro usuário ADMINISTRADOR (T.I.) e as empresas serão criados via
-- bootstrap na Fase 2 (o hash BCrypt deve ser gerado pela aplicação).
-- =============================================================================
BEGIN;

-- RN03: periodicidade padrão anual (12) ou bienal (24) por tipo de documento.
-- Valores editáveis pelo Administrador.
INSERT INTO tipos_documento (sigla, nome, descricao, periodicidade_padrao_meses) VALUES
 ('PGR',   'Programa de Gerenciamento de Riscos',                 'NR-01 - inventário de riscos e plano de ação', 24),
 ('PCMSO', 'Programa de Controle Médico de Saúde Ocupacional',    'NR-07 - elaborado com base nos riscos do PGR', 12),
 ('LTCAT', 'Laudo Técnico das Condições do Ambiente de Trabalho', 'Subsídio à aposentadoria especial',            12);

INSERT INTO tipos_laudo (nome, descricao) VALUES
 ('LTCAT',                        'Laudo Técnico das Condições do Ambiente de Trabalho'),
 ('Laudo de Insalubridade',       'NR-15 - atividades e operações insalubres'),
 ('Laudo de Periculosidade',      'NR-16 - atividades e operações perigosas'),
 ('Laudo Ergonômico (AET)',       'NR-17 - análise ergonômica do trabalho');

INSERT INTO tipos_exame (nome, descricao) VALUES
 ('Exame Clínico',     'Avaliação clínica ocupacional'),
 ('Audiometria',       'Avaliação auditiva - exposição a ruído'),
 ('Espirometria',      'Função pulmonar - exposição a poeiras/químicos'),
 ('Acuidade Visual',   'Avaliação visual'),
 ('Hemograma',         'Exame laboratorial'),
 ('Eletrocardiograma', 'Avaliação cardiológica'),
 ('Glicemia',          'Exame laboratorial');

COMMIT;
