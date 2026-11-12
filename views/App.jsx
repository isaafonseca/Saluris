import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { getHomeRouteForPerfil } from './utils/formatters';
import { Loading } from './components/ui/Common';
import { AppLayout } from './components/AppLayout';

import { LoginPage } from './pages/LoginPage';
import { TrocarSenhaPage } from './pages/TrocarSenhaPage';
import { UsuariosPage } from './pages/admin/UsuariosPage';
import { EmpresasPage } from './pages/admin/EmpresasPage';
import { CatalogosPage } from './pages/admin/CatalogosPage';
import { EstruturaPage } from './pages/EstruturaPage';
import { MatrizExamesPage } from './pages/MatrizExamesPage';
import { ColaboradoresPage } from './pages/ColaboradoresPage';
import { ColaboradorDetalhePage } from './pages/ColaboradorDetalhePage';
import { ExamesPage } from './pages/ExamesPage';
import { DocumentosPage } from './pages/DocumentosPage';
import { LaudosPage } from './pages/LaudosPage';
import { AssinaturasPage } from './pages/AssinaturasPage';
import { DashboardPage } from './pages/DashboardPage';
import { AgenteIAPage } from './pages/AgenteIAPage';
import { NotificacoesPage } from './pages/NotificacoesPage';

function ProtectedRoute({ children, allowedRoles, allowDuringPasswordChange = false }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="full-page-loading">
        <Loading text="Verificando sessão..." />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (user.deveTrocarSenha && !allowDuringPasswordChange) {
    return <Navigate to="/trocar-senha" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.perfil)) {
    return <Navigate to={getHomeRouteForPerfil(user.perfil)} replace />;
  }

  return children;
}

function HomeRedirect() {
  const { user } = useAuth();
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return <Navigate to={getHomeRouteForPerfil(user.perfil)} replace />;
}

export default function App() {
  const { user } = useAuth();

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        {/* Se deveTrocarSenha for true, renderiza TrocarSenhaPage isolada sem sidebar; caso contrário, dentro do layout */}
        <Route
          path="/trocar-senha"
          element={
            <ProtectedRoute allowDuringPasswordChange>
              {user?.deveTrocarSenha ? <TrocarSenhaPage /> : <AppLayout />}
            </ProtectedRoute>
          }
        >
          {!user?.deveTrocarSenha && <Route index element={<TrocarSenhaPage />} />}
        </Route>

        {/* Rotas autenticadas dentro do AppLayout */}
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<HomeRedirect />} />

          {/* Admin T.I. e Conta Principal da Empresa */}
          <Route
            path="/usuarios"
            element={
              <ProtectedRoute allowedRoles={['ADMINISTRADOR', 'ADMIN_EMPRESA']}>
                <UsuariosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/empresas"
            element={
              <ProtectedRoute allowedRoles={['ADMINISTRADOR']}>
                <EmpresasPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/catalogos"
            element={
              <ProtectedRoute allowedRoles={['ADMINISTRADOR']}>
                <CatalogosPage />
              </ProtectedRoute>
            }
          />

          {/* Dashboard */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute allowedRoles={['ESPECIALISTA_SST', 'RH_EMPRESA']}>
                <DashboardPage />
              </ProtectedRoute>
            }
          />

          {/* Estrutura */}
          <Route
            path="/estrutura"
            element={
              <ProtectedRoute allowedRoles={['ESPECIALISTA_SST', 'RH_EMPRESA']}>
                <EstruturaPage />
              </ProtectedRoute>
            }
          />

          {/* Matriz de Exames */}
          <Route
            path="/matriz-exames"
            element={
              <ProtectedRoute allowedRoles={['ESPECIALISTA_SST', 'RH_EMPRESA']}>
                <MatrizExamesPage />
              </ProtectedRoute>
            }
          />

          {/* Colaboradores */}
          <Route
            path="/colaboradores"
            element={
              <ProtectedRoute allowedRoles={['RH_EMPRESA', 'ESPECIALISTA_SST']}>
                <ColaboradoresPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/colaboradores/:id"
            element={
              <ProtectedRoute allowedRoles={['RH_EMPRESA', 'ESPECIALISTA_SST']}>
                <ColaboradorDetalhePage />
              </ProtectedRoute>
            }
          />

          {/* Exames */}
          <Route
            path="/exames"
            element={
              <ProtectedRoute allowedRoles={['RH_EMPRESA', 'ESPECIALISTA_SST']}>
                <ExamesPage />
              </ProtectedRoute>
            }
          />

          {/* Documentos SST */}
          <Route
            path="/documentos"
            element={
              <ProtectedRoute allowedRoles={['ESPECIALISTA_SST', 'RH_EMPRESA']}>
                <DocumentosPage />
              </ProtectedRoute>
            }
          />

          {/* Laudos */}
          <Route
            path="/laudos"
            element={
              <ProtectedRoute allowedRoles={['ESPECIALISTA_SST', 'MEDICO_TRABALHO', 'RH_EMPRESA']}>
                <LaudosPage />
              </ProtectedRoute>
            }
          />

          {/* Assinaturas (Médico) */}
          <Route
            path="/assinaturas"
            element={
              <ProtectedRoute allowedRoles={['MEDICO_TRABALHO']}>
                <AssinaturasPage />
              </ProtectedRoute>
            }
          />

          {/* Agente IA */}
          <Route
            path="/ia"
            element={
              <ProtectedRoute allowedRoles={['ESPECIALISTA_SST', 'RH_EMPRESA']}>
                <AgenteIAPage />
              </ProtectedRoute>
            }
          />

          {/* Notificações */}
          <Route path="/notificacoes" element={<NotificacoesPage />} />

          <Route path="*" element={<HomeRedirect />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
