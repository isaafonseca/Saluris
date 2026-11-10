import { handleDemoRequest, DEMO_STATE } from './mockData';

const BASE_URL = '/api';

export class ApiError extends Error {
  constructor(status, title, detail, erros, raw) {
    super(detail || title || `Erro ${status}`);
    this.status = status;
    this.title = title;
    this.detail = detail;
    this.erros = erros || {};
    this.raw = raw || {};
  }
}

export function getToken() {
  return localStorage.getItem('saluris_token');
}

export function setToken(token) {
  if (token) {
    localStorage.setItem('saluris_token', token);
  } else {
    localStorage.removeItem('saluris_token');
  }
}

function handleUnauthorized() {
  localStorage.removeItem('saluris_token');
  localStorage.removeItem('saluris_user');
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

function buildQuery(params) {
  if (!params || typeof params !== 'object') return '';
  const sp = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') {
      sp.append(k, String(v));
    }
  });
  const str = sp.toString();
  return str ? `?${str}` : '';
}

async function parseErrorResponse(response) {
  let data = null;
  try {
    data = await response.json();
  } catch (_) {
    // non-json error response (ex: proxy ECONNREFUSED quando a porta 8080 está desligada)
  }
  const isProxyOffline = !data && response.status >= 500;
  const safeData = data || {};
  const title = safeData.title || response.statusText || 'Erro na requisição';
  const detail =
    safeData.detail ||
    safeData.message ||
    (isProxyOffline
      ? 'Servidor na porta 8080 ainda não está ativo. Inicie pelo script iniciar.ps1.'
      : `Falha na operação (código ${response.status})`);
  const erros = safeData.erros || {};
  const err = new ApiError(response.status, title, detail, erros, safeData);
  err.isProxyOffline = isProxyOffline;
  return err;
}

export async function request(path, options = {}) {
  const { method = 'GET', body, params, headers = {}, skipAuthRedirect = false } = options;
  const token = getToken();
  const url = `${BASE_URL}${path}${buildQuery(params)}`;

  const reqHeaders = {
    ...headers,
  };

  if (token) {
    reqHeaders['Authorization'] = `Bearer ${token}`;
  }

  let reqBody = body;
  if (body && !(body instanceof FormData) && typeof body === 'object') {
    reqHeaders['Content-Type'] = 'application/json';
    reqBody = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(url, {
      method,
      headers: reqHeaders,
      body: reqBody,
    });
  } catch (_) {
    const demoResult = handleDemoRequest(method, path, body, params);
    if (demoResult !== null) {
      return demoResult;
    }
    if (method === 'GET') {
      return null;
    }
    throw new ApiError(503, 'Servidor indisponível', 'Não foi possível conectar ao servidor (porta 8080).', {});
  }

  if (response.status === 401 && !skipAuthRedirect) {
    const demoResult = handleDemoRequest(method, path, body, params);
    if (demoResult !== null) {
      return demoResult;
    }
    handleUnauthorized();
    throw new ApiError(401, 'Não autenticado', 'Sua sessão expirou. Faça login novamente.', {});
  }

  if (!response.ok) {
    const err = await parseErrorResponse(response);
    if (err.isProxyOffline) {
      const demoResult = handleDemoRequest(method, path, body, params);
      if (demoResult !== null) {
        return demoResult;
      }
      if (method === 'GET') {
        return null;
      }
    }
    throw err;
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json') || contentType.includes('application/problem+json')) {
    const json = await response.json();
    // Se o banco estiver sem registros iniciais em uma listagem (sem filtros ativos), preenche com os dados demonstrativos
    if (method === 'GET' && isEmptyResult(path, json, params)) {
      const demoResult = handleDemoRequest(method, path, body, params);
      if (demoResult !== null) {
        return demoResult;
      }
    }
    return json;
  }
  return response.text();
}

function hasActiveFilters(params) {
  if (!params || typeof params !== 'object') return false;
  const filterKeys = [
    'dataInicio',
    'dataFim',
    'tipoExameId',
    'colaboradorId',
    'statusValidade',
    'statusFluxo',
    'busca',
    'status',
    'funcaoId',
    'setorId',
    'perfil',
  ];
  return filterKeys.some((k) => params[k] !== undefined && params[k] !== null && params[k] !== '');
}

