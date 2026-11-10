import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { api } from '../api/client';
import { useAuth } from './AuthContext';

const EmpresaContext = createContext(null);

export function EmpresaProvider({ children }) {
  const { user, isAdmin } = useAuth();
  const [empresas, setEmpresas] = useState([]);
  const [selectedEmpresaId, setSelectedEmpresaIdState] = useState('');
  const [loadingEmpresas, setLoadingEmpresas] = useState(false);

  const loadEmpresas = useCallback(async () => {
    if (!user || user.deveTrocarSenha) return;
    if (!isAdmin) {
      setEmpresas([]);
      return;
    }
    setLoadingEmpresas(true);
    try {
      localStorage.removeItem('saluris_admin_empresa');
      const list = await api.get('/empresas');
      const arr = Array.isArray(list) ? list : [];
      setEmpresas(arr);
    } catch (_) {
      setEmpresas([]);
    } finally {
      setLoadingEmpresas(false);
    }
  }, [user, isAdmin]);

  useEffect(() => {
    loadEmpresas();
  }, [loadEmpresas]);

  const setSelectedEmpresaId = useCallback((id) => {
    const val = id ? String(id) : '';
    setSelectedEmpresaIdState(val);
  }, []);

  // Para ADMIN retorna a empresa selecionada no seletor; para demais perfis retorna a própria empresa
  const activeEmpresaId = isAdmin ? (selectedEmpresaId || null) : (user?.empresaId || null);
  const selectedEmpresa = isAdmin
    ? empresas.find((e) => String(e.id) === String(selectedEmpresaId)) || null
    : null;

  return (
    <EmpresaContext.Provider
      value={{
        empresas,
        selectedEmpresaId,
        setSelectedEmpresaId,
        activeEmpresaId,
        selectedEmpresa,
        loadingEmpresas,
        reloadEmpresas: loadEmpresas,
      }}
    >
      {children}
    </EmpresaContext.Provider>
  );
}

export function useEmpresa() {
  const ctx = useContext(EmpresaContext);
  if (!ctx) {
    throw new Error('useEmpresa deve ser usado dentro de um EmpresaProvider');
  }
  return ctx;
}
