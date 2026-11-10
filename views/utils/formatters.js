// Formatação de datas para pt-BR (dd/mm/aaaa)
export function formatDate(dateStr) {
  if (!dateStr) return '—';
  // Se vier YYYY-MM-DD puro, evitar problema de fuso horário UTC
  if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
    const [y, m, d] = dateStr.split('-');
    return `${d}/${m}/${y}`;
  }
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  return d.toLocaleDateString('pt-BR');
}

export function formatDateTime(dateTimeStr) {
  if (!dateTimeStr) return '—';
  const d = new Date(dateTimeStr);
  if (isNaN(d.getTime())) return dateTimeStr;
  return d.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// Máscara e limpeza de CPF
export function onlyDigits(value = '') {
  return String(value).replace(/\D/g, '');
}

export function formatCPF(value = '') {
  const digits = onlyDigits(value).slice(0, 11);
  if (!digits) return '';
  if (digits.length <= 3) return digits;
  if (digits.length <= 6) return `${digits.slice(0, 3)}.${digits.slice(3)}`;
  if (digits.length <= 9) return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6)}`;
  return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9, 11)}`;
}

// Validação de CPF com dígitos verificadores
export function isValidCPF(cpf = '') {
  const digits = onlyDigits(cpf);
  if (digits.length !== 11) return false;
  if (/^(\d)\1{10}$/.test(digits)) return false;

  let sum = 0;
  for (let i = 0; i < 9; i++) {
    sum += parseInt(digits.charAt(i), 10) * (10 - i);
  }
  let rev = 11 - (sum % 11);
  if (rev === 10 || rev === 11) rev = 0;
  if (rev !== parseInt(digits.charAt(9), 10)) return false;

  sum = 0;
  for (let i = 0; i < 10; i++) {
    sum += parseInt(digits.charAt(i), 10) * (11 - i);
  }
  rev = 11 - (sum % 11);
  if (rev === 10 || rev === 11) rev = 0;
  if (rev !== parseInt(digits.charAt(10), 10)) return false;

  return true;
}

// Máscara de CNPJ
export function formatCNPJ(value = '') {
  const digits = onlyDigits(value).slice(0, 14);
  if (!digits) return '';
  if (digits.length <= 2) return digits;
  if (digits.length <= 5) return `${digits.slice(0, 2)}.${digits.slice(2)}`;
  if (digits.length <= 8) return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5)}`;
  if (digits.length <= 12) return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8)}`;
  return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8, 12)}-${digits.slice(12, 14)}`;
}

export function isValidCNPJ(cnpj = '') {
  const digits = onlyDigits(cnpj);
  if (digits.length !== 14) return false;
  if (/^(\d)\1{13}$/.test(digits)) return false;
  return true;
}

export function formatPhone(value = '') {
  const digits = onlyDigits(value).slice(0, 11);
  if (!digits) return '';
  if (digits.length <= 2) return `(${digits}`;
  if (digits.length <= 6) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
  if (digits.length <= 10) return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`;
  return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7, 11)}`;
}

// Validação de arquivo PDF no cliente (extensão .pdf, mime application/pdf e <= 20MB)
export function validatePdfFile(file) {
  if (!file) {
    return 'Selecione um arquivo PDF.';
  }
  const isPdfName = file.name && file.name.toLowerCase().endsWith('.pdf');
  const isPdfType = file.type === 'application/pdf' || file.type === '';
  if (!isPdfName || !isPdfType) {
    return 'O arquivo deve estar no formato PDF (.pdf).';
  }
  const maxBytes = 20 * 1024 * 1024; // 20 MB
  if (file.size > maxBytes) {
    return 'O arquivo excede o tamanho máximo permitido de 20 MB.';
  }
  return null;
}

export const PERFIL_LABELS = {
  ADMINISTRADOR: 'Administrador (T.I.)',
  ESPECIALISTA_SST: 'Especialista SST',
  MEDICO_TRABALHO: 'Médico do Trabalho',
  RH_EMPRESA: 'RH da Empresa',
};

export const PORTE_LABELS = {
  MEI: 'MEI',
  ME: 'Microempresa (ME)',
  EPP: 'Empresa de Pequeno Porte (EPP)',
  MEDIO: 'Médio Porte',
  GRANDE: 'Grande Porte',
};

export const STATUS_VALIDADE_LABELS = {
  EM_DIA: 'Em dia',
  PROXIMO_VENCIMENTO: 'Próximo do Vencimento',
  VENCIDO: 'Vencido',
};

export const STATUS_FLUXO_EXAME_LABELS = {
  PENDENTE: 'Pendente',
  AGENDADO: 'Agendado',
  REALIZADO: 'Realizado',
  AGUARDANDO_ASSINATURA: 'Aguardando Assinatura',
  HOMOLOGADO: 'Homologado',
  REVISADO_PENDENTE: 'Revisado Pendente',
};

export const STATUS_LAUDO_LABELS = {
  AGUARDANDO_ASSINATURA: 'Aguardando Assinatura',
  HOMOLOGADO: 'Homologado',
  REVISADO_PENDENTE: 'Revisado Pendente',
};

export const CLASSIFICACAO_IA_LABELS = {
  RESPONDIDA: 'Respondida com base normativa',
  FORA_DO_ESCOPO: 'Fora do escopo de SST',
  BASE_INSUFICIENTE: 'Base normativa insuficiente',
  RECUSADA_LAUDO_DIAGNOSTICO: 'Recusa de laudo / diagnóstico médico',
};

export const UFS_BRASIL = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO',
  'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI',
  'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
];

export function getHomeRouteForPerfil(perfil) {
  switch (perfil) {
    case 'ADMINISTRADOR':
      return '/usuarios';
    case 'ESPECIALISTA_SST':
    case 'RH_EMPRESA':
      return '/dashboard';
    case 'MEDICO_TRABALHO':
      return '/assinaturas';
    default:
      return '/login';
  }
}
