-- =============================================================================
-- SALURIS IA - Gestão inteligente de documentos e prazos de SST
-- FASE 3: Base de Conhecimento das Normas Regulamentadoras (NR) para o Agente IA
-- =============================================================================
-- Sínteses técnicas e normativas para consulta contextualizada pelo Agente IA
-- (Quadro 15 / RF27-RF29). Não substituem o texto oficial publicado pelo MTE.
-- =============================================================================

BEGIN;

INSERT INTO nr_trechos (norma, titulo_norma, item, conteudo, ativo) VALUES
('NR-01', 'NR-01 - Disposições Gerais e Gerenciamento de Riscos Ocupacionais (GRO)', '1.5.3',
 'Gerenciamento de Riscos Ocupacionais (GRO) e PGR: A organização deve implementar o gerenciamento de riscos ocupacionais em suas atividades, constituindo um Programa de Gerenciamento de Riscos (PGR). O PGR deve contemplar ou estar integrado com planos, programas e outros documentos previstos na legislação de segurança e saúde no trabalho.', TRUE),

('NR-01', 'NR-01 - Disposições Gerais e Gerenciamento de Riscos Ocupacionais (GRO)', '1.5.4',
 'Documentos mínimos do PGR: O Programa de Gerenciamento de Riscos (PGR) deve conter, no mínimo, dois documentos principais: (a) Inventário de Riscos Ocupacionais, consolidando a identificação de perigos e a avaliação de riscos (físicos, químicos, biológicos, ergonômicos e de acidentes); e (b) Plano de Ação, indicando as medidas de prevenção a serem introduzidas, aprimoradas ou mantidas, com cronograma e acompanhamento.', TRUE),

('NR-01', 'NR-01 - Disposições Gerais e Gerenciamento de Riscos Ocupacionais (GRO)', '1.5.4.4.6',
 'Periodicidade de avaliação e revisão do PGR: A avaliação de riscos do PGR deve ser revista a cada 2 (dois) anos, ou a cada 3 (três) anos para organizações que possuam certificações em sistema de gestão de SST. Também deve ser revisada sempre que ocorrerem inovações tecnológicas, modificações nos processos, acidentes ou doenças relacionadas ao trabalho.', TRUE),

('NR-01', 'NR-01 - Disposições Gerais e Gerenciamento de Riscos Ocupacionais (GRO)', '1.8',
 'Tratamento diferenciado para MEI, ME e EPP: O Microempreendedor Individual (MEI) está dispensado de elaborar o PGR. As Microempresas (ME) e Empresas de Pequeno Porte (EPP), graus de risco 1 e 2, que no levantamento preliminar de perigos não identificarem exposições ocupacionais a agentes físicos, químicos e biológicos e declararem as informações digitais de SST, ficam dispensadas de elaboração do PGR e do PCMSO (mantida a obrigatoriedade dos exames médicos clínicos a cada dois anos).', TRUE),

('NR-01', 'NR-01 - Disposições Gerais e Gerenciamento de Riscos Ocupacionais (GRO)', '1.7',
 'Capacitação e treinamento em Segurança e Saúde no Trabalho: O empregador deve promover capacitação e treinamento dos trabalhadores em conformidade com o disposto nas Normas Regulamentadoras (treinamento inicial, periódico e eventual). Ao término, deve ser emitido certificado contendo nome e assinatura do trabalhador, conteúdo programático, carga horária, data, local e identificação do responsável técnico.', TRUE),

('NR-04', 'NR-04 - Serviços Especializados em Segurança e em Medicina do Trabalho (SESMT)', '4.2',
 'Dimensionamento do SESMT: As organizações que possuam empregados regidos pela CLT manterão Serviços Especializados em Segurança e em Medicina do Trabalho (SESMT), com a finalidade de promover a saúde e proteger a integridade do trabalhador. O dimensionamento do SESMT vincula-se ao Grau de Risco da atividade econômica principal e secundária (1 a 4, conforme CNAE) e ao número total de empregados do estabelecimento.', TRUE),

