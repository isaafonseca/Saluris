import React, { useState, useEffect, useCallback } from 'react';
import {
  CheckCircle2,
  Clock,
  AlertTriangle,
  Percent,
  Download,
  FileText,
  Filter,
  XCircle,
} from 'lucide-react';
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
} from 'recharts';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useEmpresa } from '../context/EmpresaContext';
import { useToast } from '../context/ToastContext';
import { formatDate } from '../utils/formatters';
import { AdminEmpresaSelector } from '../components/ui/AdminEmpresaSelector';
import { Table } from '../components/ui/Table';
import { Badge, ValidadeBadge, FluxoExameBadge } from '../components/ui/Badge';
import { Loading, EmptyState } from '../components/ui/Common';

const CHART_COLORS = {
  EM_DIA: '#16a34a',
  PROXIMO: '#d97706',
  VENCIDO: '#dc2626',
};

export function DashboardPage() {
  const { isAdmin } = useAuth();
  const { activeEmpresaId } = useEmpresa();
  const toast = useToast();

  const [dash, setDash] = useState(null);
  const [loading, setLoading] = useState(false);

  // Filtros editáveis na barra de filtro do Dashboard
  const [dataInicio, setDataInicio] = useState('');
  const [dataFim, setDataFim] = useState('');
  const [tipoExameId, setTipoExameId] = useState('');
  const [colaboradorId, setColaboradorId] = useState('');
  const [statusValidade, setStatusValidade] = useState('');

  // Filtros efetivamente aplicados pelo botão "Aplicar Filtro"
  const [appliedFilters, setAppliedFilters] = useState({
    dataInicio: '',
    dataFim: '',
    tipoExameId: '',
    colaboradorId: '',
    statusValidade: '',
  });

  const [tiposExame, setTiposExame] = useState([]);
  const [colaboradores, setColaboradores] = useState([]);

  // Geração de Relatório
  const [relFormato, setRelFormato] = useState('PDF');
  const [relTipo, setRelTipo] = useState('AMBOS');
  const [relInicio, setRelInicio] = useState('');
  const [relFim, setRelFim] = useState('');
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    api
      .get('/tipos-exame', { ativos: true })
      .then((res) => setTiposExame(Array.isArray(res) ? res : []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    api
      .get('/colaboradores', {
        empresaId: isAdmin && activeEmpresaId ? activeEmpresaId : undefined,
        page: 0,
        size: 200,
      })
      .then((res) => setColaboradores(res?.content || []))
      .catch(() => setColaboradores([]));
  }, [isAdmin, activeEmpresaId]);

  const loadDashboard = useCallback(
    async (filtros = appliedFilters) => {
      setLoading(true);
      try {
        const res = await api.get('/dashboard', {
          empresaId: isAdmin && activeEmpresaId ? activeEmpresaId : undefined,
          dataInicio: filtros?.dataInicio || undefined,
          dataFim: filtros?.dataFim || undefined,
          tipoExameId: filtros?.tipoExameId || undefined,
          colaboradorId: filtros?.colaboradorId || undefined,
          statusValidade: filtros?.statusValidade || undefined,
        });
        setDash(res);
      } catch (err) {
        toast.error(err.detail || 'Erro ao carregar indicadores do dashboard.');
      } finally {
        setLoading(false);
      }
    },
    [isAdmin, activeEmpresaId, appliedFilters, toast]
  );

  useEffect(() => {
    loadDashboard(appliedFilters);
  }, [loadDashboard, appliedFilters]);

  const handleAplicarFiltro = (e) => {
    if (e) e.preventDefault();
    let inicio = dataInicio;
    let fim = dataFim;
    if (inicio && fim && inicio > fim) {
      const temp = inicio;
      inicio = fim;
      fim = temp;
      setDataInicio(inicio);
      setDataFim(fim);
    }
    const novosFiltros = {
      dataInicio: inicio,
      dataFim: fim,
      tipoExameId,
      colaboradorId,
      statusValidade,
    };
    setAppliedFilters(novosFiltros);
    if (inicio && !relInicio) setRelInicio(inicio);
    if (fim && !relFim) setRelFim(fim);
  };

  const handleLimparFiltros = () => {
    const limpos = {
      dataInicio: '',
      dataFim: '',
      tipoExameId: '',
      colaboradorId: '',
      statusValidade: '',
    };
    setDataInicio('');
    setDataFim('');
    setTipoExameId('');
    setColaboradorId('');
    setStatusValidade('');
    setRelInicio('');
    setRelFim('');
    setAppliedFilters(limpos);
  };

  const handleGerarRelatorio = async (e) => {
    e.preventDefault();
    setDownloading(true);
    try {
      const ext = relFormato.toLowerCase();
      await api.downloadFile(
        '/relatorios/conformidade',
        {
          formato: relFormato,
          tipo: relTipo,
          empresaId: isAdmin && activeEmpresaId ? activeEmpresaId : undefined,
          dataInicio: relInicio || appliedFilters.dataInicio || dataInicio || undefined,
          dataFim: relFim || appliedFilters.dataFim || dataFim || undefined,
          tipoExameId: appliedFilters.tipoExameId || tipoExameId || undefined,
          colaboradorId: appliedFilters.colaboradorId || colaboradorId || undefined,
          statusValidade: appliedFilters.statusValidade || statusValidade || undefined,
        },
        `relatorio-conformidade.${ext}`
      );
      toast.success(`Relatório (${relFormato}) gerado com sucesso!`);
    } catch (err) {
      toast.error(err.detail || 'Não há registros para o filtro selecionado ou erro na geração.');
    } finally {
      setDownloading(false);
    }
  };

  const examesStats = dash?.exames || {
    total: 0,
    emDia: 0,
    proximoVencimento: 0,
    vencido: 0,
    percentualConformidade: 100,
  };
  const docsStats = dash?.documentos || {
    total: 0,
    emDia: 0,
    proximoVencimento: 0,
    vencido: 0,
  };
  const laudosStats = dash?.laudos || {
    aguardandoAssinatura: 0,
    homologados: 0,
    revisadoPendente: 0,
  };

  const pieData = [
    { name: 'Em dia', value: examesStats.emDia, color: CHART_COLORS.EM_DIA },
    {
      name: 'Próximo do Vencimento',
      value: examesStats.proximoVencimento,
      color: CHART_COLORS.PROXIMO,
    },
    { name: 'Vencido', value: examesStats.vencido, color: CHART_COLORS.VENCIDO },
  ];

  const barData = [
    {
      categoria: 'Exames Ocupacionais',
      'Em dia': examesStats.emDia,
      'Próximo do Vencimento': examesStats.proximoVencimento,
      Vencido: examesStats.vencido,
    },
    {
      categoria: 'Documentos Vigentes',
      'Em dia': docsStats.emDia,
      'Próximo do Vencimento': docsStats.proximoVencimento,
      Vencido: docsStats.vencido,
    },
  ];

  const criticosColumns = [
    {
      header: 'Colaborador',
      render: (ex) => <span className="font-semibold">{ex.colaboradorNome}</span>,
    },
    {
      header: 'Tipo de Exame',
      render: (ex) => ex.tipoExameNome,
    },
    {
      header: 'Vencimento',
      render: (ex) => (
        <div>
          <span className="font-semibold">{formatDate(ex.dataVencimento)}</span>
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
      header: 'Fluxo',
      render: (ex) => <FluxoExameBadge status={ex.statusFluxo} />,
    },
  ];

  const docsVigentesColumns = [
    {
      header: 'Programa / Sigla',
      render: (d) => (
        <div className="flex-gap-sm">
          <Badge variant="info">{d.tipoDocumentoSigla || d.tipoDocumentoNome}</Badge>
          <span className="text-xs text-muted">v{d.versao}</span>
        </div>
      ),
    },
    {
      header: 'Título',
      render: (d) => <span className="font-semibold">{d.titulo}</span>,
    },
    {
      header: 'Emissão / Validade',
      render: (d) => `${formatDate(d.dataEmissao)} até ${formatDate(d.dataValidade)}`,
    },
    {
      header: 'Status de Validade',
      render: (d) => <ValidadeBadge status={d.statusValidade} />,
    },
    {
      header: 'Arquivo',
      render: (d) => (
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() =>
            api
              .openPdfInNewTab(`/documentos/${d.id}/arquivo`)
              .catch((err) => toast.error(err.detail || 'Erro ao abrir PDF.'))
          }
        >
          <FileText size={14} /> PDF
        </button>
      ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Dashboard de Conformidade & Relatórios SST</h2>
          <p className="page-subtitle">
            Visão consolidada da situação dos exames ocupacionais, programas vigentes e homologação de laudos técnicos.
          </p>
        </div>
      </div>

      <AdminEmpresaSelector />

      {/* Filtros do Dashboard */}
      <div className="card filter-card">
        <form onSubmit={handleAplicarFiltro} className="filter-row">
          <div className="filter-item">
            <label className="form-label">
              <Filter size={14} /> Vencimento Início
            </label>
            <input
              type="date"
              className="form-control"
              value={dataInicio}
              onChange={(e) => setDataInicio(e.target.value)}
            />
          </div>

          <div className="filter-item">
            <label className="form-label">Vencimento Fim</label>
            <input
              type="date"
              className="form-control"
              value={dataFim}
              onChange={(e) => setDataFim(e.target.value)}
            />
          </div>

          <div className="filter-item">
            <label className="form-label">Tipo de Exame</label>
            <select
              className="form-control"
              value={tipoExameId}
              onChange={(e) => setTipoExameId(e.target.value)}
            >
              <option value="">Todos os tipos</option>
              {tiposExame.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.nome}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-item">
            <label className="form-label">Colaborador</label>
            <select
              className="form-control"
              value={colaboradorId}
              onChange={(e) => setColaboradorId(e.target.value)}
            >
              <option value="">Todos os colaboradores</option>
              {colaboradores.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nome}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-item">
            <label className="form-label">Situação / Validade</label>
            <select
              className="form-control"
              value={statusValidade}
              onChange={(e) => setStatusValidade(e.target.value)}
            >
              <option value="">Todas as situações</option>
              <option value="EM_DIA">Em dia</option>
              <option value="PROXIMO_VENCIMENTO">Próximo do Vencimento</option>
              <option value="VENCIDO">Vencido</option>
            </select>
          </div>

          <div className="filter-actions" style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-end' }}>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              <Filter size={16} /> Aplicar Filtro
            </button>

            {(dataInicio ||
              dataFim ||
              tipoExameId ||
              colaboradorId ||
              statusValidade ||
              appliedFilters.dataInicio ||
              appliedFilters.dataFim ||
              appliedFilters.tipoExameId ||
              appliedFilters.colaboradorId ||
              appliedFilters.statusValidade) && (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={handleLimparFiltros}
                disabled={loading}
              >
                <XCircle size={16} /> Limpar Filtros
              </button>
            )}
          </div>
        </form>
      </div>

      {loading ? (
        <div className="card">
          <Loading />
        </div>
      ) : (
        <>
          {/* Cards Principais de Conformidade */}
          <div className="kpi-grid">
            <div className="kpi-card kpi-success">
              <div className="kpi-icon">
                <CheckCircle2 size={24} />
              </div>
              <div className="kpi-info">
                <span className="kpi-label">Exames Em Dia</span>
                <strong className="kpi-value">{examesStats.emDia}</strong>
                <span className="kpi-sub">de {examesStats.total} exames monitorados</span>
              </div>
            </div>

            <div className="kpi-card kpi-warning">
              <div className="kpi-icon">
                <Clock size={24} />
              </div>
              <div className="kpi-info">
                <span className="kpi-label">Próximos do Vencimento</span>
                <strong className="kpi-value">{examesStats.proximoVencimento}</strong>
                <span className="kpi-sub">Alerta preventivo (≤ 60 dias)</span>
              </div>
            </div>

            <div className="kpi-card kpi-danger">
              <div className="kpi-icon">
                <AlertTriangle size={24} />
              </div>
              <div className="kpi-info">
                <span className="kpi-label">Exames Vencidos</span>
                <strong className="kpi-value">{examesStats.vencido}</strong>
                <span className="kpi-sub">Exigem regularização imediata</span>
              </div>
            </div>

            <div className="kpi-card kpi-primary">
              <div className="kpi-icon">
                <Percent size={24} />
              </div>
              <div className="kpi-info">
                <span className="kpi-label">Índice de Conformidade</span>
                <strong className="kpi-value">
                  {Number(examesStats.percentualConformidade ?? 100).toFixed(1)}%
                </strong>
                <span className="kpi-sub">Taxa geral de regularidade</span>
              </div>
            </div>
          </div>

          {/* Contadores de Laudos Técnicos */}
          <div className="card" style={{ marginBottom: '1.25rem' }}>
            <div className="card-header">
              <h3>Resumo de Laudos Técnicos e Homologação Médica</h3>
            </div>
            <div className="card-body stats-grid-3">
              <div className="stat-mini-box info">
                <span className="stat-mini-value">{laudosStats.aguardandoAssinatura}</span>
                <span className="stat-mini-label">Aguardando Assinatura Médica</span>
              </div>
              <div className="stat-mini-box success">
                <span className="stat-mini-value">{laudosStats.homologados}</span>
                <span className="stat-mini-label">Laudos Homologados</span>
              </div>
              <div className="stat-mini-box orange">
                <span className="stat-mini-value">{laudosStats.revisadoPendente}</span>
                <span className="stat-mini-label">Revisado Pendente (Ajuste Solicitado)</span>
              </div>
            </div>
          </div>

          {/* Gráficos Recharts */}
          <div className="charts-grid">
            <div className="card">
              <div className="card-header">
                <h3>Distribuição de Exames por Status</h3>
              </div>
              <div className="card-body chart-box">
                {examesStats.total === 0 ? (
                  <EmptyState
                    title="Sem exames registrados"
                    description="Cadastre colaboradores e regras na matriz para visualizar o gráfico."
                  />
                ) : (
                  <ResponsiveContainer width="100%" height={260}>
                    <PieChart>
                      <Pie
                        data={pieData}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        innerRadius={55}
                        outerRadius={90}
                        paddingAngle={3}
                      >
                        {pieData.map((entry, index) => (
                          <Cell key={index} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </div>
            </div>

            <div className="card">
              <div className="card-header">
                <h3>Comparativo: Exames x Documentos Vigentes</h3>
              </div>
              <div className="card-body chart-box">
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={barData}>
                    <XAxis dataKey="categoria" />
                    <YAxis allowDecimals={false} />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="Em dia" fill={CHART_COLORS.EM_DIA} radius={[4, 4, 0, 0]} />
                    <Bar
                      dataKey="Próximo do Vencimento"
                      fill={CHART_COLORS.PROXIMO}
                      radius={[4, 4, 0, 0]}
                    />
                    <Bar dataKey="Vencido" fill={CHART_COLORS.VENCIDO} radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>

          {/* Tabelas: Exames Críticos e Documentos Vigentes */}
          <div className="card" style={{ marginBottom: '1.25rem' }}>
            <div className="card-header">
              <h3>
                Exames Críticos — Vencidos e Próximos do Vencimento (
                {dash?.examesCriticos?.length || 0})
              </h3>
            </div>
            <Table
              columns={criticosColumns}
              data={dash?.examesCriticos || []}
              emptyTitle="Nenhum exame crítico no momento"
              emptyDescription="Todos os exames filtrados estão em dia."
            />
          </div>

          <div className="card" style={{ marginBottom: '1.25rem' }}>
            <div className="card-header">
              <h3>Documentos SST Vigentes ({dash?.documentosVigentes?.length || 0})</h3>
            </div>
            <Table
              columns={docsVigentesColumns}
              data={dash?.documentosVigentes || []}
              emptyTitle="Nenhum documento vigente cadastrado"
            />
          </div>

          {/* Seção Gerar Relatório */}
          <div className="card">
            <div className="card-header">
              <div className="card-title-group">
                <Download size={20} className="text-primary" />
                <div>
                  <h3>Gerar Relatório de Conformidade</h3>
                  <p className="text-muted text-xs">
                    Exporte a relação detalhada de conformidade ocupacional em formato PDF ou CSV.
                  </p>
                </div>
              </div>
            </div>
            <div className="card-body">
              <form onSubmit={handleGerarRelatorio} className="filter-row">
                <div className="filter-item">
                  <label className="form-label">Formato do Arquivo</label>
                  <select
                    className="form-control"
                    value={relFormato}
                    onChange={(e) => setRelFormato(e.target.value)}
                  >
                    <option value="PDF">PDF (Documento formatado)</option>
                    <option value="CSV">CSV (Planilha de dados)</option>
                  </select>
                </div>

                <div className="filter-item">
                  <label className="form-label">Escopo do Relatório</label>
                  <select
                    className="form-control"
                    value={relTipo}
                    onChange={(e) => setRelTipo(e.target.value)}
                  >
                    <option value="AMBOS">Exames e Documentos SST (Ambos)</option>
                    <option value="EXAMES">Somente Exames Ocupacionais</option>
                    <option value="DOCUMENTOS">Somente Documentos SST</option>
                  </select>
                </div>

                <div className="filter-item">
                  <label className="form-label">Período Início (opcional)</label>
                  <input
                    type="date"
                    className="form-control"
                    value={relInicio}
                    onChange={(e) => setRelInicio(e.target.value)}
                  />
                </div>

                <div className="filter-item">
                  <label className="form-label">Período Fim (opcional)</label>
                  <input
                    type="date"
                    className="form-control"
                    value={relFim}
                    onChange={(e) => setRelFim(e.target.value)}
                  />
                </div>

                <div className="filter-actions">
                  <button type="submit" className="btn btn-primary" disabled={downloading}>
                    <Download size={16} />
                    {downloading ? 'Gerando...' : `Baixar ${relFormato}`}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
