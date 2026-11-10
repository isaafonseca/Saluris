import React, { useState, useEffect, useCallback } from 'react';
import { Play, CheckCircle2, Filter, RefreshCw } from 'lucide-react';
import { api } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import { useEmpresa } from '../../context/EmpresaContext';
import { formatDateTime } from '../../utils/formatters';
import { Table } from '../../components/ui/Table';
import { Badge } from '../../components/ui/Badge';
import { Pagination } from '../../components/ui/Common';

export function AuditoriaPage() {
  const toast = useToast();
  const { empresas, reloadEmpresas } = useEmpresa();

  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filtroEmpresaId, setFiltroEmpresaId] = useState('');
  const [loading, setLoading] = useState(true);

  // Execução manual do monitoramento de prazos
  const [runningMonitor, setRunningMonitor] = useState(false);
  const [monitorResult, setMonitorResult] = useState(null);

  useEffect(() => {
    reloadEmpresas();
  }, [reloadEmpresas]);

  const loadAuditoria = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get('/auditoria', {
        empresaId: filtroEmpresaId || undefined,
        page,
        size: 20,
      });
      setLogs(res?.content || []);
      setTotalPages(res?.totalPages || 0);
      setTotalElements(res?.totalElements || 0);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar registros de auditoria.');
    } finally {
      setLoading(false);
    }
  }, [filtroEmpresaId, page, toast]);

  useEffect(() => {
    loadAuditoria();
  }, [loadAuditoria]);

  const handleExecutarMonitoramento = async () => {
    setRunningMonitor(true);
    try {
      const res = await api.post('/admin/monitoramento/executar');
      setMonitorResult(res);
      toast.success('Rotina de monitoramento de prazos executada com sucesso!');
      loadAuditoria();
    } catch (err) {
      toast.error(err.detail || 'Falha ao executar monitoramento.');
    } finally {
      setRunningMonitor(false);
    }
  };

  const columns = [
    {
      header: 'Data / Hora',
      width: '160px',
      render: (r) => <span className="text-xs mono">{formatDateTime(r.criadoEm)}</span>,
    },
    {
      header: 'Usuário',
      render: (r) => (
        <div>
          <div className="font-semibold">{r.usuarioNome || 'Sistema / Rotina'}</div>
          {r.ipOrigem && <div className="text-xs text-muted">IP: {r.ipOrigem}</div>}
        </div>
      ),
    },
    {
      header: 'Ação',
      render: (r) => <Badge variant="info">{r.acao}</Badge>,
    },
    {
      header: 'Entidade',
      render: (r) => (
        <span>
          {r.entidade} {r.entidadeId ? `#${r.entidadeId}` : ''}
        </span>
      ),
    },
    {
      header: 'Detalhes',
      render: (r) => <span className="text-sm">{r.detalhes || '—'}</span>,
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Auditoria & Monitoramento de Prazos</h2>
          <p className="page-subtitle">
            Consulte a trilha de auditoria das operações críticas e acione manualmente a rotina de verificação de vencimentos.
          </p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleExecutarMonitoramento}
            disabled={runningMonitor}
          >
            <Play size={17} />
            {runningMonitor ? 'Executando rotina...' : 'Executar monitoramento agora'}
          </button>
        </div>
      </div>

      {/* Resultado do Monitoramento */}
      {monitorResult && (
        <div className="monitor-result-card">
          <div className="monitor-result-header">
            <CheckCircle2 size={20} className="text-success" />
            <strong>Resultado da Execução do Monitoramento Diário</strong>
            <button
              type="button"
              className="btn-link-sm"
              style={{ marginLeft: 'auto' }}
              onClick={() => setMonitorResult(null)}
            >
              Ocultar
            </button>
          </div>
          <div className="stats-grid-4" style={{ marginTop: '0.75rem' }}>
            <div className="stat-mini-box">
              <span className="stat-mini-value">{monitorResult.examesAtualizados ?? 0}</span>
              <span className="stat-mini-label">Exames Atualizados</span>
            </div>
            <div className="stat-mini-box">
              <span className="stat-mini-value">{monitorResult.documentosAtualizados ?? 0}</span>
              <span className="stat-mini-label">Documentos Atualizados</span>
            </div>
            <div className="stat-mini-box">
              <span className="stat-mini-value">{monitorResult.notificacoesCriadas ?? 0}</span>
              <span className="stat-mini-label">Notificações Criadas</span>
            </div>
            <div className="stat-mini-box">
              <span className="stat-mini-value">{monitorResult.emailsEnfileirados ?? 0}</span>
              <span className="stat-mini-label">E-mails Enfileirados</span>
            </div>
          </div>
        </div>
      )}

      {/* Filtro */}
      <div className="card filter-card">
        <div className="filter-row">
          <div className="filter-item">
            <label className="form-label">
              <Filter size={14} /> Filtrar por Empresa
            </label>
            <select
              className="form-control"
              value={filtroEmpresaId}
              onChange={(e) => {
                setFiltroEmpresaId(e.target.value);
                setPage(0);
              }}
            >
              <option value="">Todas as empresas (incluindo ações globais)</option>
              {empresas.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  {emp.nomeFantasia || emp.razaoSocial}
                </option>
              ))}
            </select>
          </div>
          <div className="filter-actions">
            <button type="button" className="btn btn-secondary" onClick={loadAuditoria}>
              <RefreshCw size={16} /> Atualizar
            </button>
          </div>
        </div>
      </div>

      {/* Tabela paginada */}
      <div className="card">
        <Table
          columns={columns}
          data={logs}
          loading={loading}
          emptyTitle="Nenhum log de auditoria encontrado"
        />
        <Pagination
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={setPage}
        />
      </div>
    </div>
  );
}