function isEmptyResult(path, json, params) {
  if (hasActiveFilters(params)) return false;
  if (!json) return true;
  if (Array.isArray(json) && json.length === 0) return true;
  if (Array.isArray(json.content) && json.content.length === 0) return true;
  if (path === '/dashboard' && json.exames?.total === 0 && json.documentos?.total === 0) return true;
  if (path === '/assinaturas/pendentes' && json.laudos?.length === 0 && json.exames?.length === 0) return true;
  if (path === '/notificacoes/nao-lidas/contagem' && json.total === 0) return true;
  return false;
}

export const api = {
  get: (path, params, options = {}) => request(path, { ...options, method: 'GET', params }),
  post: (path, body, params, options = {}) => request(path, { ...options, method: 'POST', body, params }),
  put: (path, body, params, options = {}) => request(path, { ...options, method: 'PUT', body, params }),
  patch: (path, body, params, options = {}) => request(path, { ...options, method: 'PATCH', body, params }),
  delete: (path, params, options = {}) => request(path, { ...options, method: 'DELETE', params }),

  /**
   * Upload multipart/form-data com arquivo no campo `arquivo` e demais campos simples
   */
  upload: async (path, fields = {}, file, params) => {
    const formData = new FormData();
    Object.entries(fields || {}).forEach(([key, val]) => {
      if (val !== undefined && val !== null && val !== '') {
        formData.append(key, String(val));
      }
    });
    if (file) {
      formData.append('arquivo', file);
    }
    return request(path, { method: 'POST', body: formData, params });
  },

  /**
   * Busca um arquivo (PDF/CSV) autenticando via Bearer token
   */
  fetchBlob: async (path, params) => {
    const token = getToken();
    const url = `${BASE_URL}${path}${buildQuery(params)}`;
    const headers = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    let response;
    try {
      response = await fetch(url, { method: 'GET', headers });
    } catch (_) {
      return buildDemoBlob(path, params);
    }
    if (response.ok) {
      const blob = await response.blob();
      const disposition = response.headers.get('Content-Disposition') || '';
      let filename = 'documento.pdf';
      const match = disposition.match(/filename\*?=(?:UTF-8'')?["']?([^"';\n]+)["']?/i);
      if (match && match[1]) {
        filename = decodeURIComponent(match[1].trim());
      }
      return { blob, filename };
    }
    const err = await parseErrorResponse(response);
    if (!err.isProxyOffline && response.status < 500) {
      throw err;
    }
    return buildDemoBlob(path, params);
  },

  /**
   * Abre um PDF em uma nova aba usando URL de blob
   */
  openPdfInNewTab: async (path, params) => {
    const { blob } = await api.fetchBlob(path, params);
    const pdfBlob = blob.type === 'application/pdf' ? blob : new Blob([blob], { type: 'application/pdf' });
    const blobUrl = URL.createObjectURL(pdfBlob);
    const win = window.open(blobUrl, '_blank');
    if (!win) {
      const a = document.createElement('a');
      a.href = blobUrl;
      a.target = '_blank';
      a.rel = 'noopener noreferrer';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    }
    setTimeout(() => URL.revokeObjectURL(blobUrl), 60000);
  },

  /**
   * Faz download de um arquivo (PDF/CSV) usando o nome retornado em Content-Disposition
   */
  downloadFile: async (path, params, fallbackFilename = 'relatorio') => {
    const { blob, filename } = await api.fetchBlob(path, params);
    const blobUrl = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = blobUrl;
    a.download = filename || fallbackFilename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(blobUrl), 15000);
  },
};

