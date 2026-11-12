// Base completa de dados fictícios realistas de SST para demonstração imediata de todas as telas e fluxos

const today = new Date();
function addDays(days) {
  const d = new Date(today);
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 10);
}
function addMonths(months) {
  const d = new Date(today);
  d.setMonth(d.getMonth() + months);
  return d.toISOString().slice(0, 10);
}
function isoDateTime(daysOffset = 0, hoursOffset = 0) {
  const d = new Date(today);
  d.setDate(d.getDate() + daysOffset);
  d.setHours(d.getHours() + hoursOffset);
  return d.toISOString();
}

export const DEMO_STATE = {
  empresas: [
    {
      id: 1,
      cnpj: '11222333000181',
      razaoSocial: 'Porto Logística e Operações Portuárias S.A.',
      nomeFantasia: 'PortoLog Santos',
      setorAtuacao: 'Operações Portuárias e Logística Industrial',
      porte: 'MEDIO',
      contatoNome: 'Mariana Costa',
      emailContato: 'contato@portolog.com.br',
      telefone: '(13) 3222-4500',
      adminEmail: 'empresa@saluris.com.br',
      adminNome: 'Conta Principal PortoLog Santos',
      ativa: true,
    },
    {
      id: 2,
      cnpj: '45723174000110',
      razaoSocial: 'Metalúrgica e Caldeiraria Atlântica Ltda.',
      nomeFantasia: 'Metalúrgica Atlântica Cubatão',
      setorAtuacao: 'Siderurgia, Caldeiraria e Manutenção Industrial',
      porte: 'GRANDE',
      contatoNome: 'Roberto Figueiredo',
      emailContato: 'sst@metalurgicaatlantica.com.br',
      telefone: '(13) 3361-8800',
      adminEmail: 'admin@metalurgicaatlantica.com.br',
      adminNome: 'Conta Principal Metalúrgica Atlântica',
      ativa: true,
    },
    {
      id: 3,
      cnpj: '84046132000133',
      razaoSocial: 'Construtora e Engenharia Litoral Sul S.A.',
      nomeFantasia: 'Construtora Litoral Sul',
      setorAtuacao: 'Construção Civil e Obras Portuárias',
      porte: 'MEDIO',
      contatoNome: 'Patrícia Nogueira',
      emailContato: 'rh@construtoralitoralsul.com.br',
      telefone: '(13) 3289-1900',
      adminEmail: 'admin@construtoralitoralsul.com.br',
      adminNome: 'Conta Principal Construtora Litoral Sul',
      ativa: true,
    },
  ],

  usuarios: [
    {
      id: 1,
      nome: 'Administrador Geral (T.I.)',
      email: 'admin@saluris.com.br',
      perfil: 'ADMINISTRADOR',
      empresaId: null,
      empresaNome: null,
      crm: null,
      crmUf: null,
      ativo: true,
      deveTrocarSenha: false,
      ultimoAcesso: isoDateTime(0, -1),
    },
    {
      id: 2,
      nome: 'Carlos Eduardo Mendes (Especialista SST)',
      email: 'especialista@saluris.com.br',
      perfil: 'ESPECIALISTA_SST',
      empresaId: 1,
      empresaNome: 'Porto Logística e Operações Portuárias S.A.',
      crm: null,
      crmUf: null,
      ativo: true,
      deveTrocarSenha: false,
      ultimoAcesso: isoDateTime(0, -2),
    },
    {
      id: 3,
      nome: 'Dra. Helena Prado (Médica do Trabalho)',
      email: 'medico@saluris.com.br',
      perfil: 'MEDICO_TRABALHO',
      empresaId: 1,
      empresaNome: 'Porto Logística e Operações Portuárias S.A.',
      crm: '148920',
      crmUf: 'SP',
      ativo: true,
      deveTrocarSenha: false,
      ultimoAcesso: isoDateTime(0, -3),
    },
    {
      id: 4,
      nome: 'Fernanda Almeida (Gestora de RH)',
      email: 'rh@saluris.com.br',
      perfil: 'RH_EMPRESA',
      empresaId: 1,
      empresaNome: 'Porto Logística e Operações Portuárias S.A.',
      crm: null,
      crmUf: null,
      ativo: true,
      deveTrocarSenha: false,
      ultimoAcesso: isoDateTime(0, -1),
    },
    {
      id: 5,
      nome: 'Eng. Ricardo Tavares',
      email: 'ricardo.sst@metalurgicaatlantica.com.br',
      perfil: 'ESPECIALISTA_SST',
      empresaId: 2,
      empresaNome: 'Metalúrgica e Caldeiraria Atlântica Ltda.',
      crm: null,
      crmUf: null,
      ativo: true,
      deveTrocarSenha: false,
      ultimoAcesso: isoDateTime(-1, -4),
    },
    {
      id: 6,
      nome: 'Dr. Roberto Campos',
      email: 'roberto.med@saluris.com.br',
      perfil: 'MEDICO_TRABALHO',
      empresaId: 2,
      empresaNome: 'Metalúrgica e Caldeiraria Atlântica Ltda.',
      crm: '112450',
      crmUf: 'SP',
      ativo: true,
      deveTrocarSenha: false,
      ultimoAcesso: isoDateTime(-2, -1),
    },
    {
      id: 7,
      nome: 'Juliana Martins (Analista de RH)',
      email: 'juliana.rh@metalurgicaatlantica.com.br',
      perfil: 'RH_EMPRESA',
      empresaId: 2,
      empresaNome: 'Metalúrgica e Caldeiraria Atlântica Ltda.',
      crm: null,
      crmUf: null,
      ativo: true,
      deveTrocarSenha: false,
      ultimoAcesso: isoDateTime(-1, -2),
    },
    {
      id: 8,
      nome: 'Conta Principal PortoLog (Admin da Empresa)',
      email: 'empresa@saluris.com.br',
      perfil: 'ADMIN_EMPRESA',
      empresaId: 1,
      empresaNome: 'Porto Logística e Operações Portuárias S.A.',
      crm: null,
      crmUf: null,
      ativo: true,
      deveTrocarSenha: false,
      ultimoAcesso: isoDateTime(0, -1),
    },
  ],

  tiposExame: [
    {
      id: 1,
      nome: 'Avaliação Clínica Ocupacional (ASO)',
      descricao: 'Anamnese clínica e exame físico ocupacional obrigatório para emissão do ASO.',
      observacoesTecnicas: 'NR-07 item 7.5.8 - Periodicidade de 12 ou 24 meses conforme risco ocupacional no PGR.',
      ativo: true,
    },
    {
      id: 2,
      nome: 'Audiometria Tonal e Vocal',
      descricao: 'Avaliação da acuidade auditiva para trabalhadores expostos a níveis elevados de pressão sonora.',
      observacoesTecnicas: 'NR-07 Anexo II - Repouso acústico mínimo de 14 horas antes do exame.',
      ativo: true,
    },
    {
      id: 3,
      nome: 'Acuidade Visual',
      descricao: 'Avaliação oftalmológica ocupacional para operação de equipamentos móveis e trabalho em altura.',
      observacoesTecnicas: 'Obrigatório para operadores de empilhadeira, guindaste e motoristas (NR-11 / NR-35).',
      ativo: true,
    },
    {
      id: 4,
      nome: 'Eletrocardiograma (ECG)',
      descricao: 'Avaliação cardiológica complementar para atividades críticas em eletricidade e trabalho em altura.',
      observacoesTecnicas: 'NR-10 e NR-35 - Rastreamento de arritmias e cardiopatias.',
      ativo: true,
    },
    {
      id: 5,
      nome: 'Eletroencefalograma (EEG)',
      descricao: 'Avaliação neurológica complementar para atividades em altura acima de 2 metros e espaço confinado.',
      observacoesTecnicas: 'NR-33 e NR-35 - Prevenção de mal súbito e crises convulsivas.',
      ativo: true,
    },
    {
      id: 6,
      nome: 'Espirometria (Prova de Função Pulmonar)',
      descricao: 'Avaliação da capacidade ventilatória pulmonar para expostos a poeiras, fumos metálicos e gases.',
      observacoesTecnicas: 'NR-07 Quadro 2 - Indicado para soldadores e operadores de granel sólido.',
      ativo: true,
    },
    {
      id: 7,
      nome: 'Raio-X de Tórax (Padrão OIT)',
      descricao: 'Radiografia de tórax conforme diretrizes da OIT para exposição a aerodispersoides fibrogênicos.',
      observacoesTecnicas: 'NR-07 Anexo IV - Leitura radiológica padronizada OIT.',
      ativo: true,
    },
    {
      id: 8,
      nome: 'Hemograma Completo e Glicemia de Jejum',
      descricao: 'Avaliação laboratorial complementar para trabalho em altura, espaço confinado e agentes químicos.',
      observacoesTecnicas: 'Jejum mínimo de 8 horas para dosagem glicêmica (NR-35).',
      ativo: true,
    },
  ],

  tiposDocumento: [
    {
      id: 1,
      sigla: 'PGR',
      nome: 'Programa de Gerenciamento de Riscos',
      descricao: 'Inventário de Riscos Ocupacionais e Plano de Ação conforme NR-01 (GRO).',
      periodicidadePadraoMeses: 24,
      ativo: true,
    },
    {
      id: 2,
      sigla: 'PCMSO',
      nome: 'Programa de Controle Médico de Saúde Ocupacional',
      descricao: 'Planejamento médico e matriz de exames elaborado com base nos riscos do PGR (NR-07).',
      periodicidadePadraoMeses: 12,
      ativo: true,
    },
    {
      id: 3,
      sigla: 'LTCAT',
      nome: 'Laudo Técnico das Condições Ambientais do Trabalho',
      descricao: 'Laudo conclusivo sobre exposição a agentes nocivos para fins de aposentadoria especial (INSS).',
      periodicidadePadraoMeses: 24,
      ativo: true,
    },
    {
      id: 4,
      sigla: 'AET',
      nome: 'Análise Ergonômica do Trabalho',
      descricao: 'Avaliação ergonômica das condições psicofisiológicas e biomecânicas dos postos (NR-17).',
      periodicidadePadraoMeses: 24,
      ativo: true,
    },
    {
      id: 5,
      sigla: 'PCA',
      nome: 'Programa de Conservação Auditiva',
      descricao: 'Medidas técnicas e administrativas para prevenção da perda auditiva induzida por ruído (NR-07/NR-09).',
      periodicidadePadraoMeses: 12,
      ativo: true,
    },
    {
      id: 6,
      sigla: 'PPR',
      nome: 'Programa de Proteção Respiratória',
      descricao: 'Seleção, ensaio de vedação e uso de equipamentos de proteção respiratória (Fundacentro / NR-06).',
      periodicidadePadraoMeses: 12,
      ativo: true,
    },
  ],

  tiposLaudo: [
    {
      id: 1,
      nome: 'LTCAT - Condições Ambientais do Trabalho',
      descricao: 'Avaliação quantitativa de agentes físicos, químicos e biológicos no ambiente laboral.',
      observacoes: 'Exige assinatura e validação para disponibilização à empresa cliente.',
      ativo: true,
    },
    {
      id: 2,
      nome: 'Laudo Técnico de Insalubridade (NR-15)',
      descricao: 'Caracterização ou descaracterização de adicional de insalubridade (graus mínimo, médio ou máximo).',
      observacoes: 'Fundamentado nos Anexos 1 a 14 da NR-15 do MTE.',
      ativo: true,
    },
    {
      id: 3,
      nome: 'Laudo Técnico de Periculosidade (NR-16)',
      descricao: 'Avaliação de exposição a inflamáveis, explosivos, energia elétrica e segurança patrimonial.',
      observacoes: 'Fundamentado nos Anexos da NR-16 (adicional de 30%).',
      ativo: true,
    },
    {
      id: 4,
      nome: 'Parecer Técnico de Ergonomia (NR-17)',
      descricao: 'Avaliação ergonômica preliminar (AEP) e laudo biomecânico de postos operacionais.',
      observacoes: 'Integrado ao inventário de riscos do PGR.',
      ativo: true,
    },
  ],

  unidades: [
    { id: 1, nome: 'Terminal Margem Direita - Santos', endereco: 'Av. Eng. Augusto Barata, s/n - Alemoa', cidade: 'Santos', uf: 'SP', ativa: true },
    { id: 2, nome: 'Pátio Logístico Retroportuário - Guarujá', endereco: 'Rod. Cônego Domênico Rangoni, km 268', cidade: 'Guarujá', uf: 'SP', ativa: true },
    { id: 3, nome: 'Base Industrial e Manutenção - Cubatão', endereco: 'Av. Henry Borden, 1420 - Vila Parisi', cidade: 'Cubatão', uf: 'SP', ativa: true },
  ],

  funcoes: [
    { id: 1, nome: 'Operador de Empilhadeira', cbo: '7822-20', descricao: 'Operação de empilhadeira elétrica e GLP na movimentação de contêineres e paletes (NR-11).', ativa: true },
    { id: 2, nome: 'Eletricista de Manutenção Industrial', cbo: '9511-05', descricao: 'Manutenção preventiva e corretiva em painéis de média/baixa tensão e iluminação em altura (NR-10 / NR-35).', ativa: true },
    { id: 3, nome: 'Soldador Industrial', cbo: '7243-15', descricao: 'Soldagem MIG/TIG e corte oxiacetilênico em estruturas metálicas e contêineres (NR-12 / NR-15).', ativa: true },
    { id: 4, nome: 'Conferente de Carga Portuária', cbo: '4142-15', descricao: 'Conferência documental e inspeção visual de cargas no cais e armazéns.', ativa: true },
    { id: 5, nome: 'Analista Administrativo', cbo: '2521-05', descricao: 'Atividades administrativas, faturamento e controle documental em escritório climatizado (NR-17).', ativa: true },
  ],

  setores: [
    { id: 1, nome: 'Operações de Pátio e Cais', descricao: 'Área operacional externa com movimentação de máquinas pesadas e ruído intermitente (86 dB(A)).', ativo: true },
    { id: 2, nome: 'Manutenção Elétrica e Mecânica', descricao: 'Oficina industrial com risco elétrico, trabalho em altura e uso de ferramentas rotativas.', ativo: true },
    { id: 3, nome: 'Caldeiraria e Solda', descricao: 'Área de reparo estrutural com exposição a fumos metálicos, radiação não ionizante e ruído.', ativo: true },
    { id: 4, nome: 'Armazém Geral e Logística', descricao: 'Galpão coberto de estocagem, paletização e expedição de mercadorias.', ativo: true },
    { id: 5, nome: 'Administrativo e Gestão', descricao: 'Escritórios administrativos climatizados com risco ergonômico leve.', ativo: true },
  ],

  matrizExames: [
    {
      id: 1,
      funcaoId: 1,
      funcaoNome: 'Operador de Empilhadeira',
      setorId: 1,
      setorNome: 'Operações de Pátio e Cais',
      tipoExameId: 1,
      tipoExameNome: 'Avaliação Clínica Ocupacional (ASO)',
      periodicidadeMeses: 12,
      obrigatorio: true,
      configuradoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      atualizadoEm: isoDateTime(-15),
    },
    {
      id: 2,
      funcaoId: 1,
      funcaoNome: 'Operador de Empilhadeira',
      setorId: 1,
      setorNome: 'Operações de Pátio e Cais',
      tipoExameId: 2,
      tipoExameNome: 'Audiometria Tonal e Vocal',
      periodicidadeMeses: 6,
      obrigatorio: true,
      configuradoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      atualizadoEm: isoDateTime(-15),
    },
    {
      id: 3,
      funcaoId: 1,
      funcaoNome: 'Operador de Empilhadeira',
      setorId: 1,
      setorNome: 'Operações de Pátio e Cais',
      tipoExameId: 3,
      tipoExameNome: 'Acuidade Visual',
      periodicidadeMeses: 12,
      obrigatorio: true,
      configuradoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      atualizadoEm: isoDateTime(-15),
    },
    {
      id: 4,
      funcaoId: 2,
      funcaoNome: 'Eletricista de Manutenção Industrial',
      setorId: 2,
      setorNome: 'Manutenção Elétrica e Mecânica',
      tipoExameId: 1,
      tipoExameNome: 'Avaliação Clínica Ocupacional (ASO)',
      periodicidadeMeses: 12,
      obrigatorio: true,
      configuradoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      atualizadoEm: isoDateTime(-12),
    },
    {
      id: 5,
      funcaoId: 2,
      funcaoNome: 'Eletricista de Manutenção Industrial',
      setorId: 2,
      setorNome: 'Manutenção Elétrica e Mecânica',
      tipoExameId: 4,
      tipoExameNome: 'Eletrocardiograma (ECG)',
      periodicidadeMeses: 12,
      obrigatorio: true,
      configuradoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      atualizadoEm: isoDateTime(-12),
    },
    {
      id: 6,
      funcaoId: 2,
      funcaoNome: 'Eletricista de Manutenção Industrial',
      setorId: 2,
      setorNome: 'Manutenção Elétrica e Mecânica',
      tipoExameId: 5,
      tipoExameNome: 'Eletroencefalograma (EEG)',
      periodicidadeMeses: 12,
      obrigatorio: true,
      configuradoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      atualizadoEm: isoDateTime(-12),
    },
    {
      id: 7,
      funcaoId: 3,
      funcaoNome: 'Soldador Industrial',
      setorId: 3,
      setorNome: 'Caldeiraria e Solda',
      tipoExameId: 6,
      tipoExameNome: 'Espirometria (Prova de Função Pulmonar)',
      periodicidadeMeses: 12,
      obrigatorio: true,
      configuradoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      atualizadoEm: isoDateTime(-10),
    },
    {
      id: 8,
      funcaoId: 3,
      funcaoNome: 'Soldador Industrial',
      setorId: 3,
      setorNome: 'Caldeiraria e Solda',
      tipoExameId: 2,
      tipoExameNome: 'Audiometria Tonal e Vocal',
      periodicidadeMeses: 6,
      obrigatorio: true,
      configuradoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      atualizadoEm: isoDateTime(-10),
    },
    {
      id: 9,
      funcaoId: 4,
      funcaoNome: 'Conferente de Carga Portuária',
      setorId: 4,
      setorNome: 'Armazém Geral e Logística',
      tipoExameId: 1,
      tipoExameNome: 'Avaliação Clínica Ocupacional (ASO)',
      periodicidadeMeses: 12,
      obrigatorio: true,
      configuradoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      atualizadoEm: isoDateTime(-8),
    },
    {
      id: 10,
      funcaoId: 5,
      funcaoNome: 'Analista Administrativo',
      setorId: 5,
      setorNome: 'Administrativo e Gestão',
      tipoExameId: 1,
      tipoExameNome: 'Avaliação Clínica Ocupacional (ASO)',
      periodicidadeMeses: 24,
      obrigatorio: true,
      configuradoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      atualizadoEm: isoDateTime(-5),
    },
  ],

  colaboradores: [
    {
      id: 1,
      nome: 'João Marcos da Silva',
      cpf: '52998224725',
      dataNascimento: '1989-05-14',
      dataAdmissao: addMonths(-11),
      dataDemissao: null,
      email: 'joao.silva@portolog.com.br',
      ativo: true,
      unidadeId: 1,
      unidadeNome: 'Terminal Margem Direita - Santos',
      funcaoId: 1,
      funcaoNome: 'Operador de Empilhadeira',
      setorId: 1,
      setorNome: 'Operações de Pátio e Cais',
      statusConformidade: 'VENCIDO',
    },
    {
      id: 2,
      nome: 'Beatriz Santos Oliveira',
      cpf: '11144477735',
      dataNascimento: '1994-08-22',
      dataAdmissao: addMonths(-6),
      dataDemissao: null,
      email: 'beatriz.oliveira@portolog.com.br',
      ativo: true,
      unidadeId: 2,
      unidadeNome: 'Pátio Logístico Retroportuário - Guarujá',
      funcaoId: 2,
      funcaoNome: 'Eletricista de Manutenção Industrial',
      setorId: 2,
      setorNome: 'Manutenção Elétrica e Mecânica',
      statusConformidade: 'PROXIMO_VENCIMENTO',
    },
    {
      id: 3,
      nome: 'Rafael Pereira Souza',
      cpf: '98765432100',
      dataNascimento: '1997-02-10',
      dataAdmissao: addMonths(-3),
      dataDemissao: null,
      email: 'rafael.souza@portolog.com.br',
      ativo: true,
      unidadeId: 1,
      unidadeNome: 'Terminal Margem Direita - Santos',
      funcaoId: 5,
      funcaoNome: 'Analista Administrativo',
      setorId: 5,
      setorNome: 'Administrativo e Gestão',
      statusConformidade: 'EM_DIA',
    },
    {
      id: 4,
      nome: 'Marcos Vinícius Rocha',
      cpf: '28625587887',
      dataNascimento: '1985-11-03',
      dataAdmissao: addMonths(-14),
      dataDemissao: null,
      email: 'marcos.rocha@portolog.com.br',
      ativo: true,
      unidadeId: 3,
      unidadeNome: 'Base Industrial e Manutenção - Cubatão',
      funcaoId: 3,
      funcaoNome: 'Soldador Industrial',
      setorId: 3,
      setorNome: 'Caldeiraria e Solda',
      statusConformidade: 'PROXIMO_VENCIMENTO',
    },
    {
      id: 5,
      nome: 'Camila Ribeiro Nascimento',
      cpf: '02819112085',
      dataNascimento: '1992-04-19',
      dataAdmissao: addMonths(-9),
      dataDemissao: null,
      email: 'camila.nascimento@portolog.com.br',
      ativo: true,
      unidadeId: 1,
      unidadeNome: 'Terminal Margem Direita - Santos',
      funcaoId: 4,
      funcaoNome: 'Conferente de Carga Portuária',
      setorId: 4,
      setorNome: 'Armazém Geral e Logística',
      statusConformidade: 'EM_DIA',
    },
    {
      id: 6,
      nome: 'André Luiz Gonçalves',
      cpf: '12345678909',
      dataNascimento: '1988-09-27',
      dataAdmissao: addMonths(-18),
      dataDemissao: null,
      email: 'andre.goncalves@portolog.com.br',
      ativo: true,
      unidadeId: 2,
      unidadeNome: 'Pátio Logístico Retroportuário - Guarujá',
      funcaoId: 1,
      funcaoNome: 'Operador de Empilhadeira',
      setorId: 1,
      setorNome: 'Operações de Pátio e Cais',
      statusConformidade: 'VENCIDO',
    },
  ],

  exames: [
    {
      id: 1,
      colaboradorId: 1,
      colaboradorNome: 'João Marcos da Silva',
      tipoExameId: 2,
      tipoExameNome: 'Audiometria Tonal e Vocal',
      periodicidadeMeses: 6,
      dataAgendada: null,
      localClinica: null,
      observacoes: null,
      dataRealizacao: null,
      dataVencimento: addDays(-6),
      diasParaVencer: -6,
      statusValidade: 'VENCIDO',
      statusFluxo: 'PENDENTE',
      possuiArquivo: false,
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
    },
    {
      id: 2,
      colaboradorId: 1,
      colaboradorNome: 'João Marcos da Silva',
      tipoExameId: 1,
      tipoExameNome: 'Avaliação Clínica Ocupacional (ASO)',
      periodicidadeMeses: 12,
      dataAgendada: addDays(5),
      localClinica: 'Clínica MedSantos Ocupacional - Av. Ana Costa, 250',
      observacoes: 'Comparecer com documento com foto às 08h30.',
      dataRealizacao: null,
      dataVencimento: addDays(15),
      diasParaVencer: 15,
      statusValidade: 'PROXIMO_VENCIMENTO',
      statusFluxo: 'AGENDADO',
      possuiArquivo: false,
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
    },
    {
      id: 3,
      colaboradorId: 1,
      colaboradorNome: 'João Marcos da Silva',
      tipoExameId: 3,
      tipoExameNome: 'Acuidade Visual',
      periodicidadeMeses: 12,
      dataAgendada: addDays(-2),
      localClinica: 'Clínica MedSantos Ocupacional - Av. Ana Costa, 250',
      observacoes: 'Exame realizado, aguardando homologação médica.',
      dataRealizacao: addDays(-1),
      dataVencimento: addMonths(12),
      diasParaVencer: 365,
      statusValidade: 'EM_DIA',
      statusFluxo: 'AGUARDANDO_ASSINATURA',
      possuiArquivo: true,
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
    },
    {
      id: 4,
      colaboradorId: 2,
      colaboradorNome: 'Beatriz Santos Oliveira',
      tipoExameId: 4,
      tipoExameNome: 'Eletrocardiograma (ECG)',
      periodicidadeMeses: 12,
      dataAgendada: addDays(3),
      localClinica: 'Centro Médico Ocupacional Guarujá - Av. Puglisi, 410',
      observacoes: 'Exame periódico obrigatório para NR-10.',
      dataRealizacao: null,
      dataVencimento: addDays(7),
      diasParaVencer: 7,
      statusValidade: 'PROXIMO_VENCIMENTO',
      statusFluxo: 'AGENDADO',
      possuiArquivo: false,
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
    },
    {
      id: 5,
      colaboradorId: 2,
      colaboradorNome: 'Beatriz Santos Oliveira',
      tipoExameId: 5,
      tipoExameNome: 'Eletroencefalograma (EEG)',
      periodicidadeMeses: 12,
      dataAgendada: addDays(-3),
      localClinica: 'Centro Médico Ocupacional Guarujá',
      observacoes: 'Laudo enviado para assinatura da Dra. Helena Prado.',
      dataRealizacao: addDays(-2),
      dataVencimento: addMonths(12),
      diasParaVencer: 363,
      statusValidade: 'EM_DIA',
      statusFluxo: 'AGUARDANDO_ASSINATURA',
      possuiArquivo: true,
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
    },
    {
      id: 6,
      colaboradorId: 2,
      colaboradorNome: 'Beatriz Santos Oliveira',
      tipoExameId: 1,
      tipoExameNome: 'Avaliação Clínica Ocupacional (ASO)',
      periodicidadeMeses: 12,
      dataAgendada: null,
      localClinica: null,
      observacoes: null,
      dataRealizacao: null,
      dataVencimento: addMonths(6),
      diasParaVencer: 180,
      statusValidade: 'EM_DIA',
      statusFluxo: 'PENDENTE',
      possuiArquivo: false,
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
    },
    {
      id: 7,
      colaboradorId: 3,
      colaboradorNome: 'Rafael Pereira Souza',
      tipoExameId: 1,
      tipoExameNome: 'Avaliação Clínica Ocupacional (ASO)',
      periodicidadeMeses: 24,
      dataAgendada: null,
      localClinica: null,
      observacoes: null,
      dataRealizacao: addMonths(-3),
      dataVencimento: addMonths(21),
      diasParaVencer: 630,
      statusValidade: 'EM_DIA',
      statusFluxo: 'PENDENTE',
      possuiArquivo: false,
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
    },
    {
      id: 8,
      colaboradorId: 4,
      colaboradorNome: 'Marcos Vinícius Rocha',
      tipoExameId: 6,
      tipoExameNome: 'Espirometria (Prova de Função Pulmonar)',
      periodicidadeMeses: 12,
      dataAgendada: null,
      localClinica: null,
      observacoes: null,
      dataRealizacao: null,
      dataVencimento: addDays(28),
      diasParaVencer: 28,
      statusValidade: 'PROXIMO_VENCIMENTO',
      statusFluxo: 'PENDENTE',
      possuiArquivo: false,
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
    },
    {
      id: 9,
      colaboradorId: 5,
      colaboradorNome: 'Camila Ribeiro Nascimento',
      tipoExameId: 1,
      tipoExameNome: 'Avaliação Clínica Ocupacional (ASO)',
      periodicidadeMeses: 12,
      dataAgendada: null,
      localClinica: null,
      observacoes: null,
      dataRealizacao: null,
      dataVencimento: addMonths(4),
      diasParaVencer: 120,
      statusValidade: 'EM_DIA',
      statusFluxo: 'PENDENTE',
      possuiArquivo: false,
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
    },
    {
      id: 10,
      colaboradorId: 6,
      colaboradorNome: 'André Luiz Gonçalves',
      tipoExameId: 3,
      tipoExameNome: 'Acuidade Visual',
      periodicidadeMeses: 12,
      dataAgendada: null,
      localClinica: null,
      observacoes: null,
      dataRealizacao: addDays(-5),
      dataVencimento: addDays(-12),
      diasParaVencer: -12,
      statusValidade: 'VENCIDO',
      statusFluxo: 'REVISADO_PENDENTE',
      possuiArquivo: true,
      medicoNome: 'Dra. Helena Prado (Médica do Trabalho)',
      crmAssinatura: '148920',
      crmUfAssinatura: 'SP',
      assinadoEm: isoDateTime(-3),
      justificativaRecusa: 'O documento anexado está sem o carimbo e assinatura do oftalmologista examinador.',
    },
    {
      id: 11,
      colaboradorId: 5,
      colaboradorNome: 'Camila Ribeiro Nascimento',
      tipoExameId: 2,
      tipoExameNome: 'Audiometria Tonal e Vocal',
      periodicidadeMeses: 12,
      dataAgendada: addMonths(-8),
      localClinica: 'Clínica MedSantos Ocupacional',
      observacoes: 'Audiometria admissional homologada.',
      dataRealizacao: addMonths(-8),
      dataVencimento: addMonths(4),
      diasParaVencer: 120,
      statusValidade: 'EM_DIA',
      statusFluxo: 'HOMOLOGADO',
      possuiArquivo: true,
      medicoNome: 'Dra. Helena Prado (Médica do Trabalho)',
      crmAssinatura: '148920',
      crmUfAssinatura: 'SP',
      assinadoEm: isoDateTime(-240),
      justificativaRecusa: null,
    },
    {
      id: 12,
      colaboradorId: 4,
      colaboradorNome: 'Marcos Vinícius Rocha',
      tipoExameId: 7,
      tipoExameNome: 'Raio-X de Tórax (Padrão OIT)',
      periodicidadeMeses: 24,
      dataAgendada: null,
      localClinica: null,
      observacoes: 'Exposição a fumos metálicos de solda.',
      dataRealizacao: null,
      dataVencimento: addDays(-3),
      diasParaVencer: -3,
      statusValidade: 'VENCIDO',
      statusFluxo: 'PENDENTE',
      possuiArquivo: false,
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
    },
    {
      id: 13,
      colaboradorId: 2,
      colaboradorNome: 'Beatriz Santos Oliveira',
      tipoExameId: 8,
      tipoExameNome: 'Hemograma Completo e Glicemia de Jejum',
      periodicidadeMeses: 12,
      dataAgendada: addDays(4),
      localClinica: 'Centro Médico Ocupacional Guarujá',
      observacoes: 'Jejum mínimo de 8 horas (NR-35).',
      dataRealizacao: null,
      dataVencimento: addDays(18),
      diasParaVencer: 18,
      statusValidade: 'PROXIMO_VENCIMENTO',
      statusFluxo: 'AGENDADO',
      possuiArquivo: false,
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
    },
  ],

  documentos: [
    {
      id: 1,
      tipoDocumentoId: 1,
      tipoDocumentoSigla: 'PGR',
      tipoDocumentoNome: 'Programa de Gerenciamento de Riscos',
      titulo: 'PGR - Terminal Margem Direita Santos 2025/2027',
      descricao: 'Inventário de Riscos Ocupacionais e Plano de Ação conforme NR-01 (GRO).',
      versao: 2,
      dataEmissao: addMonths(-6),
      dataValidade: addMonths(18),
      diasParaVencer: 540,
      statusValidade: 'EM_DIA',
      documentoBaseId: null,
      documentoBaseTitulo: null,
      arquivoNome: 'PGR-PortoLog-Santos-v2.pdf',
      enviadoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      criadoEm: isoDateTime(-180),
      vigente: true,
    },
    {
      id: 2,
      tipoDocumentoId: 2,
      tipoDocumentoSigla: 'PCMSO',
      tipoDocumentoNome: 'Programa de Controle Médico de Saúde Ocupacional',
      titulo: 'PCMSO - PortoLog Santos Vigência 2025/2026',
      descricao: 'Elaborado com base nos riscos ocupacionais inventariados no PGR v2 (NR-07).',
      versao: 1,
      dataEmissao: addMonths(-11),
      dataValidade: addDays(15),
      diasParaVencer: 15,
      statusValidade: 'PROXIMO_VENCIMENTO',
      documentoBaseId: 1,
      documentoBaseTitulo: 'PGR - Terminal Margem Direita Santos 2025/2027',
      arquivoNome: 'PCMSO-PortoLog-2026.pdf',
      enviadoPorNome: 'Fernanda Almeida (Gestora de RH)',
      criadoEm: isoDateTime(-330),
      vigente: true,
    },
    {
      id: 3,
      tipoDocumentoId: 3,
      tipoDocumentoSigla: 'LTCAT',
      tipoDocumentoNome: 'Laudo Técnico das Condições Ambientais do Trabalho',
      titulo: 'LTCAT - Operações Portuárias e Manutenção Industrial',
      descricao: 'Avaliação quantitativa de ruído, calor e agentes químicos para o eSocial (Evento S-2240).',
      versao: 1,
      dataEmissao: addMonths(-8),
      dataValidade: addMonths(16),
      diasParaVencer: 480,
      statusValidade: 'EM_DIA',
      documentoBaseId: 1,
      documentoBaseTitulo: 'PGR - Terminal Margem Direita Santos 2025/2027',
      arquivoNome: 'LTCAT-PortoLog-2026.pdf',
      enviadoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      criadoEm: isoDateTime(-240),
      vigente: true,
    },
    {
      id: 4,
      tipoDocumentoId: 4,
      tipoDocumentoSigla: 'AET',
      tipoDocumentoNome: 'Análise Ergonômica do Trabalho',
      titulo: 'AET - Postos Operacionais de Empilhadeira e Escritório',
      descricao: 'Análise biomecânica e mobiliário conforme NR-17.',
      versao: 1,
      dataEmissao: addMonths(-24),
      dataValidade: addDays(-9),
      diasParaVencer: -9,
      statusValidade: 'VENCIDO',
      documentoBaseId: 1,
      documentoBaseTitulo: 'PGR - Terminal Margem Direita Santos 2025/2027',
      arquivoNome: 'AET-Ergonomia-PortoLog.pdf',
      enviadoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      criadoEm: isoDateTime(-730),
      vigente: true,
    },
    {
      id: 5,
      tipoDocumentoId: 5,
      tipoDocumentoSigla: 'PCA',
      tipoDocumentoNome: 'Programa de Conservação Auditiva',
      titulo: 'PCA - Controle de Exposição ao Ruído no Pátio e Cais',
      descricao: 'Monitoramento audiométrico semestral e atenuação de protetores auriculares.',
      versao: 1,
      dataEmissao: addMonths(-4),
      dataValidade: addMonths(8),
      diasParaVencer: 240,
      statusValidade: 'EM_DIA',
      documentoBaseId: 2,
      documentoBaseTitulo: 'PCMSO - PortoLog Santos Vigência 2025/2026',
      arquivoNome: 'PCA-PortoLog-2026.pdf',
      enviadoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      criadoEm: isoDateTime(-120),
      vigente: true,
    },
  ],

  laudos: [
    {
      id: 1,
      tipoLaudoId: 1,
      tipoLaudoNome: 'LTCAT - Condições Ambientais do Trabalho',
      titulo: 'LTCAT - Dosimetria de Ruído Operações de Pátio (NR-15 Anexo 1)',
      descricao: 'Avaliação quantitativa por dosimetria de ruído nos operadores de empilhadeira no Terminal Margem Direita.',
      status: 'AGUARDANDO_ASSINATURA',
      dataEmissao: addDays(-2),
      dataValidade: addMonths(12),
      colaboradorId: 1,
      colaboradorNome: 'João Marcos da Silva',
      arquivoNome: 'LTCAT-Dosimetria-Patio-Santos.pdf',
      criadoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
      criadoEm: isoDateTime(-2),
    },
    {
      id: 2,
      tipoLaudoId: 3,
      tipoLaudoNome: 'Laudo Técnico de Periculosidade (NR-16)',
      titulo: 'Laudo de Periculosidade - Instalações Elétricas de Alta e Baixa Tensão',
      descricao: 'Caracterização de exposição ao risco elétrico no setor de Manutenção Industrial conforme NR-16 Anexo IV.',
      status: 'AGUARDANDO_ASSINATURA',
      dataEmissao: addDays(-1),
      dataValidade: addMonths(12),
      colaboradorId: 2,
      colaboradorNome: 'Beatriz Santos Oliveira',
      arquivoNome: 'Laudo-Periculosidade-Eletrica-NR16.pdf',
      criadoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      medicoNome: null,
      crmAssinatura: null,
      crmUfAssinatura: null,
      assinadoEm: null,
      justificativaRecusa: null,
      criadoEm: isoDateTime(-1),
    },
    {
      id: 3,
      tipoLaudoId: 2,
      tipoLaudoNome: 'Laudo Técnico de Insalubridade (NR-15)',
      titulo: 'Laudo de Insalubridade - Fumos Metálicos na Caldeiraria e Solda',
      descricao: 'Avaliação química quantitativa de fumos de solda (manganês, ferro e cromo) conforme NR-15 Anexo 11 e 12.',
      status: 'HOMOLOGADO',
      dataEmissao: addMonths(-2),
      dataValidade: addMonths(10),
      colaboradorId: 4,
      colaboradorNome: 'Marcos Vinícius Rocha',
      arquivoNome: 'Laudo-Insalubridade-Caldeiraria-NR15.pdf',
      criadoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      medicoNome: 'Dra. Helena Prado (Médica do Trabalho)',
      crmAssinatura: '148920',
      crmUfAssinatura: 'SP',
      assinadoEm: isoDateTime(-55),
      justificativaRecusa: null,
      criadoEm: isoDateTime(-60),
    },
    {
      id: 4,
      tipoLaudoId: 4,
      tipoLaudoNome: 'Parecer Técnico de Ergonomia (NR-17)',
      titulo: 'Parecer Ergonômico - Postos Administrativos e Conferência',
      descricao: 'Avaliação de iluminamento, conforto térmico e mobiliário (NR-17).',
      status: 'HOMOLOGADO',
      dataEmissao: addMonths(-3),
      dataValidade: addMonths(21),
      colaboradorId: 3,
      colaboradorNome: 'Rafael Pereira Souza',
      arquivoNome: 'Parecer-Ergonomico-NR17.pdf',
      criadoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      medicoNome: 'Dra. Helena Prado (Médica do Trabalho)',
      crmAssinatura: '148920',
      crmUfAssinatura: 'SP',
      assinadoEm: isoDateTime(-88),
      justificativaRecusa: null,
      criadoEm: isoDateTime(-90),
    },
    {
      id: 5,
      tipoLaudoId: 2,
      tipoLaudoNome: 'Laudo Técnico de Insalubridade (NR-15)',
      titulo: 'Laudo de Vibração de Corpo Inteiro (VCI) - Empilhadeiras GLP',
      descricao: 'Avaliação de exposição à vibração ocupacional (aren e VDVR) conforme NR-15 Anexo 8.',
      status: 'REVISADO_PENDENTE',
      dataEmissao: addDays(-10),
      dataValidade: addMonths(12),
      colaboradorId: 6,
      colaboradorNome: 'André Luiz Gonçalves',
      arquivoNome: 'Laudo-Vibracao-VCI-NR15.pdf',
      criadoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      medicoNome: 'Dra. Helena Prado (Médica do Trabalho)',
      crmAssinatura: '148920',
      crmUfAssinatura: 'SP',
      assinadoEm: isoDateTime(-7),
      justificativaRecusa: 'Incluir o certificado de calibração atualizado do acelerômetro triaxial no Anexo II do PDF antes da homologação.',
      criadoEm: isoDateTime(-10),
    },
  ],

  notificacoes: [
    {
      id: 1,
      tipo: 'ALERTA_VENCIMENTO_EXAME',
      titulo: 'URGENTE - Exame vencido: Audiometria Tonal e Vocal (João Marcos da Silva)',
      mensagem: 'O exame Audiometria Tonal e Vocal do colaborador João Marcos da Silva venceu há 6 dia(s). Providencie o agendamento imediato.',
      lida: false,
      criadoEm: isoDateTime(0, -1),
      lidaEm: null,
    },
    {
      id: 2,
      tipo: 'ALERTA_VENCIMENTO_DOCUMENTO',
      titulo: 'URGENTE - Documento SST vencido: AET - Postos Operacionais de Empilhadeira e Escritório',
      mensagem: 'O documento AET (v1) venceu há 9 dia(s). Realize o upload da nova versão atualizada.',
      lida: false,
      criadoEm: isoDateTime(0, -1),
      lidaEm: null,
    },
    {
      id: 3,
      tipo: 'ALERTA_VENCIMENTO_EXAME',
      titulo: 'Alerta (7 dias) - Exame próximo do vencimento: Eletrocardiograma (ECG)',
      mensagem: 'O exame Eletrocardiograma (ECG) da colaboradora Beatriz Santos Oliveira vencerá em 7 dias (agendado na Clínica Guarujá).',
      lida: false,
      criadoEm: isoDateTime(0, -2),
      lidaEm: null,
    },
    {
      id: 4,
      tipo: 'ALERTA_VENCIMENTO_DOCUMENTO',
      titulo: 'Alerta (15 dias) - Documento SST próximo do vencimento: PCMSO',
      mensagem: 'O documento PCMSO - PortoLog Santos Vigência 2025/2026 (v1) vencerá em 15 dias.',
      lida: false,
      criadoEm: isoDateTime(0, -2),
      lidaEm: null,
    },
    {
      id: 5,
      tipo: 'LAUDO_PENDENTE_ASSINATURA',
      titulo: 'Novo laudo aguardando assinatura: LTCAT - Dosimetria de Ruído Operações de Pátio',
      mensagem: 'O laudo técnico LTCAT foi emitido por Carlos Eduardo Mendes e aguarda validação e assinatura médica.',
      lida: false,
      criadoEm: isoDateTime(-1),
      lidaEm: null,
    },
    {
      id: 6,
      tipo: 'LAUDO_REVISADO_PENDENTE',
      titulo: 'Laudo em revisão pendente: Laudo de Vibração de Corpo Inteiro (VCI)',
      mensagem: 'Devolvido pela Dra. Helena Prado (CRM 148920/SP). Justificativa: Incluir o certificado de calibração do acelerômetro.',
      lida: true,
      criadoEm: isoDateTime(-5),
      lidaEm: isoDateTime(-4),
    },
    {
      id: 7,
      tipo: 'LAUDO_HOMOLOGADO',
      titulo: 'Laudo homologado: Laudo de Insalubridade - Fumos Metálicos na Caldeiraria',
      mensagem: 'Assinado digitalmente pela Dra. Helena Prado (CRM 148920/SP). Status atualizado para Homologado.',
      lida: true,
      criadoEm: isoDateTime(-10),
      lidaEm: isoDateTime(-9),
    },
  ],

  conversasIa: [
    {
      id: 1,
      titulo: 'Periodicidade dos exames periódicos no PCMSO segundo a NR-07',
      atualizadoEm: isoDateTime(-1, -2),
      mensagens: [
        {
          id: 1,
          papel: 'USUARIO',
          conteudo: 'Qual a periodicidade dos exames periódicos no PCMSO segundo a NR-07?',
          classificacao: null,
          fontes: [],
          criadoEm: isoDateTime(-1, -2),
        },
        {
          id: 2,
          papel: 'ASSISTENTE',
          conteudo:
            'Com base na base normativa de Saúde e Segurança do Trabalho, seguem as referências aplicáveis:\n\n' +
            '• **NR-07 (item 7.5.8) — Programa de Controle Médico de Saúde Ocupacional (PCMSO)**:\n' +
            'O exame clínico periódico deve ser realizado de acordo com os intervalos mínimos de tempo definidos no PCMSO:\n' +
            'a) Para empregados expostos a riscos ocupacionais identificados e classificados no PGR e para portadores de doenças crônicas que aumentem a susceptibilidade a tais riscos: **a cada ano (12 meses)** ou a intervalos menores (como **6 meses** para audiometria em situações específicas), a critério do médico responsável;\n' +
            'b) Para os demais empregados: o exame clínico deve ser realizado **a cada dois anos (24 meses)**.\n\n' +
            '_Nota: Resposta orientativa fundamentada nas Normas Regulamentadoras. Não substitui parecer técnico ou médico habilitado._',
          classificacao: 'RESPONDIDA',
          fontes: [
            { trechoId: 7, norma: 'NR-07', item: '7.5.8', titulo: 'Programa de Controle Médico de Saúde Ocupacional - Periodicidade' },
            { trechoId: 8, norma: 'NR-07', item: 'Anexo II', titulo: 'Controle Médico Ocupacional da Exposição a Níveis de Pressão Sonora Elevados' },
          ],
          criadoEm: isoDateTime(-1, -2),
        },
      ],
    },
    {
      id: 2,
      titulo: 'Integração entre os riscos do PGR (NR-01) e a elaboração do PCMSO',
      atualizadoEm: isoDateTime(-3, -5),
      mensagens: [
        {
          id: 3,
          papel: 'USUARIO',
          conteudo: 'Como o PGR da NR-01 se relaciona com a elaboração do PCMSO e a definição de exames por função e setor?',
          classificacao: null,
          fontes: [],
          criadoEm: isoDateTime(-3, -5),
        },
        {
          id: 4,
          papel: 'ASSISTENTE',
          conteudo:
            'Conforme a **NR-01 (item 1.5.3.1)** e a **NR-07 (item 7.5.1)**, o **PCMSO deve ser elaborado considerando os riscos ocupacionais identificados e classificados pelo PGR (Programa de Gerenciamento de Riscos)**.\n\n' +
            'Na prática operacional de SST:\n' +
            '1. O **Inventário de Riscos do PGR** mapeia os perigos e agentes (físicos, químicos, biológicos, ergonômicos e de acidentes) por combinação de **Função e Setor**;\n' +
            '2. A partir desse mapeamento, o Especialista SST e o Médico do Trabalho configuram a **Matriz de Periodicidade de Exames** (6 a 24 meses) para cada par Função + Setor;\n' +
            '3. Qualquer alteração nos riscos do PGR exige atualização correspondente do PCMSO.\n\n' +
            '_Nota: Resposta orientativa fundamentada nas Normas Regulamentadoras._',
          classificacao: 'RESPONDIDA',
          fontes: [
            { trechoId: 1, norma: 'NR-01', item: '1.5.3.1', titulo: 'Disposições Gerais e Gerenciamento de Riscos Ocupacionais (GRO / PGR)' },
            { trechoId: 6, norma: 'NR-07', item: '7.5.1', titulo: 'Diretrizes do PCMSO articuladas com o PGR' },
          ],
          criadoEm: isoDateTime(-3, -5),
        },
      ],
    },
  ],

  auditoria: [
    {
      id: 1,
      empresaId: 1,
      usuarioId: 3,
      usuarioEmail: 'medico@saluris.com.br',
      acao: 'ASSINAR_LAUDO',
      entidade: 'laudos',
      entidadeId: 3,
      detalhes: { crm: '148920/SP', titulo: 'Laudo de Insalubridade - Fumos Metálicos na Caldeiraria e Solda' },
      ipOrigem: '127.0.0.1',
      criadoEm: isoDateTime(-2, -3),
    },
    {
      id: 2,
      empresaId: 1,
      usuarioId: 3,
      usuarioEmail: 'medico@saluris.com.br',
      acao: 'RECUSAR_LAUDO',
      entidade: 'laudos',
      entidadeId: 5,
      detalhes: { justificativa: 'Incluir o certificado de calibração atualizado do acelerômetro triaxial no Anexo II.', titulo: 'Laudo de Vibração de Corpo Inteiro (VCI)' },
      ipOrigem: '127.0.0.1',
      criadoEm: isoDateTime(-3, -1),
    },
    {
      id: 3,
      empresaId: 1,
      usuarioId: 4,
      usuarioEmail: 'rh@saluris.com.br',
      acao: 'GERAR_RELATORIO',
      entidade: 'relatorios',
      entidadeId: null,
      detalhes: { formato: 'PDF', tipo: 'AMBOS', exames: 10, documentos: 5 },
      ipOrigem: '127.0.0.1',
      criadoEm: isoDateTime(-1, -4),
    },
    {
      id: 4,
      empresaId: 1,
      usuarioId: 1,
      usuarioEmail: 'admin@saluris.com.br',
      acao: 'CRIAR_USUARIO',
      entidade: 'usuarios',
      entidadeId: 4,
      detalhes: { email: 'rh@saluris.com.br', perfil: 'RH_EMPRESA' },
      ipOrigem: '127.0.0.1',
      criadoEm: isoDateTime(-10, -2),
    },
    {
      id: 5,
      empresaId: 2,
      usuarioId: 1,
      usuarioEmail: 'admin@saluris.com.br',
      acao: 'CRIAR_EMPRESA',
      entidade: 'empresas',
      entidadeId: 2,
      detalhes: { cnpj: '45723174000110', razaoSocial: 'Metalúrgica e Caldeiraria Atlântica Ltda.' },
      ipOrigem: '127.0.0.1',
      criadoEm: isoDateTime(-14, -5),
    },
  ],

  emails: [
    {
      id: 1,
      destinatario: 'rh@saluris.com.br',
      assunto: '[Saluris IA] URGENTE - Exame vencido: Audiometria Tonal e Vocal (João Marcos da Silva)',
      status: 'ENVIADO',
      tentativas: 1,
      ultimoErro: null,
      proximaTentativaEm: null,
      enviadoEm: isoDateTime(0, -1),
      criadoEm: isoDateTime(0, -1),
    },
    {
      id: 2,
      destinatario: 'especialista@saluris.com.br',
      assunto: '[Saluris IA] Alerta (15 dias) - Documento SST próximo do vencimento: PCMSO',
      status: 'ENVIADO',
      tentativas: 1,
      ultimoErro: null,
      proximaTentativaEm: null,
      enviadoEm: isoDateTime(0, -2),
      criadoEm: isoDateTime(0, -2),
    },
    {
      id: 3,
      destinatario: 'beatriz.oliveira@portolog.com.br',
      assunto: '[Saluris IA] Exame agendado: Eletrocardiograma (ECG) - Beatriz Santos Oliveira',
      status: 'ENVIADO',
      tentativas: 1,
      ultimoErro: null,
      proximaTentativaEm: null,
      enviadoEm: isoDateTime(-1, -3),
      criadoEm: isoDateTime(-1, -3),
    },
  ],
};

