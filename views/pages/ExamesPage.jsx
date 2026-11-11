import React, { useState, useEffect, useCallback } from 'react';
import {
  Calendar,
  Upload,
  FileText,
  Filter,
  AlertCircle,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useEmpresa } from '../context/EmpresaContext';
import { useToast } from '../context/ToastContext';
import {
  formatDate,
  formatDateTime,
  STATUS_VALIDADE_LABELS,
  STATUS_FLUXO_EXAME_LABELS,
  validatePdfFile,
} from '../utils/formatters';
import { AdminEmpresaSelector } from '../components/ui/AdminEmpresaSelector';
import { Table } from '../components/ui/Table';
import { Modal } from '../components/ui/Modal';
import { ValidadeBadge, FluxoExameBadge } from '../components/ui/Badge';
import {
  ClearFiltersButton,
  FormField,
  Pagination,
  EmptyState,
} from '../components/ui/Common';

export function ExamesPage() {
  const { isAdmin, isRh } = useAuth();
  const { activeEmpresaId } = useEmpresa();
  const toast = useToast();

  const [exames, setExames] = useState([]);
  const [tiposExame, setTiposExame] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);

  // Filtros
  const [statusValidade, setStatusValidade] = useState('');
  const [statusFluxo, setStatusFluxo] = useState('');
  const [tipoExameId, setTipoExameId] = useState('');
  const [incluirHistorico, setIncluirHistorico] = useState(false);

  // Modal Agendar
  const [agendarModal, setAgendarModal] = useState({ open: false, exame: null });
  const [agendarForm, setAgendarForm] = useState({
    dataAgendada: '',
    localClinica: '',
    observacoes: '',
  });
  const [agendarErrors, setAgendarErrors] = useState({});
  const [agendarMsg, setAgendarMsg] = useState('');

  // Modal Enviar Resultado
  const [resultadoModal, setResultadoModal] = useState({ open: false, exame: null });
  const [resultadoForm, setResultadoForm] = useState({
    dataRealizacao: '',
    arquivo: null,
  });
  const [resultadoErrors, setResultadoErrors] = useState({});
  const [resultadoMsg, setResultadoMsg] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api
      .get('/tipos-exame', { ativos: true })
      .then((res) => setTiposExame(Array.isArray(res) ? res : []))
      .catch(() => {});
  }, []);

  const loadExames = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get('/exames', {
        empresaId: isAdmin && activeEmpresaId ? activeEmpresaId : undefined,
        statusValidade: statusValidade || undefined,
        statusFluxo: statusFluxo || undefined,
        tipoExameId: tipoExameId || undefined,
        incluirHistorico: incluirHistorico ? true : undefined,
        page,
        size: 15,
      });
      setExames(res?.content || []);
      setTotalPages(res?.totalPages || 0);
      setTotalElements(res?.totalElements || 0);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar exames ocupacionais.');
    } finally {
      setLoading(false);
    }
  }, [
    isAdmin,
    activeEmpresaId,
    statusValidade,
    statusFluxo,
    tipoExameId,
    incluirHistorico,
    page,
    toast,
  ]);

  useEffect(() => {
    loadExames();
  }, [loadExames]);

  const openAgendar = (exame) => {
    setAgendarModal({ open: true, exame });
    setAgendarForm({
      dataAgendada: exame.dataAgendada || '',
      localClinica: exame.localClinica || '',
      observacoes: exame.observacoes || '',
    });
    setAgendarErrors({});
    setAgendarMsg('');
  };

  const handleAgendarSubmit = async (e) => {
    e.preventDefault();
    setAgendarErrors({});
    setAgendarMsg('');

    const today = new Date().toISOString().slice(0, 10);
    if (!agendarForm.dataAgendada || agendarForm.dataAgendada <= today) {
      setAgendarErrors({ dataAgendada: 'A data agendada deve ser futura.' });
      return;
    }
    if (!agendarForm.localClinica.trim()) {
      setAgendarErrors({ localClinica: 'Informe o local ou clínica.' });
      return;
    }

    setSaving(true);
    try {
      await api.post(`/exames/${agendarModal.exame.id}/agendar`, {
        dataAgendada: agendarForm.dataAgendada,
        localClinica: agendarForm.localClinica.trim(),
        observacoes: agendarForm.observacoes.trim() || null,
      });
      toast.success('Exame agendado com sucesso!');
      setAgendarModal({ open: false, exame: null });
      loadExames();
    } catch (err) {
      setAgendarMsg(err.detail || 'Erro ao agendar exame.');
      if (err.erros) setAgendarErrors(err.erros);
    } finally {
      setSaving(false);
    }
  };

  const openResultado = (exame) => {
    const today = new Date().toISOString().slice(0, 10);
    setResultadoModal({ open: true, exame });
    setResultadoForm({
      dataRealizacao: exame.dataRealizacao || today,
      arquivo: null,
    });
    setResultadoErrors({});
    setResultadoMsg('');
  };

  const handleResultadoSubmit = async (e) => {
    e.preventDefault();
    setResultadoErrors({});
    setResultadoMsg('');

    const pdfErr = validatePdfFile(resultadoForm.arquivo);
    if (pdfErr) {
      setResultadoErrors({ arquivo: pdfErr });
      return;
    }
    if (!resultadoForm.dataRealizacao) {
      setResultadoErrors({ dataRealizacao: 'Informe a data de realização.' });
      return;
    }

    setSaving(true);
    try {
      await api.upload(
        `/exames/${resultadoModal.exame.id}/resultado`,
        { dataRealizacao: resultadoForm.dataRealizacao },
        resultadoForm.arquivo
      );
      toast.success('Resultado em PDF enviado! Encaminhado para assinatura do Médico do Trabalho.');
      setResultadoModal({ open: false, exame: null });
      loadExames();
    } catch (err) {
      setResultadoMsg(err.detail || 'Erro ao enviar resultado.');
      if (err.erros) setResultadoErrors(err.erros);
    } finally {
      setSaving(false);
    }
  };

  const handleViewPdf = async (exameId) => {
    try {
      await api.openPdfInNewTab(`/exames/${exameId}/arquivo`);
    } catch (err) {
      toast.error(err.detail || 'Erro ao abrir PDF do exame.');
    }
  };

  const columns = [
    {
      header: 'Colaborador',
      render: (ex) => <span className="font-semibold">{ex.colaboradorNome}</span>,
    },
    {
      header: 'Tipo de Exame',
      render: (ex) => (
        <div>
          <div>{ex.tipoExameNome}</div>
          <div className="text-xs text-muted">Periodicidade: {ex.periodicidadeMeses} meses</div>
        </div>
      ),
    },
    {
      header: 'Vencimento / Prazo',
      render: (ex) => (
        <div>
          <div className="font-semibold">{formatDate(ex.dataVencimento)}</div>
          {ex.diasParaVencer !== null && ex.diasParaVencer !== undefined && (
            <div className="text-xs text-muted">
              {ex.diasParaVencer < 0
                ? `Vencido há ${Math.abs(ex.diasParaVencer)} dia(s)`
                : `Vence em ${ex.diasParaVencer} dia(s)`}
            </div>
          )}
        </div>
      ),
    },
    {
      header: 'Validade',
      render: (ex) => <ValidadeBadge status={ex.statusValidade} />,
    },
    {
      header: 'Status do Fluxo',
      render: (ex) => (
        <div>
          <FluxoExameBadge status={ex.statusFluxo} />
          {ex.statusFluxo === 'REVISADO_PENDENTE' && ex.justificativaRecusa && (
            <div className="rejection-note">
              <strong>Justificativa da recusa:</strong> {ex.justificativaRecusa}
            </div>
          )}
          {ex.statusFluxo === 'HOMOLOGADO' && ex.medicoNome && (
            <div className="text-xs text-muted" style={{ marginTop: 4 }}>
              Dr(a). {ex.medicoNome} (CRM {ex.crmAssinatura}/{ex.crmUfAssinatura}) em{' '}
              {formatDateTime(ex.assinadoEm)}
            </div>
          )}
        </div>
      ),
    },
    {
      header: 'Agendamento / Realização',
      render: (ex) => (
        <div className="text-xs">
          {ex.dataAgendada && (
            <div>
              Agendado: <strong>{formatDate(ex.dataAgendada)}</strong>
              {ex.localClinica ? ` — ${ex.localClinica}` : ''}
            </div>
          )}
          {ex.dataRealizacao && <div>Realizado: {formatDate(ex.dataRealizacao)}</div>}
          {!ex.dataAgendada && !ex.dataRealizacao && <span className="text-muted">—</span>}
        </div>
      ),
    },
    {
      header: 'Ações',
      render: (ex) => (
        <div className="table-actions">
          {ex.possuiArquivo && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => handleViewPdf(ex.id)}
              title="Visualizar PDF em nova aba"
            >
              <FileText size={14} /> PDF
            </button>
          )}
          {isRh && ex.statusFluxo !== 'HOMOLOGADO' && (
            <>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => openAgendar(ex)}
              >
                <Calendar size={14} /> Agendar
              </button>
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={() => openResultado(ex)}
              >
                <Upload size={14} /> Enviar Resultado
              </button>
            </>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Exames Ocupacionais</h2>
          <p className="page-subtitle">
            Monitore os prazos de validade, agende exames periódicos e envie os resultados em PDF para homologação médica.
          </p>
        </div>
      </div>

      <AdminEmpresaSelector />

      {/* Filtros */}
      <div className="card filter-card">
        <div className="filter-row">
          <div className="filter-item">
            <label className="form-label">
              <Filter size={14} /> Status de Validade
            </label>
            <select
              className="form-control"
              value={statusValidade}
              onChange={(e) => {
                setStatusValidade(e.target.value);
                setPage(0);
              }}
            >
              <option value="">Todos</option>
              {Object.entries(STATUS_VALIDADE_LABELS).map(([k, label]) => (
                <option key={k} value={k}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-item">
            <label className="form-label">Status do Fluxo</label>
            <select
              className="form-control"
              value={statusFluxo}
              onChange={(e) => {
                setStatusFluxo(e.target.value);
                setPage(0);
              }}
            >
              <option value="">Todos</option>
              {Object.entries(STATUS_FLUXO_EXAME_LABELS).map(([k, label]) => (
                <option key={k} value={k}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-item">
            <label className="form-label">Tipo de Exame</label>
            <select
              className="form-control"
              value={tipoExameId}
              onChange={(e) => {
                setTipoExameId(e.target.value);
                setPage(0);
              }}
            >
              <option value="">Todos os tipos</option>
              {tiposExame.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.nome}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-item" style={{ justifyContent: 'flex-end' }}>
            <label className="checkbox-field" style={{ marginTop: 24 }}>
              <input
                type="checkbox"
                checked={incluirHistorico}
                onChange={(e) => {
                  setIncluirHistorico(e.target.checked);
                  setPage(0);
                }}
              />
              <span>Incluir histórico homologado</span>
            </label>
          </div>

        </div>
        <ClearFiltersButton
          active={Boolean(statusValidade || statusFluxo || tipoExameId || incluirHistorico)}
          onClick={() => {
            setStatusValidade('');
            setStatusFluxo('');
            setTipoExameId('');
            setIncluirHistorico(false);
            setPage(0);
          }}
        />
      </div>

      {/* Tabela */}
      <div className="card">
        <Table
          columns={columns}
          data={exames}
          loading={loading}
          emptyTitle="Nenhum exame encontrado para os filtros aplicados"
        />
        <Pagination
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={setPage}
        />
      </div>

      {/* Modal Agendar */}
      <Modal
        isOpen={agendarModal.open}
        onClose={() => setAgendarModal({ open: false, exame: null })}
        title={`Agendar Exame — ${agendarModal.exame?.colaboradorNome || ''}`}
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setAgendarModal({ open: false, exame: null })}
              disabled={saving}
            >
              Cancelar
            </button>
            <button type="submit" form="agendar-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Salvando...' : 'Agendar Exame'}
            </button>
          </>
        }
      >
        {agendarMsg && (
          <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
            <AlertCircle size={18} />
            <span>{agendarMsg}</span>
          </div>
        )}
        <p className="text-sm text-muted" style={{ marginBottom: '1rem' }}>
          Exame: <strong>{agendarModal.exame?.tipoExameNome}</strong> • Vencimento:{' '}
          <strong>{formatDate(agendarModal.exame?.dataVencimento)}</strong>
        </p>
        <form id="agendar-form" onSubmit={handleAgendarSubmit}>
          <FormField
            label="Data Agendada (deve ser futura)"
            required
            error={agendarErrors.dataAgendada}
          >
            <input
              type="date"
              className="form-control"
              value={agendarForm.dataAgendada}
              onChange={(e) => setAgendarForm({ ...agendarForm, dataAgendada: e.target.value })}
              required
            />
          </FormField>

          <FormField
            label="Local / Clínica Ocupacional"
            required
            error={agendarErrors.localClinica}
          >
            <input
              type="text"
              className="form-control"
              placeholder="Informe a clínica onde o colaborador fará o exame"
              value={agendarForm.localClinica}
              onChange={(e) => setAgendarForm({ ...agendarForm, localClinica: e.target.value })}
              required
            />
          </FormField>

          <FormField label="Observações" error={agendarErrors.observacoes}>
            <textarea
              className="form-control"
              rows={3}
              placeholder="Instruções de preparo, endereço ou horário..."
              value={agendarForm.observacoes}
              onChange={(e) => setAgendarForm({ ...agendarForm, observacoes: e.target.value })}
            />
          </FormField>
        </form>
      </Modal>

      {/* Modal Enviar Resultado */}
      <Modal
        isOpen={resultadoModal.open}
        onClose={() => setResultadoModal({ open: false, exame: null })}
        title={`Enviar Resultado em PDF — ${resultadoModal.exame?.colaboradorNome || ''}`}
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setResultadoModal({ open: false, exame: null })}
              disabled={saving}
            >
              Cancelar
            </button>
            <button type="submit" form="resultado-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Enviando...' : 'Enviar Resultado'}
            </button>
          </>
        }
      >
        {resultadoMsg && (
          <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
            <AlertCircle size={18} />
            <span>{resultadoMsg}</span>
          </div>
        )}
        <p className="text-sm text-muted" style={{ marginBottom: '1rem' }}>
          Exame: <strong>{resultadoModal.exame?.tipoExameNome}</strong>. Após o envio, o status mudará para{' '}
          <strong>Aguardando Assinatura</strong> do Médico do Trabalho.
        </p>
        <form id="resultado-form" onSubmit={handleResultadoSubmit}>
          <FormField
            label="Data de Realização"
            required
            error={resultadoErrors.dataRealizacao}
          >
            <input
              type="date"
              className="form-control"
              value={resultadoForm.dataRealizacao}
              onChange={(e) =>
                setResultadoForm({ ...resultadoForm, dataRealizacao: e.target.value })
              }
              required
            />
          </FormField>

          <FormField
            label="Arquivo PDF do Exame / ASO (máx. 20 MB)"
            required
            error={resultadoErrors.arquivo}
          >
            <input
              type="file"
              accept=".pdf,application/pdf"
              className="form-control"
              onChange={(e) =>
                setResultadoForm({ ...resultadoForm, arquivo: e.target.files?.[0] || null })
              }
              required
            />
          </FormField>
        </form>
      </Modal>
    </div>
  );
}
