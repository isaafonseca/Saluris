import React, { useState, useEffect, useCallback } from 'react';
import {
  Upload,
  FileText,
  RefreshCw,
  Filter,
  AlertCircle,
  CheckCircle2,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useEmpresa } from '../context/EmpresaContext';
import { useToast } from '../context/ToastContext';
import {
  formatDate,
  formatDateTime,
  STATUS_LAUDO_LABELS,
  validatePdfFile,
} from '../utils/formatters';
import { AdminEmpresaSelector } from '../components/ui/AdminEmpresaSelector';
import { Table } from '../components/ui/Table';
import { Modal } from '../components/ui/Modal';
import { Badge, LaudoStatusBadge } from '../components/ui/Badge';
import { FormField, EmptyState } from '../components/ui/Common';

export function LaudosPage() {
  const { isAdmin, isEspecialista } = useAuth();
  const { activeEmpresaId } = useEmpresa();
  const toast = useToast();

  const [laudos, setLaudos] = useState([]);
  const [tiposLaudo, setTiposLaudo] = useState([]);
  const [colaboradores, setColaboradores] = useState([]);
  const [statusFiltro, setStatusFiltro] = useState('');
  const [loading, setLoading] = useState(false);

  // Modal Criar Laudo (Especialista)
  const [createModal, setCreateModal] = useState(false);
  const [form, setForm] = useState({
    tipoLaudoId: '',
    titulo: '',
    descricao: '',
    dataEmissao: '',
    dataValidade: '',
    colaboradorId: '',
    arquivo: null,
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMsg, setErrorMsg] = useState('');
  const [saving, setSaving] = useState(false);

  // Modal Reenviar Laudo (Especialista quando REVISADO_PENDENTE)
  const [reenviarModal, setReenviarModal] = useState({ open: false, laudo: null });
  const [reenviarFile, setReenviarFile] = useState(null);
  const [reenviarError, setReenviarError] = useState('');

  useEffect(() => {
    api
      .get('/tipos-laudo', { ativos: true })
      .then((res) => setTiposLaudo(Array.isArray(res) ? res : []))
      .catch(() => {});
  }, []);

  const loadColaboradores = useCallback(async () => {
    if (!isEspecialista) return;
    try {
      const res = await api.get('/colaboradores', { page: 0, size: 200 });
      setColaboradores(res?.content || []);
    } catch (_) {}
  }, [isEspecialista]);

  useEffect(() => {
    loadColaboradores();
  }, [loadColaboradores]);

  const loadLaudos = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.get('/laudos', {
        empresaId: isAdmin && activeEmpresaId ? activeEmpresaId : undefined,
        status: statusFiltro || undefined,
      });
      setLaudos(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar laudos técnicos.');
    } finally {
      setLoading(false);
    }
  }, [isAdmin, activeEmpresaId, statusFiltro, toast]);

  useEffect(() => {
    loadLaudos();
  }, [loadLaudos]);

  const openCreateModal = () => {
    const today = new Date().toISOString().slice(0, 10);
    setForm({
      tipoLaudoId: tiposLaudo[0]?.id ? String(tiposLaudo[0].id) : '',
      titulo: '',
      descricao: '',
      dataEmissao: today,
      dataValidade: '',
      colaboradorId: '',
      arquivo: null,
    });
    setFieldErrors({});
    setErrorMsg('');
    setCreateModal(true);
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setErrorMsg('');

    const errs = {};
    if (!form.tipoLaudoId) errs.tipoLaudoId = 'Selecione o tipo de laudo.';
    if (!form.titulo.trim()) errs.titulo = 'Informe o título do laudo.';
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
        '/laudos',
        {
          tipoLaudoId: form.tipoLaudoId,
          titulo: form.titulo.trim(),
          descricao: form.descricao.trim() || undefined,
          dataEmissao: form.dataEmissao,
          dataValidade: form.dataValidade || undefined,
          colaboradorId: form.colaboradorId || undefined,
        },
        form.arquivo
      );
      toast.success('Laudo enviado! Encaminhado para a fila de assinatura do Médico do Trabalho.');
      setCreateModal(false);
      loadLaudos();
    } catch (err) {
      setErrorMsg(err.detail || 'Erro ao cadastrar laudo.');
      if (err.erros) setFieldErrors(err.erros);
    } finally {
      setSaving(false);
    }
  };

  const openReenviarModal = (laudo) => {
    setReenviarModal({ open: true, laudo });
    setReenviarFile(null);
    setReenviarError('');
  };

  const handleReenviarSubmit = async (e) => {
    e.preventDefault();
    setReenviarError('');
    const pdfErr = validatePdfFile(reenviarFile);
    if (pdfErr) {
      setReenviarError(pdfErr);
      return;
    }

    setSaving(true);
    try {
      await api.upload(`/laudos/${reenviarModal.laudo.id}/reenviar`, {}, reenviarFile);
      toast.success('Laudo corrigido e reenviado para assinatura médica!');
      setReenviarModal({ open: false, laudo: null });
      loadLaudos();
    } catch (err) {
      setReenviarError(err.detail || 'Erro ao reenviar laudo.');
    } finally {
      setSaving(false);
    }
  };

  const handleViewPdf = async (laudoId) => {
    try {
      await api.openPdfInNewTab(`/laudos/${laudoId}/arquivo`);
    } catch (err) {
      toast.error(err.detail || 'Erro ao abrir PDF do laudo.');
    }
  };

  const columns = [
    {
      header: 'Tipo / Título',
      render: (l) => (
        <div>
          <Badge variant="info">{l.tipoLaudoNome}</Badge>
          <div className="font-semibold" style={{ marginTop: 4 }}>
            {l.titulo}
          </div>
          {l.descricao && <div className="text-xs text-muted">{l.descricao}</div>}
          {l.colaboradorNome && (
            <div className="text-xs text-muted">Colaborador: {l.colaboradorNome}</div>
          )}
        </div>
      ),
    },
    {
      header: 'Emissão / Validade',
      render: (l) => (
        <div className="text-sm">
          <div>Emissão: {formatDate(l.dataEmissao)}</div>
          <div>Validade: {formatDate(l.dataValidade)}</div>
        </div>
      ),
    },
    {
      header: 'Status / Homologação Médica',
      render: (l) => (
        <div>
          <LaudoStatusBadge status={l.status} />
          {l.status === 'HOMOLOGADO' && l.medicoNome && (
            <div className="signature-box">
              <CheckCircle2 size={14} className="text-success" />
              <span>
                Assinado por <strong>{l.medicoNome}</strong> (CRM {l.crmAssinatura}/
                {l.crmUfAssinatura}) em {formatDateTime(l.assinadoEm)}
              </span>
            </div>
          )}
          {l.status === 'REVISADO_PENDENTE' && l.justificativaRecusa && (
            <div className="rejection-note">
              <strong>Justificativa da recusa médica:</strong> {l.justificativaRecusa}
            </div>
          )}
        </div>
      ),
    },
    {
      header: 'Elaborado por',
      render: (l) => (
        <div className="text-xs">
          <div>{l.criadoPorNome || '—'}</div>
          <div className="text-muted">{formatDateTime(l.criadoEm)}</div>
        </div>
      ),
    },
    {
      header: 'Ações',
      render: (l) => (
        <div className="table-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleViewPdf(l.id)}
          >
            <FileText size={15} /> Ver PDF
          </button>
          {isEspecialista && l.status === 'REVISADO_PENDENTE' && (
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={() => openReenviarModal(l)}
            >
              <RefreshCw size={14} /> Reenviar PDF
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Laudos Técnicos (LTCAT, Insalubridade, Periculosidade, Ergonômico)</h2>
          <p className="page-subtitle">
            Envie laudos em PDF para homologação do Médico do Trabalho e acompanhe o registro de assinaturas e revisões.
          </p>
        </div>
        {isEspecialista && (
          <button type="button" className="btn btn-primary" onClick={openCreateModal}>
            <Upload size={18} /> Novo Laudo Técnico
          </button>
        )}
      </div>

      <AdminEmpresaSelector />

      <div className="card filter-card">
        <div className="filter-row">
          <div className="filter-item">
            <label className="form-label">
              <Filter size={14} /> Filtrar por Status de Homologação
            </label>
            <select
              className="form-control"
              value={statusFiltro}
              onChange={(e) => setStatusFiltro(e.target.value)}
            >
              <option value="">Todos os status</option>
              {Object.entries(STATUS_LAUDO_LABELS).map(([k, label]) => (
                <option key={k} value={k}>
                  {label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div className="card">
        <Table
          columns={columns}
          data={laudos}
          loading={loading}
          emptyTitle="Nenhum laudo técnico encontrado"
        />
      </div>

      {/* Modal Criar Laudo */}
      <Modal
        isOpen={createModal}
        onClose={() => setCreateModal(false)}
        title="Novo Laudo Técnico (PDF)"
        size="lg"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setCreateModal(false)}
              disabled={saving}
            >
              Cancelar
            </button>
            <button type="submit" form="laudo-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Enviando...' : 'Enviar para Assinatura'}
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

        <form id="laudo-form" onSubmit={handleCreateSubmit}>
          <div className="form-row-2">
            <FormField label="Tipo de Laudo" required error={fieldErrors.tipoLaudoId}>
              <select
                className="form-control"
                value={form.tipoLaudoId}
                onChange={(e) => setForm({ ...form, tipoLaudoId: e.target.value })}
                required
              >
                <option value="">-- Selecione --</option>
                {tiposLaudo.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nome}
                  </option>
                ))}
              </select>
            </FormField>

            <FormField label="Título do Laudo" required error={fieldErrors.titulo}>
              <input
                type="text"
                className="form-control"
                placeholder="Ex: LTCAT Setor de Produção 2026"
                value={form.titulo}
                onChange={(e) => setForm({ ...form, titulo: e.target.value })}
                required
              />
            </FormField>
          </div>

          <div className="form-row-3">
            <FormField label="Data de Emissão" required error={fieldErrors.dataEmissao}>
              <input
                type="date"
                className="form-control"
                value={form.dataEmissao}
                onChange={(e) => setForm({ ...form, dataEmissao: e.target.value })}
                required
              />
            </FormField>

            <FormField label="Data de Validade (opcional)" error={fieldErrors.dataValidade}>
              <input
                type="date"
                className="form-control"
                value={form.dataValidade}
                onChange={(e) => setForm({ ...form, dataValidade: e.target.value })}
              />
            </FormField>

            <FormField label="Colaborador Específico (opcional)" error={fieldErrors.colaboradorId}>
              <select
                className="form-control"
                value={form.colaboradorId}
                onChange={(e) => setForm({ ...form, colaboradorId: e.target.value })}
              >
                <option value="">Geral da Empresa / Setor</option>
                {colaboradores.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nome}
                  </option>
                ))}
              </select>
            </FormField>
          </div>

          <FormField label="Descrição / Resumo Técnico" error={fieldErrors.descricao}>
            <textarea
              className="form-control"
              rows={2}
              value={form.descricao}
              onChange={(e) => setForm({ ...form, descricao: e.target.value })}
            />
          </FormField>

          <FormField
            label="Arquivo PDF do Laudo (máx. 20 MB)"
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

      {/* Modal Reenviar Laudo Corrigido */}
      <Modal
        isOpen={reenviarModal.open}
        onClose={() => setReenviarModal({ open: false, laudo: null })}
        title={`Reenviar Laudo Revisado — ${reenviarModal.laudo?.titulo || ''}`}
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setReenviarModal({ open: false, laudo: null })}
              disabled={saving}
            >
              Cancelar
            </button>
            <button type="submit" form="reenviar-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Reenviando...' : 'Reenviar para Assinatura'}
            </button>
          </>
        }
      >
        {reenviarModal.laudo?.justificativaRecusa && (
          <div className="rejection-note" style={{ marginBottom: '1rem' }}>
            <strong>Motivo da solicitação de revisão médica:</strong>{' '}
            {reenviarModal.laudo.justificativaRecusa}
          </div>
        )}

        <form id="reenviar-form" onSubmit={handleReenviarSubmit}>
          <FormField
            label="Novo Arquivo PDF Corrigido (máx. 20 MB)"
            required
            error={reenviarError}
          >
            <input
              type="file"
              accept=".pdf,application/pdf"
              className="form-control"
              onChange={(e) => setReenviarFile(e.target.files?.[0] || null)}
              required
            />
          </FormField>
        </form>
      </Modal>
    </div>
  );
}
