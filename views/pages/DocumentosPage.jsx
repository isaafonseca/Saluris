import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Upload,
  FileText,
  Filter,
  AlertCircle,
  Link2,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useEmpresa } from '../context/EmpresaContext';
import { useToast } from '../context/ToastContext';
import { formatDate, formatDateTime, validatePdfFile } from '../utils/formatters';
import { AdminEmpresaSelector } from '../components/ui/AdminEmpresaSelector';
import { Table } from '../components/ui/Table';
import { Modal } from '../components/ui/Modal';
import { Badge, ValidadeBadge } from '../components/ui/Badge';
import { FormField, EmptyState } from '../components/ui/Common';

export function DocumentosPage() {
  const { isAdmin, isEspecialista, isRh } = useAuth();
  const { activeEmpresaId } = useEmpresa();
  const toast = useToast();

  const canUpload = isEspecialista || isRh;

  const [documentos, setDocumentos] = useState([]);
  const [tiposDoc, setTiposDoc] = useState([]);
  const [somenteVigentes, setSomenteVigentes] = useState(false);
  const [loading, setLoading] = useState(false);

  // Modal Upload
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState({
    tipoDocumentoId: '',
    titulo: '',
    descricao: '',
    dataEmissao: '',
    dataValidade: '',
    documentoBaseId: '',
    arquivo: null,
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMsg, setErrorMsg] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api
      .get('/tipos-documento', { ativos: true })
      .then((res) => setTiposDoc(Array.isArray(res) ? res : []))
      .catch(() => {});
  }, []);

  const loadDocumentos = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.get('/documentos', {
        empresaId: isAdmin && activeEmpresaId ? activeEmpresaId : undefined,
        somenteVigentes: somenteVigentes ? true : undefined,
      });
      setDocumentos(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar documentos SST.');
    } finally {
      setLoading(false);
    }
  }, [isAdmin, activeEmpresaId, somenteVigentes, toast]);

  useEffect(() => {
    loadDocumentos();
  }, [loadDocumentos]);

  // Identifica se o tipo selecionado é PCMSO para sugerir o PGR vigente como documento base
  const selectedTipoObj = useMemo(
    () => tiposDoc.find((t) => String(t.id) === String(form.tipoDocumentoId)),
    [tiposDoc, form.tipoDocumentoId]
  );

  const isPcmsoSelected = useMemo(() => {
    const sigla = (selectedTipoObj?.sigla || '').toUpperCase();
    const nome = (selectedTipoObj?.nome || '').toUpperCase();
    return sigla === 'PCMSO' || nome.includes('PCMSO');
  }, [selectedTipoObj]);

  const pgrVigente = useMemo(
    () =>
      documentos.find(
        (d) =>
          d.vigente &&
          ((d.tipoDocumentoSigla || '').toUpperCase() === 'PGR' ||
            (d.tipoDocumentoNome || '').toUpperCase().includes('PGR'))
      ),
    [documentos]
  );

  const handleTipoChange = (newTipoId) => {
    const tipoObj = tiposDoc.find((t) => String(t.id) === String(newTipoId));
    const sigla = (tipoObj?.sigla || '').toUpperCase();
    let suggestedBaseId = form.documentoBaseId;

    if (sigla === 'PCMSO' && pgrVigente) {
      suggestedBaseId = String(pgrVigente.id);
    }

    setForm((prev) => ({
      ...prev,
      tipoDocumentoId: newTipoId,
      documentoBaseId: suggestedBaseId,
    }));
  };

  const openUploadModal = () => {
    const today = new Date().toISOString().slice(0, 10);
    const defaultTipo = tiposDoc[0];
    const defaultIsPcmso = (defaultTipo?.sigla || '').toUpperCase() === 'PCMSO';

    setForm({
      tipoDocumentoId: defaultTipo ? String(defaultTipo.id) : '',
      titulo: '',
      descricao: '',
      dataEmissao: today,
      dataValidade: '',
      documentoBaseId: defaultIsPcmso && pgrVigente ? String(pgrVigente.id) : '',
      arquivo: null,
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const handleUploadSubmit = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setErrorMsg('');

    const errs = {};
    if (!form.tipoDocumentoId) errs.tipoDocumentoId = 'Selecione o tipo de documento.';
    if (!form.titulo.trim()) errs.titulo = 'Informe o título do documento.';
    if (!form.dataEmissao) errs.dataEmissao = 'Informe a data de emissão.';

    const pdfErr = validatePdfFile(form.arquivo);
    if (pdfErr) errs.arquivo = pdfErr;

    if (Object.keys(errs).length > 0) {
      setFieldErrors(errs);
      return;
    }

    setSaving(true);
    try {
      await api.upload(
        '/documentos',
        {
          tipoDocumentoId: form.tipoDocumentoId,
          titulo: form.titulo.trim(),
          descricao: form.descricao.trim() || undefined,
          dataEmissao: form.dataEmissao,
          dataValidade: form.dataValidade || undefined,
          documentoBaseId: form.documentoBaseId || undefined,
        },
        form.arquivo
      );
      toast.success('Documento SST enviado e versionado com sucesso!');
      setModalOpen(false);
      loadDocumentos();
    } catch (err) {
      setErrorMsg(err.detail || 'Erro ao enviar documento.');
      if (err.erros) setFieldErrors(err.erros);
    } finally {
      setSaving(false);
    }
  };

  const handleViewPdf = async (docId) => {
    try {
      await api.openPdfInNewTab(`/documentos/${docId}/arquivo`);
    } catch (err) {
      toast.error(err.detail || 'Não foi possível visualizar o PDF do documento.');
    }
  };

  const columns = [
    {
      header: 'Tipo / Versão',
      render: (d) => (
        <div className="flex-gap-sm">
          <Badge variant="info">{d.tipoDocumentoSigla || d.tipoDocumentoNome}</Badge>
          <Badge variant={d.vigente ? 'success' : 'neutral'}>
            v{d.versao} {d.vigente ? '(Vigente)' : '(Histórico)'}
          </Badge>
        </div>
      ),
    },
    {
      header: 'Título / Documento Base',
      render: (d) => (
        <div>
          <div className="font-semibold">{d.titulo}</div>
          {d.descricao && <div className="text-xs text-muted">{d.descricao}</div>}
          {d.documentoBaseTitulo && (
            <div className="text-xs text-primary" style={{ marginTop: 3 }}>
              <Link2 size={12} style={{ verticalAlign: 'middle' }} /> Baseado em:{' '}
              <strong>{d.documentoBaseTitulo}</strong>
            </div>
          )}
        </div>
      ),
    },
    {
      header: 'Emissão / Validade',
      render: (d) => (
        <div className="text-sm">
          <div>Emissão: {formatDate(d.dataEmissao)}</div>
          <div>
            Validade: <strong>{formatDate(d.dataValidade)}</strong>
          </div>
          {d.diasParaVencer !== null && d.diasParaVencer !== undefined && (
            <div className="text-xs text-muted">
              {d.diasParaVencer < 0
                ? `Vencido há ${Math.abs(d.diasParaVencer)} dia(s)`
                : `Vence em ${d.diasParaVencer} dia(s)`}
            </div>
          )}
        </div>
      ),
    },
    {
      header: 'Status de Validade',
      render: (d) => <ValidadeBadge status={d.statusValidade} />,
    },
    {
      header: 'Enviado por',
      render: (d) => (
        <div className="text-xs">
          <div>{d.enviadoPorNome || '—'}</div>
          <div className="text-muted">{formatDateTime(d.criadoEm)}</div>
        </div>
      ),
    },
    {
      header: 'Arquivo',
      render: (d) => (
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => handleViewPdf(d.id)}
        >
          <FileText size={15} /> Visualizar PDF
        </button>
      ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Documentos e Programas de SST (PGR / PCMSO)</h2>
          <p className="page-subtitle">
            Gerencie as versões vigentes e históricas dos programas obrigatórios de Saúde e Segurança do Trabalho.
          </p>
        </div>
        {canUpload && (
          <button type="button" className="btn btn-primary" onClick={openUploadModal}>
            <Upload size={18} /> Enviar Documento (PDF)
          </button>
        )}
      </div>

      <AdminEmpresaSelector />

      <div className="card filter-card">
        <div className="filter-row">
          <div className="filter-item">
            <label className="checkbox-field">
              <input
                type="checkbox"
                checked={somenteVigentes}
                onChange={(e) => setSomenteVigentes(e.target.checked)}
              />
              <span>
                <Filter size={14} style={{ verticalAlign: 'middle' }} /> Exibir somente documentos vigentes (última versão de cada tipo)
              </span>
            </label>
          </div>
        </div>
      </div>

      <div className="card">
        <Table
          columns={columns}
          data={documentos}
          loading={loading}
          emptyTitle="Nenhum documento SST cadastrado"
        />
      </div>

      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Enviar Novo Documento SST (PDF)"
        size="lg"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setModalOpen(false)}
              disabled={saving}
            >
              Cancelar
            </button>
            <button type="submit" form="doc-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Enviando...' : 'Salvar Documento'}
            </button>
          </>
        }
      >
        {errorMsg && (
          <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
            <AlertCircle size={18} />
            <span>{errorMsg}</span>
          </div>
        )}

        <form id="doc-form" onSubmit={handleUploadSubmit}>
          <div className="form-row-2">
            <FormField label="Tipo de Documento" required error={fieldErrors.tipoDocumentoId}>
              <select
                className="form-control"
                value={form.tipoDocumentoId}
                onChange={(e) => handleTipoChange(e.target.value)}
                required
              >
                <option value="">-- Selecione --</option>
                {tiposDoc.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.sigla} — {t.nome} ({t.periodicidadePadraoMeses} meses)
                  </option>
                ))}
              </select>
            </FormField>

            <FormField label="Título / Identificação" required error={fieldErrors.titulo}>
              <input
                type="text"
                className="form-control"
                placeholder="Ex: PGR Matriz 2026/2027"
                value={form.titulo}
                onChange={(e) => setForm({ ...form, titulo: e.target.value })}
                required
              />
            </FormField>
          </div>

          <div className="form-row-2">
            <FormField label="Data de Emissão" required error={fieldErrors.dataEmissao}>
              <input
                type="date"
                className="form-control"
                value={form.dataEmissao}
                onChange={(e) => setForm({ ...form, dataEmissao: e.target.value })}
                required
              />
            </FormField>

            <FormField
              label="Data de Validade (opcional)"
              error={fieldErrors.dataValidade}
              hint={
                selectedTipoObj
                  ? `Se não informada, será calculada como Emissão + ${selectedTipoObj.periodicidadePadraoMeses} meses.`
                  : 'Se omitida, utiliza a periodicidade padrão do tipo.'
              }
            >
              <input
                type="date"
                className="form-control"
                value={form.dataValidade}
                onChange={(e) => setForm({ ...form, dataValidade: e.target.value })}
              />
            </FormField>
          </div>

          <FormField
            label="Documento Base Vinculado (opcional)"
            error={fieldErrors.documentoBaseId}
            hint={
              isPcmsoSelected
                ? 'Recomendação normativa (NR-07): vincule o PCMSO ao PGR vigente da empresa.'
                : 'Opcionalmente vincule um documento anterior ou programa de referência.'
            }
          >
            <select
              className="form-control"
              value={form.documentoBaseId}
              onChange={(e) => setForm({ ...form, documentoBaseId: e.target.value })}
            >
              <option value="">-- Nenhum documento base --</option>
              {documentos.map((d) => (
                <option key={d.id} value={d.id}>
                  [{d.tipoDocumentoSigla}] {d.titulo} (v{d.versao} {d.vigente ? '• Vigente' : ''})
                </option>
              ))}
            </select>
          </FormField>

          <FormField label="Descrição / Observações" error={fieldErrors.descricao}>
            <textarea
              className="form-control"
              rows={2}
              value={form.descricao}
              onChange={(e) => setForm({ ...form, descricao: e.target.value })}
            />
          </FormField>

          <FormField
            label="Arquivo PDF (somente .pdf, máx. 20 MB)"
            required
            error={fieldErrors.arquivo}
          >
            <input
              type="file"
              accept=".pdf,application/pdf"
              className="form-control"
              onChange={(e) => setForm({ ...form, arquivo: e.target.files?.[0] || null })}
              required
            />
          </FormField>
        </form>
      </Modal>
    </div>
  );
}