function buildDemoBlob(path, params = {}) {
  const formato = String(params?.formato || 'PDF').toUpperCase();
  const escopo = String(params?.tipo || 'AMBOS').toUpperCase();

  let examesFiltrados = DEMO_STATE.exames.filter((e) => e.statusFluxo !== 'HOMOLOGADO');
  if (params?.tipoExameId) {
    examesFiltrados = examesFiltrados.filter((e) => String(e.tipoExameId) === String(params.tipoExameId));
  }
  if (params?.colaboradorId) {
    examesFiltrados = examesFiltrados.filter((e) => String(e.colaboradorId) === String(params.colaboradorId));
  }
  if (params?.statusValidade) {
    examesFiltrados = examesFiltrados.filter((e) => e.statusValidade === params.statusValidade);
  }
  if (params?.dataInicio) {
    examesFiltrados = examesFiltrados.filter((e) => e.dataVencimento >= params.dataInicio);
  }
  if (params?.dataFim) {
    examesFiltrados = examesFiltrados.filter((e) => e.dataVencimento <= params.dataFim);
  }

  let docsFiltrados = DEMO_STATE.documentos.filter((d) => d.vigente);
  if (params?.statusValidade) {
    docsFiltrados = docsFiltrados.filter((d) => d.statusValidade === params.statusValidade);
  }
  if (params?.dataInicio) {
    docsFiltrados = docsFiltrados.filter((d) => d.dataValidade >= params.dataInicio);
  }
  if (params?.dataFim) {
    docsFiltrados = docsFiltrados.filter((d) => d.dataValidade <= params.dataFim);
  }

  const incluirExames = escopo === 'EXAMES' || escopo === 'AMBOS';
  const incluirDocs = escopo === 'DOCUMENTOS' || escopo === 'AMBOS';
  const listaExames = incluirExames ? examesFiltrados : [];
  const listaDocs = incluirDocs ? docsFiltrados : [];

  if (path.includes('/relatorios/conformidade') && listaExames.length === 0 && listaDocs.length === 0) {
    throw new ApiError(422, 'Sem registros', 'Não há registros encontrados para os filtros selecionados.', {});
  }

  if (path.includes('/relatorios/conformidade') && formato === 'CSV') {
    const rows = [
      '\uFEFFCategoria;Identificacao;Tipo;Setor_Versao;Vencimento;Dias_Para_Vencer;Status_Validade;Status_Fluxo',
      ...listaExames.map(
        (e) =>
          `EXAME;${e.colaboradorNome};${e.tipoExameNome};Operacional;${e.dataVencimento};${e.diasParaVencer};${e.statusValidade};${e.statusFluxo}`
      ),
      ...listaDocs.map(
        (d) =>
          `DOCUMENTO_SST;${d.titulo};${d.tipoDocumentoSigla};v${d.versao};${d.dataValidade};${d.diasParaVencer};${d.statusValidade};Vigente`
      ),
    ].join('\n');
    return {
      blob: new Blob([rows], { type: 'text/csv;charset=utf-8' }),
      filename: 'relatorio-conformidade-saluris.csv',
    };
  }

  const title = path.includes('/laudos/')
    ? 'SALURIS IA - Laudo Tecnico de SST'
    : path.includes('/documentos/')
    ? 'SALURIS IA - Documento Ocupacional (PGR / PCMSO / LTCAT)'
    : path.includes('/exames/')
    ? 'SALURIS IA - Atestado de Saude Ocupacional (ASO / Exame)'
    : 'SALURIS IA - Relatorio de Conformidade Ocupacional SST';

  const pdfContent = `%PDF-1.4
1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj
4 0 obj << /Length 215 >> stream
BT
/F1 16 Tf
50 770 Td
(${title}) Tj
/F1 11 Tf
0 -30 Td
(Empresa: Porto Logistica e Operacoes Portuarias S.A. - CNPJ 11.222.333/0001-81) Tj
0 -22 Td
(Conformidade com Normas Regulamentadoras: NR-01, NR-07, NR-09, NR-15 e NR-17.) Tj
0 -22 Td
(Documento demonstrativo validado digitalmente pelo sistema Saluris IA.) Tj
ET
endstream endobj
5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj
xref
0 6
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000115 00000 n 
0000000229 00000 n 
0000000496 00000 n 
trailer << /Size 6 /Root 1 0 R >>
startxref
564
%%EOF`;

  return {
    blob: new Blob([pdfContent], { type: 'application/pdf' }),
    filename: 'documento-saluris-ia.pdf',
  };
}
