import React, { useState, useEffect, useCallback } from 'react';
import { Bell, CheckCheck, Check } from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { formatDateTime } from '../utils/formatters';
import { Badge } from '../components/ui/Badge';
import { Loading, EmptyState, Pagination } from '../components/ui/Common';

export function NotificacoesPage() {
  const { user } = useAuth();
  const isAdmin = user?.perfil === 'ADMINISTRADOR';
  const toast = useToast();

  const [notificacoes, setNotificacoes] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const loadNotificacoes = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get('/notificacoes', { page, size: 15 });
      setNotificacoes(res?.content || []);
      setTotalPages(res?.totalPages || 0);
      setTotalElements(res?.totalElements || 0);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar notificações.');
    } finally {
      setLoading(false);
    }
  }, [page, toast]);

  useEffect(() => {
    loadNotificacoes();
  }, [loadNotificacoes]);

  const handleMarkRead = async (id) => {
    try {
      await api.post(`/notificacoes/${id}/lida`);
      setNotificacoes((prev) => prev.map((n) => (n.id === id ? { ...n, lida: true } : n)));
      window.dispatchEvent(new Event('saluris:notificacoes-atualizadas'));
    } catch (err) {
      toast.error(err.detail || 'Não foi possível marcar como lida.');
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await api.post('/notificacoes/marcar-todas-lidas');
      setNotificacoes((prev) => prev.map((n) => ({ ...n, lida: true })));
      window.dispatchEvent(new Event('saluris:notificacoes-atualizadas'));
      toast.success('Todas as notificações foram marcadas como lidas.');
    } catch (err) {
      toast.error(err.detail || 'Erro ao marcar todas como lidas.');
    }
  };

  const getTipoVariant = (tipo) => {
    if (!tipo) return 'neutral';
    if (tipo === 'URGENCIA' || tipo.includes('REVISADO')) return 'danger';
    if (tipo === 'PREVENTIVO' || tipo.includes('PENDENTE') || tipo.includes('NAO_CONFIGURADA')) {
      return 'warning';
    }
    if (tipo.includes('HOMOLOGADO')) return 'success';
    return 'info';
  };

  const getTipoLabel = (tipo) => {
    if (!isAdmin) return tipo;
    if (tipo === 'URGENCIA') return 'Problema';
    if (tipo === 'PREVENTIVO') return 'Configuração';
    return tipo;
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>
            {isAdmin
              ? 'Notificações de Configuração e Problemas do Sistema'
              : 'Central de Notificações e Alertas'}
          </h2>
          <p className="page-subtitle">
            {isAdmin
              ? 'Acompanhe alertas sobre configurações globais, integrações (SMTP/IA) e possíveis problemas nas contas ou envios.'
              : 'Acompanhe alertas preventivos e de urgência sobre vencimentos de exames, documentos e fila de assinaturas.'}
          </p>
        </div>
        <button type="button" className="btn btn-secondary" onClick={handleMarkAllRead}>
          <CheckCheck size={17} /> Marcar todas como lidas
        </button>
      </div>

      <div className="card">
        {loading ? (
          <Loading />
        ) : notificacoes.length === 0 ? (
          <EmptyState
            icon={Bell}
            title={isAdmin ? 'Nenhum problema de configuração' : 'Nenhuma notificação registrada'}
            description={
              isAdmin
                ? 'As configurações globais, contas e envios do sistema não apresentam pendências críticas no momento.'
                : 'Quando houver novos alertas de vencimento ou atualizações de fluxo, eles aparecerão aqui.'
            }
          />
        ) : (
          <div className="notif-page-list">
            {notificacoes.map((n) => (
              <div
                key={n.id}
                className={`notif-page-item ${!n.lida ? 'unread' : ''}`}
              >
                <div className="notif-page-main">
                  <div className="notif-page-header">
                    <Badge variant={getTipoVariant(n.tipo)}>{getTipoLabel(n.tipo)}</Badge>
                    {n.marcoDias !== null && n.marcoDias !== undefined && (
                      <Badge variant="neutral">Marco: {n.marcoDias} dias</Badge>
                    )}
                    <strong className="notif-page-title">{n.titulo}</strong>
                    {!n.lida && <Badge variant="info">Nova</Badge>}
                  </div>
                  <p className="notif-page-msg">{n.mensagem}</p>
                  <span className="notif-page-date">{formatDateTime(n.criadoEm)}</span>
                </div>
                {!n.lida && (
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => handleMarkRead(n.id)}
                  >
                    <Check size={14} /> Marcar como lida
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
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
