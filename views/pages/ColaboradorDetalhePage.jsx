import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Calendar,
  Upload,
  FileText,
  User,
  Briefcase,
  MapPin,
  AlertCircle,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import {
  formatCPF,
  formatDate,
  formatDateTime,
  validatePdfFile,
} from '../utils/formatters';
import { Table } from '../components/ui/Table';
import { Modal } from '../components/ui/Modal';
import { Badge, ValidadeBadge, FluxoExameBadge } from '../components/ui/Badge';
import { FormField, Loading } from '../components/ui/Common';

export function ColaboradorDetalhePage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isRh } = useAuth();
  const toast = useToast();

  const [colaborador, setColaborador] = useState(null);
  const [exames, setExames] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modal Agendar Exame
  const [agendarModal, setAgendarModal] = useState({ open: false, exame: null });
  const [agendarForm, setAgendarForm] = useState({
    dataAgendada: '',
    localClinica: '',
    observacoes: '',
  });
  const [agendarErrors, setAgendarErrors] = useState({});
  const [agendarMsg, setAgendarMsg] = useState('');

  // Modal Enviar Resultado PDF
  const [resultadoModal, setResultadoModal] = useState({ open: false, exame: null });
  const [resultadoForm, setResultadoForm] = useState({
    dataRealizacao: '',
    arquivo: null,
  });
  const [resultadoErrors, setResultadoErrors] = useState({});
  const [resultadoMsg, setResultadoMsg] = useState('');
  const [saving, setSaving] = useState(false);

  const loadDetail = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get(`/colaboradores/${id}`);
      setColaborador(res?.colaborador || null);
      setExames(Array.isArray(res?.exames) ? res.exames : []);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar detalhes do colaborador.');
    } finally {
      setLoading(false);
    }
  }, [id, toast]);

  useEffect(() => {
    loadDetail();
  }, [loadDetail]);

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
      setAgendarErrors({ dataAgendada: 'A data de agendamento deve ser futura.' });
      return;
    }
    if (!agendarForm.localClinica.trim()) {
      setAgendarErrors({ localClinica: 'Informe o nome da clínica ou local.' });
      return;
    }

    setSaving(true);
    try {
      await api.post(`/exames/${agendarModal.exame.id}/agendar`, {
        dataAgendada: agendarForm.dataAgendada,
        localClinica: agendarForm.localClinica.trim(),
        observacoes: agendarForm.observacoes.trim() || null,
      });
      toast.success('Exame agendado com sucesso.');
      setAgendarModal({ open: false, exame: null });
      loadDetail();
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

    const fileError = validatePdfFile(resultadoForm.arquivo);
    if (fileError) {
      setResultadoErrors({ arquivo: fileError });
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
      toast.success('Resultado enviado! O exame aguarda assinatura do Médico do Trabalho.');
      setResultadoModal({ open: false, exame: null });
      loadDetail();
    } catch (err) {
      setResultadoMsg(err.detail || 'Erro ao enviar resultado do exame.');
      if (err.erros) setResultadoErrors(err.erros);
    } finally {
      setSaving(false);
    }
  };

  const handleViewPdf = async (exameId) => {
    try {
      await api.openPdfInNewTab(`/exames/${exameId}/arquivo`);
    } catch (err) {
      toast.error(err.detail || 'Não foi possível abrir o arquivo PDF do exame.');
    }
  };

  if (loading) {
    return (
      <div className="page-container">
        <Loading />
      </div>
    );
  }

  if (!colaborador) {
    return (
      <div className="page-container">
        <button type="button" className="btn btn-secondary" onClick={() => navigate('/colaboradores')}>
          <ArrowLeft size={16} /> Voltar
        </button>
        <div className="card" style={{ marginTop: '1rem' }}>
          <p>Colaborador não encontrado.</p>
        </div>
      </div>
    );
  }

  const columns = [
    {
      header: 'Tipo de Exame',
      render: (ex) => (
        <div>
          <div className="font-semibold">{ex.tipoExameNome}</div>
          <div className="text-xs text-muted">Periodicidade: {ex.periodicidadeMeses} meses</div>
        </div>
      ),
    },
    {
      header: 'Status de Validade',
      render: (ex) => <ValidadeBadge status={ex.statusValidade} />,
    },
    {
      header: 'Fluxo / Homologação',
      render: (ex) => (
        <div>
          <FluxoExameBadge status={ex.statusFluxo} />
          {ex.statusFluxo === 'REVISADO_PENDENTE' && ex.justificativaRecusa && (
            <div className="rejection-note">
              <strong>Justificativa médica:</strong> {ex.justificativaRecusa}
            </div>
          )}
          {ex.statusFluxo === 'HOMOLOGADO' && ex.medicoNome && (
            <div className="text-xs text-muted" style={{ marginTop: 4 }}>
              Assinado por {ex.medicoNome} (CRM {ex.crmAssinatura}/{ex.crmUfAssinatura}) em{' '}
              {formatDateTime(ex.assinadoEm)}
            </div>
          )}
        </div>
      ),
    },
    {
      header: 'Agendamento / Realização',
      render: (ex) => (
        <div className="text-sm">
          {ex.dataAgendada && (
            <div>
              Agendado: <strong>{formatDate(ex.dataAgendada)}</strong>
              {ex.localClinica ? ` (${ex.localClinica})` : ''}
            </div>
          )}
          {ex.dataRealizacao && <div>Realizado em: {formatDate(ex.dataRealizacao)}</div>}
          {!ex.dataAgendada && !ex.dataRealizacao && <span className="text-muted">Não agendado</span>}
        </div>
      ),
    },
    {
      header: 'Vencimento',
      render: (ex) => (
        <div>
          <div className="font-semibold">{formatDate(ex.dataVencimento)}</div>
          {ex.diasParaVencer !== null && ex.diasParaVencer !== undefined && (
            <div className="text-xs text-muted">
              {ex.diasParaVencer < 0
                ? `Vencido há ${Math.abs(ex.diasParaVencer)} dia(s)`
                : `Em ${ex.diasParaVencer} dia(s)`}
            </div>
          )}
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
            >
              <FileText size={14} /> Ver PDF
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
                <Upload size={14} /> Resultado
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
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            style={{ marginBottom: '0.5rem' }}
            onClick={() => navigate('/colaboradores')}
          >
            <ArrowLeft size={15} /> Voltar para Colaboradores
          </button>
          <h2>Prontuário Ocupacional — {colaborador.nome}</h2>
          <p className="page-subtitle">
            Histórico e situação atual dos exames ocupacionais vinculados ao colaborador.
          </p>
        </div>
        <ValidadeBadge status={colaborador.statusConformidade} />
      </div>

      {/* Dados cadastrais do colaborador */}
      <div className="card" style={{ marginBottom: '1.25rem' }}>
        <div className="card-body detail-grid">
          <div className="detail-item">
            <span className="detail-label">
              <User size={14} /> CPF / Nascimento
            </span>
            <span className="detail-value">
              {formatCPF(colaborador.cpf)} • Nasc.: {formatDate(colaborador.dataNascimento)}
            </span>
          </div>
          <div className="detail-item">
            <span className="detail-label">
              <MapPin size={14} /> Unidade Operacional
            </span>
            <span className="detail-value">{colaborador.unidadeNome || '—'}</span>
          </div>
          <div className="detail-item">
            <span className="detail-label">
              <Briefcase size={14} /> Função & Setor
            </span>
            <span className="detail-value">
              {colaborador.funcaoNome} • {colaborador.setorNome}
            </span>
          </div>
          <div className="detail-item">
            <span className="detail-label">Admissão / Status</span>
            <span className="detail-value">
              {formatDate(colaborador.dataAdmissao)}{' '}
              <Badge variant={colaborador.ativo ? 'success' : 'neutral'}>
                {colaborador.ativo ? 'Ativo' : 'Desligado'}
              </Badge>
            </span>
          </div>
        </div>
      </div>

      {/* Tabela de exames do colaborador */}
      <div className="card">
        <div className="card-header">
          <h3>Exames Ocupacionais do Colaborador ({exames.length})</h3>
        </div>
        <Table
          columns={columns}
          data={exames}
          emptyTitle="Nenhum exame registrado para este colaborador"
          emptyDescription="Verifique se a matriz de periodicidade está configurada para a função e setor deste colaborador."
        />
      </div>

      {/* Modal Agendar */}
      <Modal
        isOpen={agendarModal.open}
        onClose={() => setAgendarModal({ open: false, exame: null })}
        title={`Agendar Exame — ${agendarModal.exame?.tipoExameNome || ''}`}
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
            <button type="submit" form="agendar-colab-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Agendando...' : 'Confirmar Agendamento'}
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
        <form id="agendar-colab-form" onSubmit={handleAgendarSubmit}>
          <FormField
            label="Data Agendada (futura)"
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
            label="Clínica / Local de Realização"
            required
            error={agendarErrors.localClinica}
          >
            <input
              type="text"
              className="form-control"
              placeholder="Ex: Clínica Medicina do Trabalho Centro"
              value={agendarForm.localClinica}
              onChange={(e) => setAgendarForm({ ...agendarForm, localClinica: e.target.value })}
              required
            />
          </FormField>
          <FormField label="Observações / Orientações" error={agendarErrors.observacoes}>
            <textarea
              className="form-control"
              rows={3}
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
        title={`Enviar Resultado — ${resultadoModal.exame?.tipoExameNome || ''}`}
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
            <button type="submit" form="res-colab-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Enviando...' : 'Enviar para Assinatura Médica'}
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
        <form id="res-colab-form" onSubmit={handleResultadoSubmit}>
          <FormField
            label="Data de Realização do Exame"
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
            label="Arquivo do Exame / ASO (PDF, máx. 20 MB)"
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