('NR-05', 'NR-05 - Comissão Interna de Prevenção de Acidentes e de Assédio (CIPA)', '5.3',
 'Objetivo e atribuições da CIPA: A Comissão Interna de Prevenção de Acidentes e de Assédio (CIPA) tem como objetivo a prevenção de acidentes e doenças relacionadas ao trabalho, bem como a prevenção e o combate ao assédio sexual e às demais formas de violência no âmbito do trabalho. O mandato dos membros eleitos da CIPA tem a duração de 1 (um) ano, permitida uma reeleição, com estabilidade provisória.', TRUE),

('NR-06', 'NR-06 - Equipamentos de Proteção Individual (EPI)', '6.5',
 'Obrigações da organização quanto ao EPI e Certificado de Aprovação (CA): A organização é obrigada a fornecer aos empregados, gratuitamente, EPI adequado ao risco, em perfeito estado de conservação e funcionamento, com Certificado de Aprovação (CA) válido emitido pelo Ministério do Trabalho e Emprego. Cabe ao empregador exigir seu uso, orientar e treinar o trabalhador, substituir imediatamente quando danificado ou extraviado e registrar o seu fornecimento (ficha ou sistema eletrônico).', TRUE),

('NR-07', 'NR-07 - Programa de Controle Médico de Saúde Ocupacional (PCMSO)', '7.3',
 'Diretrizes e relação do PCMSO com o PGR: O Programa de Controle Médico de Saúde Ocupacional (PCMSO) é parte integrante do conjunto mais amplo de iniciativas da organização no campo da saúde de seus empregados, devendo ser elaborado e implementado com base nos riscos à saúde dos empregados, especialmente os identificados nas avaliações previstas nas demais NRs e no Inventário de Riscos do PGR.', TRUE),

('NR-07', 'NR-07 - Programa de Controle Médico de Saúde Ocupacional (PCMSO)', '7.5.6',
 'Exames médicos obrigatórios do PCMSO: O PCMSO deve incluir a realização obrigatória dos exames médicos: (a) admissional (antes que o empregado assuma suas atividades); (b) periódico; (c) de retorno ao trabalho (no primeiro dia da volta ao trabalho de empregado ausente por período igual ou superior a 30 dias por motivo de doença ou acidente, de natureza ocupacional ou não); (d) de mudança de riscos ocupacionais (antes da data da mudança); e (e) demissional.', TRUE),

('NR-07', 'NR-07 - Programa de Controle Médico de Saúde Ocupacional (PCMSO)', '7.5.8',
 'Periodicidade dos exames médicos periódicos: O exame clínico periódico deve ser realizado de acordo com os seguintes intervalos: (I) para empregados expostos a riscos ocupacionais identificados e classificados no PGR e para portadores de doenças crônicas que aumentem a susceptibilidade a tais riscos: a cada ano (12 meses) ou a intervalos menores, a critério do médico responsável; (II) para os demais empregados: a cada 2 (dois) anos (24 meses).', TRUE),

('NR-07', 'NR-07 - Programa de Controle Médico de Saúde Ocupacional (PCMSO)', '7.5.11',
 'Exame demissional e prazos de dispensa: No exame demissional, o exame clínico deve ser realizado em até 10 (dez) dias contados do término do contrato, podendo ser dispensado caso o exame médico ocupacional mais recente tenha sido realizado há menos de 135 (cento e trinta e cinco) dias, para as organizações de graus de risco 1 e 2, e há menos de 90 (noventa) dias, para as organizações de graus de risco 3 e 4.', TRUE),

('NR-07', 'NR-07 - Programa de Controle Médico de Saúde Ocupacional (PCMSO)', '7.5.19',
 'Atestado de Saúde Ocupacional (ASO): Para cada exame clínico ocupacional realizado, o médico emitirá Atestado de Saúde Ocupacional (ASO), que deve conter no mínimo: razão social e CNPJ da organização, nome completo e CPF do empregado e sua função, descrição dos perigos ou fatores de risco identificados no PGR, indicação e data de realização dos exames clínicos e complementares, definição de apto ou inapto para a função, nome e número de inscrição no CRM do médico responsável e data/assinatura.', TRUE),

