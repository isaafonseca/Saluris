import React, { useState, useEffect, useCallback } from 'react';
import {
  CheckCircle2,
  XCircle,
  FileText,
  FileCheck2,
  Stethoscope,
  ShieldCheck,
  AlertCircle,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { formatDate } from '../utils/formatters';
import { Table } from '../components/ui/Table';
import { Modal } from '../components/ui/Modal';
import { Badge, LaudoStatusBadge, FluxoExameBadge } from '../components/ui/Badge';
import { FormField, Loading, EmptyState } from '../components/ui/Common';

export function AssinaturasPage() {
  const { user } = useAuth();
  const toast = useToast();

  const [laudos, setLaudos] = useState([]);
  const [exames, setExames] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modal de Confirmação de Assinatura (mostrando CRM do médico logado)
  const [signModal, setSignModal] = useState({ open: false, tipo: null, item: null });
  // Modal de Recusa (com justificativa obrigatória)
  const [rejectModal, setRejectModal] = useState({ open: false, tipo: null, item: null });
  const [justificativa, setJustificativa] = useState('');
  const [rejectError, setRejectError] = useState('');
  const [processing, setProcessing] = useState(false);

  const loadPendentes = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get('/assinaturas/pendentes');
      setLaudos(Array.isArray(res?.laudos) ? res.laudos : []);
      setExames(Array.isArray(res?.exames) ? res.exames : []);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar fila de assinaturas.');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    loadPendentes();
  }, [loadPendentes]);

  const handleViewPdf = async (tipo, id) => {
    try {
      const path = tipo === 'laudo' ? `/laudos/${id}/arquivo` : `/exames/${id}/arquivo`;
      await api.openPdfInNewTab(path);
    } catch (err) {
      toast.error(err.detail || 'Não foi possível abrir o arquivo PDF.');
    }
  };

  const handleConfirmSign = async () => {
    if (!signModal.item) return;
    setProcessing(true);
    try {
      const endpoint =
        signModal.tipo === 'laudo'
          ? `/assinaturas/laudos/${signModal.item.id}/assinar`
          : `/assinaturas/exames/${signModal.item.id}/assinar`;
      await api.post(endpoint);
      toast.success(
        `${
          signModal.tipo === 'laudo' ? 'Laudo técnico' : 'Exame ocupacional'
        } assinado e homologado com sucesso!`
      );
      setSignModal({ open: false, tipo: null, item: null });
      loadPendentes();
    } catch (err) {
      toast.error(err.detail || 'Erro ao assinar documento.');
    } finally {
      setProcessing(false);
    }
  };

  const openReject = (tipo, item) => {
    setRejectModal({ open: true, tipo, item });
    setJustificativa('');
    setRejectError('');
  };

  const handleConfirmReject = async (e) => {
    e.preventDefault();
    if (!justificativa.trim()) {
      setRejectError('A justificativa técnica é obrigatória para solicitar revisão.');
      return;
    }

    setProcessing(true);
    try {
      const endpoint =
        rejectModal.tipo === 'laudo'
          ? `/assinaturas/laudos/${rejectModal.item.id}/recusar`
          : `/assinaturas/exames/${rejectModal.item.id}/recusar`;
      await api.post(endpoint, { justificativa: justificativa.trim() });
      toast.info(
        'Documento devolvido com status "Revisado Pendente" e justificativa registrada.'
      );
      setRejectModal({ open: false, tipo: null, item: null });
      loadPendentes();
    } catch (err) {
      setRejectError(err.detail || 'Erro ao recusar documento.');
    } finally {
      setProcessing(false);
    }
  };

  const laudoColumns = [
    {
      header: 'Tipo / Título do Laudo',
      render: (l) => (
        <div>
          <Badge variant="info">{l.tipoLaudoNome}</Badge>
          <div className="font-semibold" style={{ marginTop: 4 }}>
            {l.titulo}
          </div>
          {l.descricao && <div className="text-xs text-muted">{l.descricao}</div>}
        </div>
      ),
    },
    {
      header: 'Colaborador / Escopo',
      render: (l) => l.colaboradorNome || 'Geral da Empresa',
    },
    {
      header: 'Emissão / Elaborado por',
      render: (l) => (
        <div className="text-sm">
          <div>Emissão: {formatDate(l.dataEmissao)}</div>
          <div className="text-xs text-muted">Por: {l.criadoPorNome || 'Especialista SST'}</div>
        </div>
      ),
    },
    {
      header: 'Status',
      render: (l) => <LaudoStatusBadge status={l.status} />,
    },
    {
      header: 'Ações do Médico',
      render: (l) => (
        <div className="table-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleViewPdf('laudo', l.id)}
          >
            <FileText size={14} /> Visualizar PDF
          </button>
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={() => setSignModal({ open: true, tipo: 'laudo', item: l })}
          >
            <CheckCircle2 size={14} /> Assinar
          </button>
          <button
            type="button"
            className="btn btn-outline-danger btn-sm"
            onClick={() => openReject('laudo', l)}
          >
            <XCircle size={14} /> Recusar
          </button>
        </div>
      ),
    },
  ];

  const exameColumns = [
    {
      header: 'Colaborador',
      render: (ex) => <span className="font-semibold">{ex.colaboradorNome}</span>,
    },
    {
      header: 'Exame Ocupacional',
      render: (ex) => (
        <div>
          <div>{ex.tipoExameNome}</div>
          <div className="text-xs text-muted">Periodicidade: {ex.periodicidadeMeses} meses</div>
        </div>
      ),
    },
    {
      header: 'Realização / Vencimento',
      render: (ex) => (
        <div className="text-sm">
          <div>Realizado em: <strong>{formatDate(ex.dataRealizacao)}</strong></div>
          <div className="text-xs text-muted">Vencimento: {formatDate(ex.dataVencimento)}</div>
        </div>
      ),
    },
    {
      header: 'Status',
      render: (ex) => <FluxoExameBadge status={ex.statusFluxo} />,
    },
    {
      header: 'Ações do Médico',
      render: (ex) => (
        <div className="table-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleViewPdf('exame', ex.id)}
          >
            <FileText size={14} /> Visualizar PDF
          </button>
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={() => setSignModal({ open: true, tipo: 'exame', item: ex })}
          >
            <CheckCircle2 size={14} /> Assinar
          </button>
          <button
            type="button"
            className="btn btn-outline-danger btn-sm"
            onClick={() => openReject('exame', ex)}
          >
            <XCircle size={14} /> Recusar
          </button>
        </div>
      ),
    },
  ];

  const totalPendentes = laudos.length + exames.length;

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Fila de Assinaturas e Homologação Médica</h2>
          <p className="page-subtitle">
            Avalie os laudos técnicos e exames ocupacionais enviados, visualize os PDFs e registre sua assinatura médica (CRM {user?.crm || '—'}/{user?.crmUf || '—'}).
          </p>
        </div>
        <Badge variant={totalPendentes > 0 ? 'warning' : 'success'}>
          {totalPendentes} pendência(s) aguardando assinatura
        </Badge>
      </div>

      {loading ? (
        <div className="card">
          <Loading />
        </div>
      ) : totalPendentes === 0 ? (
        <div className="card">
          <EmptyState
            icon={ShieldCheck}
            title="Tudo em dia! Não há pendências de assinatura."
            description="Todos os laudos técnicos e resultados de exames enviados já foram analisados."
          />
        </div>
      ) : (
        <>
          {/* Lista 1: Laudos Aguardando Assinatura */}
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <div className="card-header">
              <div className="card-title-group">
                <FileCheck2 size={20} className="text-primary" />
                <h3>Laudos Técnicos Aguardando Assinatura ({laudos.length})</h3>
              </div>
            </div>
            <Table
              columns={laudoColumns}
              data={laudos}
              emptyTitle="Nenhum laudo técnico pendente de assinatura"
            />
          </div>

          {/* Lista 2: Exames Aguardando Assinatura */}
          <div className="card">
            <div className="card-header">
              <div className="card-title-group">
                <Stethoscope size={20} className="text-primary" />
                <h3>Exames Ocupacionais Aguardando Assinatura ({exames.length})</h3>
              </div>
            </div>
            <Table
              columns={exameColumns}
              data={exames}
              emptyTitle="Nenhum exame ocupacional pendente de assinatura"
            />
          </div>
        </>
      )}

      {/* Modal Confirmar Assinatura (com CRM do médico logado) */}
      <Modal
        isOpen={signModal.open}
        onClose={() => setSignModal({ open: false, tipo: null, item: null })}
        title="Confirmar Homologação e Assinatura Médica"
        size="md"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setSignModal({ open: false, tipo: null, item: null })}
              disabled={processing}
            >
              Cancelar
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleConfirmSign}
              disabled={processing}
            >
              {processing ? 'Assinando...' : 'Confirmar Assinatura (Homologar)'}
            </button>
          </>
        }
      >
        <div className="crm-confirmation-box">
          <p>
            Você está prestes a homologar o seguinte{' '}
            <strong>{signModal.tipo === 'laudo' ? 'Laudo Técnico' : 'Exame Ocupacional'}</strong>:
          </p>
          <div className="crm-doc-summary">
            {signModal.tipo === 'laudo' ? (
              <>
                <div>
                  <strong>Título:</strong> {signModal.item?.titulo} ({signModal.item?.tipoLaudoNome})
                </div>
                <div>
                  <strong>Data de Emissão:</strong> {formatDate(signModal.item?.dataEmissao)}
                </div>
              </>
            ) : (
              <>
                <div>
                  <strong>Colaborador:</strong> {signModal.item?.colaboradorNome}
                </div>
                <div>
                  <strong>Exame:</strong> {signModal.item?.tipoExameNome}
                </div>
                <div>
                  <strong>Realizado em:</strong> {formatDate(signModal.item?.dataRealizacao)}
                </div>
              </>
            )}
          </div>
          <div className="crm-stamp">
            <ShieldCheck size={20} className="text-primary" />
            <div>
              <div>
                <strong>Médico Responsável:</strong> {user?.nome}
              </div>
              <div>
                <strong>Registro Profissional:</strong> CRM {user?.crm || '—'}/{user?.crmUf || '—'}
              </div>
              <div className="text-xs text-muted">
                A data e hora exatas da assinatura serão gravadas na trilha de auditoria e o status passará para <strong>Homologado</strong>.
              </div>
            </div>
          </div>
        </div>
      </Modal>

      {/* Modal Recusar / Solicitar Revisão com Justificativa Obrigatória */}
      <Modal
        isOpen={rejectModal.open}
        onClose={() => setRejectModal({ open: false, tipo: null, item: null })}
        title="Recusar e Solicitar Revisão"
        size="md"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setRejectModal({ open: false, tipo: null, item: null })}
              disabled={processing}
            >
              Cancelar
            </button>
            <button
              type="submit"
              form="reject-form"
              className="btn btn-danger"
              disabled={processing}
            >
              {processing ? 'Registrando...' : 'Recusar (Revisado Pendente)'}
            </button>
          </>
        }
      >
        {rejectError && (
          <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
            <AlertCircle size={18} />
            <span>{rejectError}</span>
          </div>
        )}
        <p className="text-sm text-muted" style={{ marginBottom: '1rem' }}>
          O item retornará para a equipe responsável com o status <strong>Revisado Pendente</strong>. Descreva abaixo os ajustes necessários:
        </p>
        <form id="reject-form" onSubmit={handleConfirmReject}>
          <FormField label="Justificativa Técnica da Recusa" required>
            <textarea
              className="form-control"
              rows={4}
              placeholder="Informe detalhadamente o motivo da recusa ou correção solicitada..."
              value={justificativa}
              onChange={(e) => setJustificativa(e.target.value)}
              required
              autoFocus
            />
          </FormField>
        </form>
      </Modal>
    </div>
  );
}