function paginate(items, params = {}) {
  const page = Number(params?.page || 0);
  const size = Number(params?.size || 20);
  const start = page * size;
  const content = items.slice(start, start + size);
  return {
    content,
    page,
    size,
    totalElements: items.length,
    totalPages: Math.max(1, Math.ceil(items.length / size)),
  };
}

function resolveItemEmpresaId(item, kind) {
  if (item?.empresaId) return Number(item.empresaId);
  if (kind === 'colaborador') {
    if (item.id === 4) return 2;
    if (item.id === 6) return 3;
    return 1;
  }
  if (kind === 'exame') {
    if (item.colaboradorId === 4) return 2;
    if (item.colaboradorId === 6) return 3;
    return 1;
  }
  if (kind === 'documento') {
    if (item.id === 3) return 2;
    if (item.id === 4) return 3;
    return 1;
  }
  if (kind === 'laudo') {
    if (item.id === 3) return 2;
    if (item.id === 5) return 3;
    return 1;
  }
  if (kind === 'unidade') {
    if (item.id === 3) return 2;
    if (item.id === 2) return 3;
    return 1;
  }
  return 1;
}

function filterByEmpresa(list, empresaId, kind) {
  if (!empresaId) return list;
  return list.filter((item) => String(resolveItemEmpresaId(item, kind)) === String(empresaId));
}