('NR-07', 'NR-07 - Programa de Controle Médico de Saúde Ocupacional (PCMSO)', 'Anexo I',
 'Controle médico da exposição a níveis de pressão sonora elevados (Audiometria): Devem ser submetidos a exames audiométricos de referência e sequenciais todos os empregados que exerçam ou exercerão suas atividades em ambientes cujos níveis de pressão sonora superem os limites de tolerância da NR-15. O exame audiométrico deve ser realizado no momento da admissão, no 6º (sexto) mês após a mesma, anualmente a partir de então e na demissão.', TRUE),

('NR-07', 'NR-07 - Programa de Controle Médico de Saúde Ocupacional (PCMSO)', '7.6',
 'Relatório Analítico do PCMSO e prontuário médico: O médico responsável pelo PCMSO deve elaborar anualmente um Relatório Analítico do Programa, considerando a incidência e prevalência de doenças relacionadas ao trabalho e estatísticas de exames. Os dados obtidos nos exames médicos e complementares devem ser registrados em prontuário médico individual sob responsabilidade do médico, mantido por no mínimo 20 (vinte) anos após o desligamento do empregado.', TRUE),

('NR-09', 'NR-09 - Avaliação e Controle das Exposições Ocupacionais a Agentes Físicos, Químicos e Biológicos', '9.3',
 'Identificação e avaliação das exposições ocupacionais: A NR-09 estabelece os requisitos para a avaliação das exposições ocupacionais a agentes físicos (ruído, calor, vibrações, radiações), químicos e biológicos, quando identificados no Programa de Gerenciamento de Riscos (PGR) da NR-01, subsidiando o PGR quanto às medidas de prevenção. A avaliação quantitativa deve ser representativa da exposição real dos trabalhadores (Grupos Homogêneos de Exposição).', TRUE),

('NR-10', 'NR-10 - Segurança em Instalações e Serviços em Eletricidade', '10.8',
 'Habilitação, qualificação, capacitação e autorização em eletricidade: Os trabalhadores autorizados a intervir em instalações elétricas devem possuir treinamento específico de segurança (Curso Básico da NR-10 com carga horária mínima de 40 horas, e Curso Complementar SEP quando aplicável) e ser submetidos a avaliação de saúde compatível com as atividades. Deve ser realizado treinamento de reciclagem bienal (a cada 2 anos) e sempre que ocorrer troca de função, retorno de afastamento superior a 3 meses ou modificações significativas nas instalações.', TRUE),

('NR-12', 'NR-12 - Segurança no Trabalho em Máquinas e Equipamentos', '12.16',
 'Capacitação em operação de máquinas e equipamentos: A operação, manutenção, inspeção e demais intervenções em máquinas e equipamentos devem ser realizadas por trabalhadores habilitados, qualificados ou capacitados e autorizados para este fim. Os trabalhadores devem receber capacitação providenciada pelo empregador e compatível com suas funções, antes que assumam a função, abordando os riscos a que estão expostos e as medidas de proteção existentes.', TRUE),

('NR-15', 'NR-15 - Atividades e Operações Insalubres', '15.2',
 'Caracterização de insalubridade e adicionais: O exercício de trabalho em condições de insalubridade, acima dos limites de tolerância estabelecidos nos Anexos da NR-15 (ruído contínuo ou de impacto, calor, radiações, agentes químicos, poeiras minerais, agentes biológicos etc.), assegura ao trabalhador a percepção de adicional incidente sobre o salário mínimo da região, equivalente a: 40% para insalubridade de grau máximo, 20% para grau médio e 10% para grau mínimo. A comprovação é feita por laudo técnico de engenheiro de segurança do trabalho ou médico do trabalho.', TRUE),

