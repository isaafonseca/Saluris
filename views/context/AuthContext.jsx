import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { api, getToken, setToken } from '../api/client';

const AuthContext = createContext(null);

const MOCK_USERS = {
  'admin@saluris.com.br': {
    id: 1,
    nome: 'Administrador (T.I.)',
    email: 'admin@saluris.com.br',
    perfil: 'ADMINISTRADOR',
    empresaId: null,
    empresaNome: null,
    deveTrocarSenha: false,
    crm: null,
    crmUf: null,
  },
  'especialista@saluris.com.br': {
    id: 2,
    nome: 'Carlos Eduardo (Especialista SST)',
    email: 'especialista@saluris.com.br',
    perfil: 'ESPECIALISTA_SST',
    empresaId: 1,
    empresaNome: 'PortoLog Santos',
    deveTrocarSenha: false,
    crm: null,
    crmUf: null,
  },
  'medico@saluris.com.br': {
    id: 3,
    nome: 'Dra. Helena Prado (Médica do Trabalho)',
    email: 'medico@saluris.com.br',
    perfil: 'MEDICO_TRABALHO',
    empresaId: 1,
    empresaNome: 'PortoLog Santos',
    deveTrocarSenha: false,
    crm: '148920',
    crmUf: 'SP',
  },
  'rh@saluris.com.br': {
    id: 4,
    nome: 'Fernanda Almeida (RH)',
    email: 'rh@saluris.com.br',
    perfil: 'RH_EMPRESA',
    empresaId: 1,
    empresaNome: 'PortoLog Santos',
    deveTrocarSenha: false,
    crm: null,
    crmUf: null,
  },
};

const DEFAULT_USER = MOCK_USERS['admin@saluris.com.br'];

function resolveMockUser(email) {
  const normalized = (email || '').trim().toLowerCase();
  if (MOCK_USERS[normalized]) {
    return MOCK_USERS[normalized];
  }
  if (normalized.includes('espec')) return MOCK_USERS['especialista@saluris.com.br'];
  if (normalized.includes('med')) return MOCK_USERS['medico@saluris.com.br'];
  if (normalized.includes('rh')) return MOCK_USERS['rh@saluris.com.br'];
  return {
    ...DEFAULT_USER,
    email: normalized || DEFAULT_USER.email,
  };
}

function getInitialUser() {
  try {
    const token = getToken();
    const saved = localStorage.getItem('saluris_user');
    if (token && saved) return JSON.parse(saved);
  } catch (_) {}
  return null;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getInitialUser);
  const [loading, setLoading] = useState(true);

  const loadMe = useCallback(async () => {
    const token = getToken();
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const me = await api.get('/auth/me', null, { skipAuthRedirect: true });
      if (me) {
        setUser(me);
        localStorage.setItem('saluris_user', JSON.stringify(me));
        return me;
      } else {
        const fallback = getInitialUser();
        setUser(fallback);
        return fallback;
      }
    } catch (err) {
      if (err?.status === 401) {
        setToken(null);
        localStorage.removeItem('saluris_user');
        setUser(null);
        return null;
      } else {
        const fallback = getInitialUser();
        setUser(fallback);
        return fallback;
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadMe();
  }, [loadMe]);

  const login = async (email, senha) => {
    try {
      const res = await api.post(
        '/auth/login',
        { email, senha },
        null,
        { skipAuthRedirect: true }
      );
      setToken(res.token);
      setUser(res.usuario);
      localStorage.setItem('saluris_user', JSON.stringify(res.usuario));
      return res.usuario;
    } catch (err) {
      // Se o backend estiver offline (erro de conexão/proxy), permite login local com as contas de demonstração
      if (err?.isProxyOffline || err?.status === 503) {
        const mockUser = resolveMockUser(email);
        setToken('bypass-token');
        setUser(mockUser);
        localStorage.setItem('saluris_user', JSON.stringify(mockUser));
        return mockUser;
      }
      throw err;
    }
  };

  const logout = () => {
    setToken(null);
    localStorage.removeItem('saluris_user');
    localStorage.removeItem('saluris_admin_empresa');
    setUser(null);
  };

  const refreshUser = async () => {
    return await loadMe();
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAuthenticated: !!user,
        isAdmin: user?.perfil === 'ADMINISTRADOR',
        isEspecialista: user?.perfil === 'ESPECIALISTA_SST',
        isMedico: user?.perfil === 'MEDICO_TRABALHO',
        isRh: user?.perfil === 'RH_EMPRESA',
        login,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider');
  }
  return ctx;
}
