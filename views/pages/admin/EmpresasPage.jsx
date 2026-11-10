import React, { useState, useEffect, useCallback } from 'react';
import { Building2, Plus, Edit3, Power, AlertCircle } from 'lucide-react';
import { api } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import { useEmpresa } from '../../context/EmpresaContext';
import {
  formatCNPJ,
  formatPhone,
  isValidCNPJ,
  PORTE_LABELS,
} from '../../utils/formatters';
import { Table } from '../../components/ui/Table';
import { Modal } from '../../components/ui/Modal';
import { Badge } from '../../components/ui/Badge';
import { FormField } from '../../components/ui/Common';

export function EmpresasPage() {
  const toast = useToast();
  const { reloadEmpresas } = useEmpresa();

  const [empresas, setEmpresas] = useState([]);
  const [loading, setLoading] = useState(true);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({
    cnpj: '',
    razaoSocial: '',
    nomeFantasia: '',
    setorAtuacao: '',
    porte: 'ME',
    contatoNome: '',
    emailContato: '',
    telefone: '',
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMsg, setErrorMsg] = useState('');
  const [saving, setSaving] = useState(false);

  const loadList = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.get('/empresas');
      setEmpresas(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar empresas.');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    loadList();
  }, [loadList]);

  const openCreate = () => {
    setEditing(null);
    setForm({
      cnpj: '',
      razaoSocial: '',
      nomeFantasia: '',
      setorAtuacao: '',
      porte: 'ME',
      contatoNome: '',
      emailContato: '',
      telefone: '',
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const openEdit = (emp) => {
    setEditing(emp);
    setForm({
      cnpj: formatCNPJ(emp.cnpj || ''),
      razaoSocial: emp.razaoSocial || '',
      nomeFantasia: emp.nomeFantasia || '',
      setorAtuacao: emp.setorAtuacao || '',
      porte: emp.porte || 'ME',
      contatoNome: emp.contatoNome || '',
      emailContato: emp.emailContato || '',
      telefone: formatPhone(emp.telefone || ''),
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setErrorMsg('');

    if (!isValidCNPJ(form.cnpj)) {
      setFieldErrors({ cnpj: 'Informe um CNPJ válido com 14 dígitos.' });
      return;
    }

    setSaving(true);
    try {
      const payload = {
        cnpj: form.cnpj,
        razaoSocial: form.razaoSocial.trim(),
        nomeFantasia: form.nomeFantasia.trim() || null,
        setorAtuacao: form.setorAtuacao.trim(),
        porte: form.porte,
        contatoNome: form.contatoNome.trim() || null,
        emailContato: form.emailContato.trim(),
        telefone: form.telefone.trim() || null,
      };

      if (editing) {
        await api.put(`/empresas/${editing.id}`, payload);
        toast.success('Empresa atualizada com sucesso.');
      } else {
        await api.post('/empresas', payload);
        toast.success('Empresa cadastrada com sucesso.');
      }
      setModalOpen(false);
      loadList();
      reloadEmpresas();
    } catch (err) {
      setErrorMsg(err.detail || 'Erro ao salvar empresa.');
      if (err.erros) setFieldErrors(err.erros);
    } finally {
      setSaving(false);
    }
  };

  const handleToggleAtiva = async (emp) => {
    try {
      await api.patch(`/empresas/${emp.id}/ativa`, { ativa: !emp.ativa });
      toast.success(`Empresa ${!emp.ativa ? 'ativada' : 'desativada'} com sucesso.`);
      loadList();
      reloadEmpresas();
    } catch (err) {
      toast.error(err.detail || 'Não foi possível alterar o status da empresa.');
    }
  };

  const columns = [
    {
      header: 'Razão Social / Nome Fantasia',
      render: (e) => (
        <div>
          <div className="font-semibold">{e.razaoSocial}</div>
          {e.nomeFantasia && <div className="text-muted text-xs">{e.nomeFantasia}</div>}
        </div>
      ),
    },
    {
      header: 'CNPJ',
      render: (e) => <span className="mono">{formatCNPJ(e.cnpj)}</span>,
    },
    {
      header: 'Porte / Setor',
      render: (e) => (
        <div>
          <Badge variant="info">{PORTE_LABELS[e.porte] || e.porte}</Badge>
          <div className="text-xs text-muted" style={{ marginTop: 4 }}>
            {e.setorAtuacao}
          </div>
        </div>
      ),
    },
    {
      header: 'Contato',
      render: (e) => (
        <div>
          <div>{e.contatoNome || '—'}</div>
          <div className="text-xs text-muted">{e.emailContato}</div>
          {e.telefone && <div className="text-xs text-muted">{formatPhone(e.telefone)}</div>}
        </div>
      ),
    },
    {
      header: 'Status',
      render: (e) => (
        <Badge variant={e.ativa ? 'success' : 'danger'}>
          {e.ativa ? 'Ativa' : 'Inativa'}
        </Badge>
      ),
    },
    {
      header: 'Ações',
      render: (e) => (
        <div className="table-actions">
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => openEdit(e)}>
            <Edit3 size={15} /> Editar
          </button>
          <button
            type="button"
            className={`btn btn-sm ${e.ativa ? 'btn-outline-danger' : 'btn-outline-success'}`}
            onClick={() => handleToggleAtiva(e)}
          >
            <Power size={15} /> {e.ativa ? 'Desativar' : 'Ativar'}
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Empresas Clientes</h2>
          <p className="page-subtitle">
            Gerencie as empresas atendidas pela plataforma de Saúde e Segurança do Trabalho.
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={openCreate}>
          <Plus size={18} /> Nova Empresa
        </button>
      </div>

      <div className="card">
        <Table
          columns={columns}
          data={empresas}
          loading={loading}
          emptyTitle="Nenhuma empresa cadastrada"
        />
      </div>

      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? 'Editar Empresa' : 'Nova Empresa'}
        size="lg"
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setModalOpen(false)}
              disabled={saving}
            >
              Cancelar
            </button>
            <button type="submit" form="empresa-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Salvando...' : 'Salvar Empresa'}
            </button>
          </>
        }
      >
        {errorMsg && (
          <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
            <AlertCircle size={18} />
            <span>{errorMsg}</span>
          </div>
        )}

        <form id="empresa-form" onSubmit={handleSave}>
          <div className="form-row-2">
            <FormField label="CNPJ" required error={fieldErrors.cnpj}>
              <input
                type="text"
                className="form-control"
                placeholder="00.000.000/0000-00"
                value={form.cnpj}
                onChange={(e) => setForm({ ...form, cnpj: formatCNPJ(e.target.value) })}
                required
              />
            </FormField>

            <FormField label="Porte da Empresa" required error={fieldErrors.porte}>
              <select
                className="form-control"
                value={form.porte}
                onChange={(e) => setForm({ ...form, porte: e.target.value })}
              >
                {Object.entries(PORTE_LABELS).map(([k, label]) => (
                  <option key={k} value={k}>
                    {label}
                  </option>
                ))}
              </select>
            </FormField>
          </div>

          <FormField label="Razão Social" required error={fieldErrors.razaoSocial}>
            <input
              type="text"
              className="form-control"
              value={form.razaoSocial}
              onChange={(e) => setForm({ ...form, razaoSocial: e.target.value })}
              required
            />
          </FormField>

          <div className="form-row-2">
            <FormField label="Nome Fantasia" error={fieldErrors.nomeFantasia}>
              <input
                type="text"
                className="form-control"
                value={form.nomeFantasia}
                onChange={(e) => setForm({ ...form, nomeFantasia: e.target.value })}
              />
            </FormField>

            <FormField label="Setor de Atuação (CNAE / Ramo)" required error={fieldErrors.setorAtuacao}>
              <input
                type="text"
                className="form-control"
                placeholder="Ex: Indústria Metalúrgica, Construção Civil..."
                value={form.setorAtuacao}
                onChange={(e) => setForm({ ...form, setorAtuacao: e.target.value })}
                required
              />
            </FormField>
          </div>

          <div className="form-row-3">
            <FormField label="Responsável / Contato" error={fieldErrors.contatoNome}>
              <input
                type="text"
                className="form-control"
                value={form.contatoNome}
                onChange={(e) => setForm({ ...form, contatoNome: e.target.value })}
              />
            </FormField>

            <FormField label="E-mail de Contato" required error={fieldErrors.emailContato}>
              <input
                type="email"
                className="form-control"
                value={form.emailContato}
                onChange={(e) => setForm({ ...form, emailContato: e.target.value })}
                required
              />
            </FormField>

            <FormField label="Telefone" error={fieldErrors.telefone}>
              <input
                type="text"
                className="form-control"
                placeholder="(11) 99999-9999"
                value={form.telefone}
                onChange={(e) => setForm({ ...form, telefone: formatPhone(e.target.value) })}
              />
            </FormField>
          </div>
        </form>
      </Modal>
    </div>
  );
}
