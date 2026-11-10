import React, { useEffect } from 'react';
import { Building2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useEmpresa } from '../../context/EmpresaContext';
import { formatCNPJ } from '../../utils/formatters';

export function AdminEmpresaSelector({ label = 'Visualizando dados da empresa:' }) {
  const { isAdmin } = useAuth();
  const { empresas, selectedEmpresaId, setSelectedEmpresaId, loadingEmpresas } = useEmpresa();

  // Ao entrar ou trocar de página, inicia sempre em "-- Selecione uma empresa --" (exibindo todas)
  useEffect(() => {
    setSelectedEmpresaId('');
  }, [setSelectedEmpresaId]);

  if (!isAdmin) return null;

  return (
    <div className="admin-empresa-bar">
      <div className="admin-empresa-label">
        <Building2 size={18} />
        <span>{label}</span>
      </div>
      <div className="admin-empresa-select-wrap">
        <select
          className="form-control"
          value={selectedEmpresaId}
          onChange={(e) => setSelectedEmpresaId(e.target.value)}
          disabled={loadingEmpresas}
        >
          <option value="">-- Selecione uma empresa (Todas) --</option>
          {empresas.map((emp) => (
            <option key={emp.id} value={emp.id}>
              {emp.nomeFantasia || emp.razaoSocial} ({formatCNPJ(emp.cnpj)}) {!emp.ativa ? '[Inativa]' : ''}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
