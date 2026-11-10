import React, { useState, useEffect, useCallback, useRef } from 'react';
import { NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  ShieldCheck,
  LayoutDashboard,
  Users,
  Building2,
  BookOpen,
  Network,
  CalendarClock,
  UserCheck,
  Stethoscope,
  FileText,
  FileCheck2,
  PenTool,
  Bot,
  Bell,
  History,
  LogOut,
  Menu,
  X,
  CheckCheck,
  KeyRound,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useEmpresa } from '../context/EmpresaContext';
import { api } from '../api/client';
import { PERFIL_LABELS, formatDateTime } from '../utils/formatters';

export function AppLayout() {
  const { user, logout } = useAuth();
  const { setSelectedEmpresaId } = useEmpresa();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // Notificações no sino (polling a cada 60s)
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifOpen, setNotifOpen] = useState(false);
  const [recentNotifs, setRecentNotifs] = useState([]);
  const [loadingNotifs, setLoadingNotifs] = useState(false);
  const notifRef = useRef(null);

  const fetchUnreadCount = useCallback(async () => {
    if (!user || user.deveTrocarSenha) return;
    try {
      const res = await api.get('/notificacoes/nao-lidas/contagem');
      setUnreadCount(res?.total ?? 0);
    } catch (_) {
      // Silencioso para polling
    }
  }, [user]);

  useEffect(() => {
    fetchUnreadCount();
    const timer = setInterval(fetchUnreadCount, 60000);
    return () => clearInterval(timer);
  }, [fetchUnreadCount]);

  // Atualiza contagem e limpa filtro de empresa ao trocar de página
  useEffect(() => {
    setSelectedEmpresaId('');
    fetchUnreadCount();
    setSidebarOpen(false);
    setNotifOpen(false);
  }, [location.pathname, fetchUnreadCount, setSelectedEmpresaId]);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (notifRef.current && !notifRef.current.contains(e.target)) {
        setNotifOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const toggleNotifDropdown = async () => {
    const next = !notifOpen;
    setNotifOpen(next);
    if (next) {
      setLoadingNotifs(true);
      try {
        const res = await api.get('/notificacoes', { page: 0, size: 6 });
        setRecentNotifs(res?.content || []);
      } catch (_) {
        setRecentNotifs([]);
      } finally {
        setLoadingNotifs(false);
      }
    }
  };

  const handleMarkAsRead = async (id) => {
    try {
      await api.post(`/notificacoes/${id}/lida`);
      setRecentNotifs((prev) => prev.map((n) => (n.id === id ? { ...n, lida: true } : n)));
      fetchUnreadCount();
    } catch (_) {}
  };

  const handleMarkAllRead = async () => {
    try {
      await api.post('/notificacoes/marcar-todas-lidas');
      setRecentNotifs((prev) => prev.map((n) => ({ ...n, lida: true })));
      setUnreadCount(0);
    } catch (_) {}
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const perfil = user?.perfil;

  const menuSections = [
    {
      title: 'Gestão e Operação',
      items: [
        {
          to: '/dashboard',
          label: 'Dashboard & Relatórios',
          icon: LayoutDashboard,
          roles: ['ESPECIALISTA_SST', 'RH_EMPRESA', 'ADMINISTRADOR'],
        },
        {
          to: '/assinaturas',
          label: 'Fila de Assinaturas',
          icon: PenTool,
          roles: ['MEDICO_TRABALHO'],
        },
        {
          to: '/estrutura',
          label: 'Estrutura da Empresa',
          icon: Network,
          roles: ['ESPECIALISTA_SST', 'RH_EMPRESA', 'ADMINISTRADOR'],
        },
        {
          to: '/matriz-exames',
          label: 'Matriz de Periodicidade',
          icon: CalendarClock,
          roles: ['ESPECIALISTA_SST', 'RH_EMPRESA', 'ADMINISTRADOR'],
        },
        {
          to: '/colaboradores',
          label: 'Colaboradores',
          icon: UserCheck,
          roles: ['RH_EMPRESA', 'ESPECIALISTA_SST', 'ADMINISTRADOR'],
        },
        {
          to: '/exames',
          label: 'Exames Ocupacionais',
          icon: Stethoscope,
          roles: ['RH_EMPRESA', 'ESPECIALISTA_SST', 'ADMINISTRADOR'],
        },
        {
          to: '/documentos',
          label: 'Documentos SST',
          icon: FileText,
          roles: ['ESPECIALISTA_SST', 'RH_EMPRESA', 'ADMINISTRADOR'],
        },
        {
          to: '/laudos',
          label: 'Laudos Técnicos',
          icon: FileCheck2,
          roles: ['ESPECIALISTA_SST', 'MEDICO_TRABALHO', 'RH_EMPRESA', 'ADMINISTRADOR'],
        },
        {
          to: '/ia',
          label: 'Assistente Normativo IA',
          icon: Bot,
          roles: ['ESPECIALISTA_SST', 'RH_EMPRESA'],
        },
      ],
    },
    {
      title: 'Administração (T.I.)',
      items: [
        {
          to: '/usuarios',
          label: 'Usuários',
          icon: Users,
          roles: ['ADMINISTRADOR'],
        },
        {
          to: '/empresas',
          label: 'Empresas',
          icon: Building2,
          roles: ['ADMINISTRADOR'],
        },
        {
          to: '/catalogos',
          label: 'Catálogos de SST',
          icon: BookOpen,
          roles: ['ADMINISTRADOR'],
        },
        {
          to: '/auditoria',
          label: 'Auditoria & Rotina',
          icon: History,
          roles: ['ADMINISTRADOR'],
        },
      ],
    },
    {
      title: 'Conta',
      items: [
        {
          to: '/notificacoes',
          label: 'Notificações',
          icon: Bell,
          roles: ['ADMINISTRADOR', 'ESPECIALISTA_SST', 'MEDICO_TRABALHO', 'RH_EMPRESA'],
          badge: unreadCount > 0 ? unreadCount : null,
        },
        {
          to: '/trocar-senha',
          label: 'Alterar Senha',
          icon: KeyRound,
          roles: ['ADMINISTRADOR', 'ESPECIALISTA_SST', 'MEDICO_TRABALHO', 'RH_EMPRESA'],
        },
      ],
    },
  ];

  return (
    <div className="app-shell">
      {/* Sidebar */}
      <aside className={`sidebar ${sidebarOpen ? 'open' : ''}`}>
        <div className="sidebar-brand">
          <div className="brand-logo">
            <ShieldCheck size={24} />
          </div>
          <div className="brand-text">
            <span className="brand-name">Saluris IA</span>
            <span className="brand-sub">Gestão de SST</span>
          </div>
          <button
            type="button"
            className="sidebar-close-btn"
            onClick={() => setSidebarOpen(false)}
            aria-label="Fechar menu"
          >
            <X size={20} />
          </button>
        </div>

        <nav className="sidebar-nav">
          {menuSections.map((section) => {
            const visibleItems = section.items.filter((item) => item.roles.includes(perfil));
            if (visibleItems.length === 0) return null;
            return (
              <div key={section.title} className="nav-section">
                <div className="nav-section-title">{section.title}</div>
                {visibleItems.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                    >
                      <Icon size={18} />
                      <span className="nav-link-text">{item.label}</span>
                      {item.badge && <span className="nav-badge">{item.badge}</span>}
                    </NavLink>
                  );
                })}
              </div>
            );
          })}
        </nav>

        <div className="sidebar-footer">
          <button type="button" className="btn-logout-sidebar" onClick={handleLogout}>
            <LogOut size={18} />
            <span>Sair do sistema</span>
          </button>
        </div>
      </aside>

      {sidebarOpen && <div className="sidebar-overlay" onClick={() => setSidebarOpen(false)} />}

      {/* Main Content */}
      <div className="main-wrapper">
        <header className="topbar">
          <div className="topbar-left">
            <button
              type="button"
              className="menu-toggle-btn"
              onClick={() => setSidebarOpen(true)}
              aria-label="Abrir menu"
            >
              <Menu size={22} />
            </button>
            <div className="topbar-company">
              {user?.empresaNome ? (
                <span className="company-pill">
                  <Building2 size={15} />
                  {user.empresaNome}
                </span>
              ) : (
                <span className="company-pill admin-pill">Administração Global (T.I.)</span>
              )}
            </div>
          </div>

          <div className="topbar-right">
            {/* Sino de Notificações */}
            <div className="notif-wrapper" ref={notifRef}>
              <button
                type="button"
                className="notif-bell-btn"
                onClick={toggleNotifDropdown}
                title="Notificações"
              >
                <Bell size={20} />
                {unreadCount > 0 && (
                  <span className="notif-counter">{unreadCount > 99 ? '99+' : unreadCount}</span>
                )}
              </button>

              {notifOpen && (
                <div className="notif-dropdown">
                  <div className="notif-dropdown-header">
                    <strong>Notificações</strong>
                    {unreadCount > 0 && (
                      <button
                        type="button"
                        className="btn-link-sm"
                        onClick={handleMarkAllRead}
                      >
                        <CheckCheck size={14} /> Marcar todas como lidas
                      </button>
                    )}
                  </div>
                  <div className="notif-dropdown-body">
                    {loadingNotifs ? (
                      <div className="notif-empty">Carregando...</div>
                    ) : recentNotifs.length === 0 ? (
                      <div className="notif-empty">Nenhuma notificação recente.</div>
                    ) : (
                      recentNotifs.map((n) => (
                        <div
                          key={n.id}
                          className={`notif-item ${!n.lida ? 'unread' : ''}`}
                          onClick={() => !n.lida && handleMarkAsRead(n.id)}
                        >
                          <div className="notif-item-top">
                            <span className="notif-item-title">{n.titulo}</span>
                            {!n.lida && <span className="notif-dot" title="Não lida" />}
                          </div>
                          <p className="notif-item-msg">{n.mensagem}</p>
                          <span className="notif-item-time">{formatDateTime(n.criadoEm)}</span>
                        </div>
                      ))
                    )}
                  </div>
                  <div className="notif-dropdown-footer">
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm btn-block"
                      onClick={() => {
                        setNotifOpen(false);
                        navigate('/notificacoes');
                      }}
                    >
                      Ver todas as notificações
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* Info do Usuário */}
            <div className="user-profile-box">
              <div className="user-avatar">
                {user?.nome ? user.nome.charAt(0).toUpperCase() : 'U'}
              </div>
              <div className="user-details">
                <span className="user-name">{user?.nome}</span>
                <span className="user-role">
                  {PERFIL_LABELS[user?.perfil] || user?.perfil}
                  {user?.crm ? ` • CRM ${user.crm}/${user.crmUf}` : ''}
                </span>
              </div>
            </div>
          </div>
        </header>

        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