export function buildDemoDashboard(params = {}) {
  const temFiltroAtivo = Boolean(
    params?.tipoExameId ||
      params?.colaboradorId ||
      params?.statusValidade ||
      params?.dataInicio ||
      params?.dataFim
  );

  let exames = filterByEmpresa(
    DEMO_STATE.exames.filter((e) => e.statusFluxo !== 'HOMOLOGADO'),
    params?.empresaId,
    'exame'
  );
  if (params?.tipoExameId) {
    exames = exames.filter((e) => String(e.tipoExameId) === String(params.tipoExameId));
  }
  if (params?.colaboradorId) {
    exames = exames.filter((e) => String(e.colaboradorId) === String(params.colaboradorId));
  }
  if (params?.statusValidade) {
    exames = exames.filter((e) => e.statusValidade === params.statusValidade);
  }
  if (params?.dataInicio) {
    exames = exames.filter((e) => e.dataVencimento >= params.dataInicio);
  }
  if (params?.dataFim) {
    exames = exames.filter((e) => e.dataVencimento <= params.dataFim);
  }

  const emDia = exames.filter((e) => e.statusValidade === 'EM_DIA').length;
  const proximoVencimento = exames.filter((e) => e.statusValidade === 'PROXIMO_VENCIMENTO').length;
  const vencido = exames.filter((e) => e.statusValidade === 'VENCIDO').length;
  const total = exames.length;
  const percentualConformidade =
    total === 0
      ? temFiltroAtivo
        ? 0
        : 100
      : Math.round(((emDia + proximoVencimento) / total) * 1000) / 10;

  let docs = filterByEmpresa(
    DEMO_STATE.documentos.filter((d) => d.vigente),
    params?.empresaId,
    'documento'
  );
  if (params?.statusValidade) {
    docs = docs.filter((d) => d.statusValidade === params.statusValidade);
  }
  if (params?.dataInicio) {
    docs = docs.filter((d) => d.dataValidade >= params.dataInicio);
  }
  if (params?.dataFim) {
    docs = docs.filter((d) => d.dataValidade <= params.dataFim);
  }

  let laudosFiltrados = filterByEmpresa([...DEMO_STATE.laudos], params?.empresaId, 'laudo');
  if (params?.colaboradorId) {
    laudosFiltrados = laudosFiltrados.filter(
      (l) => String(l.colaboradorId) === String(params.colaboradorId)
    );
  }
  if (params?.dataInicio) {
    laudosFiltrados = laudosFiltrados.filter(
      (l) => (l.dataValidade || l.dataEmissao) >= params.dataInicio
    );
  }
  if (params?.dataFim) {
    laudosFiltrados = laudosFiltrados.filter(
      (l) => (l.dataValidade || l.dataEmissao) <= params.dataFim
    );
  }

  const aguardandoAssinatura = laudosFiltrados.filter(
    (l) => l.status === 'AGUARDANDO_ASSINATURA'
  ).length;
  const homologados = laudosFiltrados.filter((l) => l.status === 'HOMOLOGADO').length;
  const revisadoPendente = laudosFiltrados.filter(
    (l) => l.status === 'REVISADO_PENDENTE'
  ).length;

  return {
    exames: {
      total,
      emDia,
      proximoVencimento,
      vencido,
      percentualConformidade,
    },
    documentos: {
      total: docs.length,
      emDia: docs.filter((d) => d.statusValidade === 'EM_DIA').length,
      proximoVencimento: docs.filter((d) => d.statusValidade === 'PROXIMO_VENCIMENTO').length,
      vencido: docs.filter((d) => d.statusValidade === 'VENCIDO').length,
    },
    laudos: {
      aguardandoAssinatura,
      homologados,
      revisadoPendente,
    },
    documentosVigentes: docs,
    examesCriticos: exames.filter((e) => e.statusValidade !== 'EM_DIA'),
  };
}