('NR-15', 'NR-15 - Atividades e Operações Insalubres', 'Anexo 1',
 'Limites de tolerância para ruído contínuo ou intermitente: Entende-se por ruído contínuo ou intermitente, para os fins de aplicação de Limites de Tolerância, o ruído que não seja ruído de impacto. Para uma jornada diária de 8 (oito) horas de trabalho, a máxima exposição diária permissível é de 85 dB(A). A cada acréscimo de 5 dB(A), o tempo máximo de exposição permitido reduz-se à metade (ex.: 90 dB(A) permite no máximo 4 horas diárias sem proteção eficaz).', TRUE),

('NR-16', 'NR-16 - Atividades e Operações Perigosas', '16.2',
 'Caracterização de periculosidade e adicional de 30%: São consideradas atividades ou operações perigosas aquelas que, por sua natureza ou métodos de trabalho, impliquem risco acentuado em virtude de exposição permanente do trabalhador a: inflamáveis, explosivos, energia elétrica, roubos ou outras espécies de violência física nas atividades profissionais de segurança pessoal ou patrimonial, radiações ionizantes e atividades em motocicleta. O trabalho em condições de periculosidade assegura ao trabalhador um adicional de 30% (trinta por cento) sobre o salário, sem os acréscimos resultantes de gratificações, prêmios ou participação nos lucros.', TRUE),

('NR-17', 'NR-17 - Ergonomia', '17.3',
 'Avaliação Ergonômica Preliminar (AEP) e Análise Ergonômica do Trabalho (AET): A organização deve realizar a Avaliação Ergonômica Preliminar (AEP) das situações de trabalho que, em decorrência da natureza e conteúdo das atividades requeridas, demandam adaptação às características psicofisiológicas dos trabalhadores, integrando-a ao PGR. A Análise Ergonômica do Trabalho (AET) aprofundada deve ser realizada quando observada a necessidade de uma avaliação mais detalhada, na insuficiência das ações adotadas ou quando sugerida pelo acompanhamento de saúde dos trabalhadores.', TRUE),

('NR-23', 'NR-23 - Proteção Contra Incêndios', '23.1',
 'Prevenção e combate a incêndios: Todas as organizações devem adotar medidas de prevenção contra incêndios em conformidade com a legislação estadual e, quando aplicável, com as normas técnicas oficiais. O empregador deve providenciar para todos os trabalhadores informações sobre utilização dos equipamentos de combate ao incêndio, procedimentos para evacuação dos locais de trabalho com segurança e dispositivos de alarme existentes.', TRUE),

('NR-33', 'NR-33 - Segurança e Saúde nos Trabalhos em Espaços Confinados', '33.5',
 'Avaliação médica e capacitação para trabalho em espaços confinados: Todo trabalhador designado para trabalhos em espaços confinados deve ser submetido a exames médicos específicos para a função que irá desempenhar, conforme estabelecem as NRs 07 e 33, incluindo os fatores de riscos psicossociais, com a emissão do respectivo ASO. É vedada a designação para trabalhos em espaços confinados sem a prévia capacitação do trabalhador (vigias e trabalhadores autorizados: carga horária mínima de 16 horas na inicial e 8 horas na periódica anual; supervisores de entrada: 40 horas na inicial).', TRUE),

('NR-35', 'NR-35 - Trabalho em Altura', '35.2',
 'Definição, aptidão médica e treinamento para trabalho em altura: Considera-se trabalho em altura toda atividade executada acima de 2,00 m (dois metros) do nível inferior, onde haja risco de queda. Cabe ao empregador garantir a realização de avaliação prévia das condições no local, assegurar que todo trabalho em altura só se inicie depois de adotadas as medidas de proteção e promover treinamento teórico e prático para trabalho em altura com carga horária mínima de 8 (oito) horas, com periodicidade bienal (a cada 2 anos). A aptidão para trabalho em altura deve ser consignada no ASO do trabalhador.', TRUE);

COMMIT;