function getCurrentDemoUser() {
  try {
    const raw = typeof localStorage !== 'undefined' ? localStorage.getItem('saluris_user') : null;
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function handleDemoRequest(method, path, body, params = {}) {
  const m = (method || 'GET').toUpperCase();
  const currentUser = getCurrentDemoUser();

  // Dashboard
  if (path === '/dashboard' && m === 'GET') {
    return buildDemoDashboard(params);
  }

  // Empresas
  if (path === '/empresas' && m === 'GET') {
    return [...DEMO_STATE.empresas];
  }
  if (path === '/empresas' && m === 'POST') {
    const novo = {
      id: Date.now(),
      ...body,
      adminEmail: body?.adminEmail?.trim() || '',
      adminNome: body?.adminNome?.trim() || 'Conta Principal da Empresa',
      adminSenhaTemporaria: 'Saluris@Emp2026',
      ativa: true,
    };
    DEMO_STATE.empresas.unshift(novo);
    if (novo.adminEmail) {
      DEMO_STATE.usuarios.unshift({
        id: Date.now() + 1,
        nome: novo.adminNome,
        email: novo.adminEmail,
        perfil: 'ADMIN_EMPRESA',
        empresaId: novo.id,
        empresaNome: novo.razaoSocial || novo.nomeFantasia,
        crm: null,
        crmUf: null,
        ativo: true,
        deveTrocarSenha: true,
        ultimoAcesso: null,
      });
    }
    return novo;
  }
  if (path.startsWith('/empresas/') && m === 'PUT') {
    const id = Number(path.split('/')[2]);
    const idx = DEMO_STATE.empresas.findIndex((e) => e.id === id);
    if (idx >= 0) {
      DEMO_STATE.empresas[idx] = { ...DEMO_STATE.empresas[idx], ...body };
      return DEMO_STATE.empresas[idx];
    }
  }
  if (path.match(/^\/empresas\/\d+\/ativa$/) && m === 'PATCH') {
    const id = Number(path.split('/')[2]);
    const emp = DEMO_STATE.empresas.find((e) => e.id === id);
    if (emp) {
      emp.ativa = !!body?.ativa;
      return { ...emp };
    }
  }

  // Usuários
  if (path === '/usuarios' && m === 'GET') {
    let list = [...DEMO_STATE.usuarios];
    if (currentUser?.perfil === 'ADMINISTRADOR') {
      list = list.filter((u) => u.perfil === 'ADMINISTRADOR');
    } else if (currentUser?.perfil === 'ADMIN_EMPRESA') {
      list = list.filter(
        (u) => String(u.empresaId) === String(currentUser.empresaId) && u.perfil !== 'ADMINISTRADOR'
      );
    } else if (params?.empresaId) {
      list = list.filter((u) => String(u.empresaId) === String(params.empresaId));
    }
    if (params?.perfil) {
      list = list.filter((u) => u.perfil === params.perfil);
    }
    return list;
  }
  if (path === '/usuarios' && m === 'POST') {
    const emp = DEMO_STATE.empresas.find((e) => String(e.id) === String(body?.empresaId));
    const usuario = {
      id: Date.now(),
      nome: body.nome,
      email: body.email,
      perfil: body.perfil,
      empresaId: body.empresaId ? Number(body.empresaId) : null,
      empresaNome: emp ? emp.razaoSocial : null,
      crm: body.crm || null,
      crmUf: body.crmUf || null,
      ativo: true,
      deveTrocarSenha: true,
      ultimoAcesso: null,
    };
    DEMO_STATE.usuarios.unshift(usuario);
    return { usuario, senhaTemporaria: 'Saluris@Temp9' };
  }
  if (path.match(/^\/usuarios\/\d+$/) && m === 'PUT') {
    const id = Number(path.split('/')[2]);
    const u = DEMO_STATE.usuarios.find((x) => x.id === id);
    if (u) {
      Object.assign(u, body);
      return { ...u };
    }
  }
  if (path.match(/^\/usuarios\/\d+\/ativo$/) && m === 'PATCH') {
    const id = Number(path.split('/')[2]);
    const u = DEMO_STATE.usuarios.find((x) => x.id === id);
    if (u) {
      u.ativo = !!body?.ativo;
      return { ...u };
    }
  }
  if (path.match(/^\/usuarios\/\d+\/redefinir-senha$/) && m === 'POST') {
    return { senhaTemporaria: 'Saluris@2026#' };
  }

  // Catálogos
  if (path === '/tipos-exame' && m === 'GET') {
    return [...DEMO_STATE.tiposExame];
  }
  if (path === '/tipos-exame' && m === 'POST') {
    const item = { id: Date.now(), ...body, ativo: true };
    DEMO_STATE.tiposExame.push(item);
    return item;
  }
  if (path === '/tipos-documento' && m === 'GET') {
    return [...DEMO_STATE.tiposDocumento];
  }
  if (path === '/tipos-documento' && m === 'POST') {
    const item = { id: Date.now(), ...body, ativo: true };
    DEMO_STATE.tiposDocumento.push(item);
    return item;
  }
  if (path === '/tipos-laudo' && m === 'GET') {
    return [...DEMO_STATE.tiposLaudo];
  }
  if (path === '/tipos-laudo' && m === 'POST') {
    const item = { id: Date.now(), ...body, ativo: true };
    DEMO_STATE.tiposLaudo.push(item);
    return item;
  }

  // Estrutura da empresa
  if (path === '/unidades' && m === 'GET') {
    return filterByEmpresa([...DEMO_STATE.unidades], params?.empresaId, 'unidade');
  }
  if (path === '/unidades' && m === 'POST') {
    const item = { id: Date.now(), ...body, ativa: true };
    DEMO_STATE.unidades.push(item);
    return item;
  }
  if (path === '/funcoes' && m === 'GET') {
    return [...DEMO_STATE.funcoes];
  }
  if (path === '/funcoes' && m === 'POST') {
    const item = { id: Date.now(), ...body, ativa: true };
    DEMO_STATE.funcoes.push(item);
    return item;
  }
  if (path === '/setores' && m === 'GET') {
    return [...DEMO_STATE.setores];
  }
  if (path === '/setores' && m === 'POST') {
    const item = { id: Date.now(), ...body, ativo: true };
    DEMO_STATE.setores.push(item);
    return item;
  }

  // Matriz de Periodicidade
  if (path === '/matriz-exames' && m === 'GET') {
    let list = [...DEMO_STATE.matrizExames];
    if (params?.funcaoId) list = list.filter((x) => String(x.funcaoId) === String(params.funcaoId));
    if (params?.setorId) list = list.filter((x) => String(x.setorId) === String(params.setorId));
    return list;
  }
  if (path === '/matriz-exames' && m === 'POST') {
    const f = DEMO_STATE.funcoes.find((x) => String(x.id) === String(body.funcaoId));
    const s = DEMO_STATE.setores.find((x) => String(x.id) === String(body.setorId));
    const t = DEMO_STATE.tiposExame.find((x) => String(x.id) === String(body.tipoExameId));
    const item = {
      id: Date.now(),
      funcaoId: Number(body.funcaoId),
      funcaoNome: f?.nome || 'Função',
      setorId: Number(body.setorId),
      setorNome: s?.nome || 'Setor',
      tipoExameId: Number(body.tipoExameId),
      tipoExameNome: t?.nome || 'Exame',
      periodicidadeMeses: Number(body.periodicidadeMeses),
      obrigatorio: body.obrigatorio !== false,
      configuradoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      atualizadoEm: new Date().toISOString(),
    };
    DEMO_STATE.matrizExames.push(item);
    const afetados = DEMO_STATE.colaboradores
      .filter((c) => String(c.funcaoId) === String(body.funcaoId) && String(c.setorId) === String(body.setorId))
      .map((c) => ({ id: c.id, nome: c.nome }));
    return { configuracao: item, colaboradoresAfetados: afetados };
  }
  if (path.match(/^\/matriz-exames\/\d+$/) && m === 'PUT') {
    const id = Number(path.split('/')[2]);
    const item = DEMO_STATE.matrizExames.find((x) => x.id === id);
    if (item) {
      item.periodicidadeMeses = Number(body.periodicidadeMeses);
      if (body.obrigatorio !== undefined) item.obrigatorio = body.obrigatorio;
      item.atualizadoEm = new Date().toISOString();
      const afetados = DEMO_STATE.colaboradores
        .filter((c) => c.funcaoId === item.funcaoId && c.setorId === item.setorId)
        .map((c) => ({ id: c.id, nome: c.nome }));
      return { configuracao: { ...item }, colaboradoresAfetados: afetados };
    }
  }
  if (path.match(/^\/matriz-exames\/\d+$/) && m === 'DELETE') {
    const id = Number(path.split('/')[2]);
    DEMO_STATE.matrizExames = DEMO_STATE.matrizExames.filter((x) => x.id !== id);
    return null;
  }

  // Colaboradores
  if (path === '/colaboradores' && m === 'GET') {
    let list = filterByEmpresa([...DEMO_STATE.colaboradores], params?.empresaId, 'colaborador');
    if (params?.busca) {
      const q = String(params.busca).toLowerCase();
      list = list.filter((c) => c.nome.toLowerCase().includes(q) || c.cpf.includes(q));
    }
    return paginate(list, params);
  }
  if (path.match(/^\/colaboradores\/\d+$/) && m === 'GET') {
    const id = Number(path.split('/')[2]);
    const colab = DEMO_STATE.colaboradores.find((c) => c.id === id) || DEMO_STATE.colaboradores[0];
    const exames = DEMO_STATE.exames.filter((e) => e.colaboradorId === colab.id);
    return { colaborador: colab, exames };
  }
  if (path === '/colaboradores' && m === 'POST') {
    const u = DEMO_STATE.unidades.find((x) => String(x.id) === String(body.unidadeId));
    const f = DEMO_STATE.funcoes.find((x) => String(x.id) === String(body.funcaoId));
    const s = DEMO_STATE.setores.find((x) => String(x.id) === String(body.setorId));
    const regras = DEMO_STATE.matrizExames.filter(
      (r) => String(r.funcaoId) === String(body.funcaoId) && String(r.setorId) === String(body.setorId)
    );
    const colab = {
      id: Date.now(),
      nome: body.nome,
      cpf: String(body.cpf || '').replace(/\D/g, ''),
      dataNascimento: body.dataNascimento,
      dataAdmissao: body.dataAdmissao,
      dataDemissao: null,
      email: body.email,
      ativo: true,
      unidadeId: Number(body.unidadeId),
      unidadeNome: u?.nome || 'Unidade',
      funcaoId: Number(body.funcaoId),
      funcaoNome: f?.nome || 'Função',
      setorId: Number(body.setorId),
      setorNome: s?.nome || 'Setor',
      statusConformidade: regras.length > 0 ? 'EM_DIA' : null,
    };
    DEMO_STATE.colaboradores.unshift(colab);
    const examesGerados = regras.map((r, i) => {
      const ex = {
        id: Date.now() + i + 1,
        colaboradorId: colab.id,
        colaboradorNome: colab.nome,
        tipoExameId: r.tipoExameId,
        tipoExameNome: r.tipoExameNome,
        periodicidadeMeses: r.periodicidadeMeses,
        dataAgendada: null,
        localClinica: null,
        observacoes: null,
        dataRealizacao: null,
        dataVencimento: addMonths(r.periodicidadeMeses),
        diasParaVencer: r.periodicidadeMeses * 30,
        statusValidade: 'EM_DIA',
        statusFluxo: 'PENDENTE',
        possuiArquivo: false,
      };
      DEMO_STATE.exames.unshift(ex);
      return ex;
    });
    return { colaborador: colab, examesGerados, periodicidadeConfigurada: regras.length > 0 };
  }

  // Exames
  if (path === '/exames' && m === 'GET') {
    let list = filterByEmpresa([...DEMO_STATE.exames], params?.empresaId, 'exame');
    if (!params?.incluirHistorico || String(params.incluirHistorico) === 'false') {
      if (!params?.statusFluxo) {
        list = list.filter((e) => e.statusFluxo !== 'HOMOLOGADO');
      }
    }
    if (params?.colaboradorId) list = list.filter((e) => String(e.colaboradorId) === String(params.colaboradorId));
    if (params?.tipoExameId) list = list.filter((e) => String(e.tipoExameId) === String(params.tipoExameId));
    if (params?.statusValidade) list = list.filter((e) => e.statusValidade === params.statusValidade);
    if (params?.statusFluxo) list = list.filter((e) => e.statusFluxo === params.statusFluxo);
    return paginate(list, params);
  }
  if (path.match(/^\/exames\/\d+\/agendar$/) && m === 'POST') {
    const id = Number(path.split('/')[2]);
    const ex = DEMO_STATE.exames.find((x) => x.id === id);
    if (ex) {
      ex.dataAgendada = body.dataAgendada;
      ex.localClinica = body.localClinica;
      ex.observacoes = body.observacoes;
      ex.statusFluxo = 'AGENDADO';
      return { ...ex };
    }
  }
  if (path.match(/^\/exames\/\d+\/resultado$/) && m === 'POST') {
    const id = Number(path.split('/')[2]);
    const ex = DEMO_STATE.exames.find((x) => x.id === id);
    if (ex) {
      ex.dataRealizacao = addDays(0);
      ex.statusFluxo = 'AGUARDANDO_ASSINATURA';
      ex.possuiArquivo = true;
      return { ...ex };
    }
  }

  // Documentos SST
  if (path === '/documentos' && m === 'GET') {
    let list = filterByEmpresa([...DEMO_STATE.documentos], params?.empresaId, 'documento');
    if (String(params?.somenteVigentes) === 'true') {
      list = list.filter((d) => d.vigente);
    }
    return list;
  }
  if (path === '/documentos' && m === 'POST') {
    const item = {
      id: Date.now(),
      tipoDocumentoId: 1,
      tipoDocumentoSigla: 'PGR',
      tipoDocumentoNome: 'Programa de Gerenciamento de Riscos',
      titulo: 'Novo Documento SST Cadastrado',
      descricao: 'Documento enviado em formato PDF.',
      versao: 1,
      dataEmissao: addDays(0),
      dataValidade: addMonths(12),
      diasParaVencer: 365,
      statusValidade: 'EM_DIA',
      documentoBaseId: null,
      documentoBaseTitulo: null,
      arquivoNome: 'documento-sst.pdf',
      enviadoPorNome: 'Especialista SST',
      criadoEm: new Date().toISOString(),
      vigente: true,
    };
    DEMO_STATE.documentos.unshift(item);
    return item;
  }

  // Laudos Técnicos
  if (path === '/laudos' && m === 'GET') {
    let list = filterByEmpresa([...DEMO_STATE.laudos], params?.empresaId, 'laudo');
    if (params?.status) {
      list = list.filter((l) => l.status === params.status);
    }
    return list;
  }
  if (path === '/laudos' && m === 'POST') {
    const item = {
      id: Date.now(),
      tipoLaudoId: 1,
      tipoLaudoNome: 'LTCAT - Condições Ambientais do Trabalho',
      titulo: 'Novo Laudo Técnico Emitido',
      descricao: 'Aguardando validação e assinatura médica.',
      status: 'AGUARDANDO_ASSINATURA',
      dataEmissao: addDays(0),
      dataValidade: addMonths(12),
      colaboradorId: 1,
      colaboradorNome: 'João Marcos da Silva',
      arquivoNome: 'laudo-tecnico.pdf',
      criadoPorNome: 'Carlos Eduardo Mendes (Especialista SST)',
      criadoEm: new Date().toISOString(),
    };
    DEMO_STATE.laudos.unshift(item);
    return item;
  }
  if (path.match(/^\/laudos\/\d+\/reenviar$/) && m === 'POST') {
    const id = Number(path.split('/')[2]);
    const l = DEMO_STATE.laudos.find((x) => x.id === id);
    if (l) {
      l.status = 'AGUARDANDO_ASSINATURA';
      l.justificativaRecusa = null;
      return { ...l };
    }
  }

  // Assinaturas Médicas (Quadro 11)
  if (path === '/assinaturas/pendentes' && m === 'GET') {
    return {
      laudos: DEMO_STATE.laudos.filter((l) => l.status === 'AGUARDANDO_ASSINATURA'),
      exames: DEMO_STATE.exames.filter((e) => e.statusFluxo === 'AGUARDANDO_ASSINATURA'),
    };
  }
  if (path.match(/^\/assinaturas\/laudos\/\d+\/assinar$/) && m === 'POST') {
    const id = Number(path.split('/')[3]);
    const l = DEMO_STATE.laudos.find((x) => x.id === id);
    if (l) {
      l.status = 'HOMOLOGADO';
      l.medicoNome = 'Dra. Helena Prado (Médica do Trabalho)';
      l.crmAssinatura = '148920';
      l.crmUfAssinatura = 'SP';
      l.assinadoEm = new Date().toISOString();
      return { ...l };
    }
  }
  if (path.match(/^\/assinaturas\/laudos\/\d+\/recusar$/) && m === 'POST') {
    const id = Number(path.split('/')[3]);
    const l = DEMO_STATE.laudos.find((x) => x.id === id);
    if (l) {
      l.status = 'REVISADO_PENDENTE';
      l.medicoNome = 'Dra. Helena Prado (Médica do Trabalho)';
      l.justificativaRecusa = body?.justificativa || 'Ajuste técnico solicitado.';
      return { ...l };
    }
  }
  if (path.match(/^\/assinaturas\/exames\/\d+\/assinar$/) && m === 'POST') {
    const id = Number(path.split('/')[3]);
    const ex = DEMO_STATE.exames.find((x) => x.id === id);
    if (ex) {
      ex.statusFluxo = 'HOMOLOGADO';
      ex.medicoNome = 'Dra. Helena Prado (Médica do Trabalho)';
      ex.crmAssinatura = '148920';
      ex.crmUfAssinatura = 'SP';
      ex.assinadoEm = new Date().toISOString();
      return { ...ex };
    }
  }
  if (path.match(/^\/assinaturas\/exames\/\d+\/recusar$/) && m === 'POST') {
    const id = Number(path.split('/')[3]);
    const ex = DEMO_STATE.exames.find((x) => x.id === id);
    if (ex) {
      ex.statusFluxo = 'REVISADO_PENDENTE';
      ex.medicoNome = 'Dra. Helena Prado (Médica do Trabalho)';
      ex.justificativaRecusa = body?.justificativa || 'Documento ilegível ou incompleto.';
      return { ...ex };
    }
  }

  // Agente IA (Quadro 15)
  if (path === '/ia/status' && m === 'GET') {
    return { modo: 'OFFLINE', modelo: 'RAG-NR-Local (NR-01 a NR-35)' };
  }
  if (path === '/ia/conversas' && m === 'GET') {
    return DEMO_STATE.conversasIa.map(({ id, titulo, atualizadoEm }) => ({ id, titulo, atualizadoEm }));
  }
  if (path.match(/^\/ia\/conversas\/\d+\/mensagens$/) && m === 'GET') {
    const id = Number(path.split('/')[3]);
    const conv = DEMO_STATE.conversasIa.find((c) => c.id === id);
    return conv ? [...conv.mensagens] : [];
  }
  if (path.match(/^\/ia\/conversas\/\d+$/) && m === 'DELETE') {
    const id = Number(path.split('/')[3]);
    DEMO_STATE.conversasIa = DEMO_STATE.conversasIa.filter((c) => c.id !== id);
    return null;
  }
  if (path === '/ia/perguntar' && m === 'POST') {
    const texto = String(body?.pergunta || '').trim();
    const lower = texto.toLowerCase();
    let classificacao = 'RESPONDIDA';
    let respostaTexto =
      'Com base nas Normas Regulamentadoras do MTE (NR-01, NR-07 e NR-09), o gerenciamento de SST exige a identificação contínua dos perigos no **PGR** e o monitoramento médico correspondente no **PCMSO**, respeitando a periodicidade de **6 a 24 meses** configurada por combinação de **Função e Setor**.\n\n_Nota: Resposta orientativa fundamentada nas NRs._';
    let fontes = [
      { trechoId: 1, norma: 'NR-01', item: '1.5.3.1', titulo: 'Gerenciamento de Riscos Ocupacionais (GRO / PGR)' },
      { trechoId: 7, norma: 'NR-07', item: '7.5.8', titulo: 'Programa de Controle Médico de Saúde Ocupacional (PCMSO)' },
    ];

    if (/(emit|ger|assin|fa[cç]a|redij|elabor).*(laudo|aso|atestado|diagn[oó]stico|parecer)/i.test(lower)) {
      classificacao = 'RECUSADA_LAUDO_DIAGNOSTICO';
      respostaTexto =
        'Não posso emitir diagnósticos clínicos, pareceres médicos, Atestados de Saúde Ocupacional (ASO) ou redigir/assinar laudos técnicos. Conforme as Normas Regulamentadoras (NR-01, NR-07, NR-15 e NR-16), tais atos são privativos de Médico do Trabalho (CRM) ou Engenheiro de Segurança habilitado.';
      fontes = [];
    } else if (!/(nr|sst|pgr|pcmso|ltcat|aso|exame|epi|cipa|risco|trabalho|altura|ruido|insalubr|periculos|ergonom|laudo|medico|setor|funcao)/i.test(lower)) {
      classificacao = 'FORA_DO_ESCOPO';
      respostaTexto =
        'Atuo exclusivamente como assistente técnico de Saúde e Segurança do Trabalho (SST) e Normas Regulamentadoras (NRs). Por favor, envie uma dúvida relacionada a NRs, PGR, PCMSO, LTCAT, exames ocupacionais ou gestão de SST.';
      fontes = [];
    }

    let conv = DEMO_STATE.conversasIa.find((c) => c.id === Number(body?.conversaId));
    if (!conv) {
      conv = {
        id: Date.now(),
        titulo: texto.slice(0, 75),
        atualizadoEm: new Date().toISOString(),
        mensagens: [],
      };
      DEMO_STATE.conversasIa.unshift(conv);
    }
    const msgPergunta = {
      id: Date.now() + 1,
      papel: 'USUARIO',
      conteudo: texto,
      classificacao: null,
      fontes: [],
      criadoEm: new Date().toISOString(),
    };
    const msgResposta = {
      id: Date.now() + 2,
      papel: 'ASSISTENTE',
      conteudo: respostaTexto,
      classificacao,
      fontes,
      criadoEm: new Date().toISOString(),
    };
    conv.mensagens.push(msgPergunta, msgResposta);
    conv.atualizadoEm = new Date().toISOString();
    return { conversaId: conv.id, pergunta: msgPergunta, resposta: msgResposta };
  }

  // Notificações
  if (path === '/notificacoes/nao-lidas/contagem' && m === 'GET') {
    if (currentUser?.perfil === 'ADMINISTRADOR' || currentUser?.perfil === 'ADMIN_EMPRESA') {
      return { total: 0 };
    }
    return { total: DEMO_STATE.notificacoes.filter((n) => !n.lida).length };
  }
  if (path === '/notificacoes' && m === 'GET') {
    if (currentUser?.perfil === 'ADMINISTRADOR' || currentUser?.perfil === 'ADMIN_EMPRESA') {
      return paginate([], params);
    }
    let list = [...DEMO_STATE.notificacoes];
    if (String(params?.somenteNaoLidas) === 'true') {
      list = list.filter((n) => !n.lida);
    }
    return paginate(list, params);
  }
  if (path.match(/^\/notificacoes\/\d+\/lida$/) && m === 'POST') {
    const id = Number(path.split('/')[2]);
    const n = DEMO_STATE.notificacoes.find((x) => x.id === id);
    if (n) {
      n.lida = true;
      n.lidaEm = new Date().toISOString();
    }
    return { ...n };
  }
  if (path === '/notificacoes/marcar-todas-lidas' && m === 'POST') {
    let atualizadas = 0;
    DEMO_STATE.notificacoes.forEach((n) => {
      if (!n.lida) {
        n.lida = true;
        n.lidaEm = new Date().toISOString();
        atualizadas++;
      }
    });
    return { atualizadas };
  }

  // Administração / Auditoria / Monitoramento
  if (path === '/admin/auditoria' && m === 'GET') {
    let list = [...DEMO_STATE.auditoria];
    if (params?.empresaId) {
      list = list.filter((a) => String(a.empresaId) === String(params.empresaId));
    }
    return paginate(list, params);
  }
  if (path === '/admin/emails' && m === 'GET') {
    return paginate([...DEMO_STATE.emails], params);
  }
  if (path === '/admin/monitoramento/executar' && m === 'POST') {
    return {
      examesAtualizados: 2,
      documentosAtualizados: 1,
      notificacoesCriadas: 4,
      emailsEnfileirados: 4,
    };
  }

  return null;
}

