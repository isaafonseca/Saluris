import React, { useState, useEffect, useCallback } from 'react';
import { Plus, Edit3, Power, MapPin, Briefcase, Layers, AlertCircle } from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useEmpresa } from '../context/EmpresaContext';
import { useToast } from '../context/ToastContext';
import { UFS_BRASIL } from '../utils/formatters';
import { AdminEmpresaSelector } from '../components/ui/AdminEmpresaSelector';
import { Table } from '../components/ui/Table';
import { Modal } from '../components/ui/Modal';
import { Badge } from '../components/ui/Badge';
import { FormField, EmptyState } from '../components/ui/Common';

const TABS = [
  { key: 'unidades', label: 'Unidades Operacionais', endpoint: '/unidades', icon: MapPin },
  { key: 'funcoes', label: 'Funções / Cargos', endpoint: '/funcoes', icon: Briefcase },
  { key: 'setores', label: 'Setores / Ambientes', endpoint: '/setores', icon: Layers },
];

export function EstruturaPage() {
  const { isAdmin } = useAuth();
  const { activeEmpresaId } = useEmpresa();
  const toast = useToast();

  const [activeTab, setActiveTab] = useState('unidades');
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({
    nome: '',
    cidade: '',
    uf: 'SP',
    descricao: '',
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMsg, setErrorMsg] = useState('');
  const [saving, setSaving] = useState(false);

  const currentTabConfig = TABS.find((t) => t.key === activeTab);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.get(currentTabConfig.endpoint, {
        empresaId: isAdmin && activeEmpresaId ? activeEmpresaId : undefined,
      });
      setItems(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar estrutura da empresa.');
    } finally {
      setLoading(false);
    }
  }, [currentTabConfig.endpoint, isAdmin, activeEmpresaId, toast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const openCreate = () => {
    setEditing(null);
    setForm({ nome: '', cidade: '', uf: 'SP', descricao: '' });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const openEdit = (item) => {
    setEditing(item);
    setForm({
      nome: item.nome || '',
      cidade: item.cidade || '',
      uf: item.uf || 'SP',
      descricao: item.descricao || '',
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setErrorMsg('');
    setSaving(true);

    try {
      const payload =
        activeTab === 'unidades'
          ? {
              nome: form.nome.trim(),
              cidade: form.cidade.trim() || null,
              uf: form.uf || null,
            }
          : {
              nome: form.nome.trim(),
              descricao: form.descricao.trim() || null,
            };

      const params = isAdmin ? { empresaId: activeEmpresaId } : undefined;

      if (editing) {
        await api.put(`${currentTabConfig.endpoint}/${editing.id}`, payload, params);
        toast.success('Registro atualizado com sucesso.');
      } else {
        await api.post(currentTabConfig.endpoint, payload, params);
        toast.success('Registro cadastrado com sucesso.');
      }
      setModalOpen(false);
      loadData();
    } catch (err) {
      setErrorMsg(err.detail || 'Erro ao salvar item.');
      if (err.erros) setFieldErrors(err.erros);
    } finally {
      setSaving(false);
    }
  };

  const handleToggleAtivo = async (item) => {
    try {
      await api.patch(`${currentTabConfig.endpoint}/${item.id}/ativo`, { ativo: !item.ativo });
      toast.success(`Registro ${!item.ativo ? 'ativado' : 'desativado'} com sucesso.`);
      loadData();
    } catch (err) {
      toast.error(err.detail || 'Não foi possível alterar o status.');
    }
  };

  const columns = [
    {
      header: 'Nome',
      render: (r) => <span className="font-semibold">{r.nome}</span>,
    },
    ...(activeTab === 'unidades'
      ? [
          {
            header: 'Localização (Cidade / UF)',
            render: (r) => (r.cidade ? `${r.cidade} / ${r.uf || ''}` : r.uf || '—'),
          },
        ]
      : [
          {
            header: 'Descrição das Atividades / Ambiente',
            render: (r) => r.descricao || '—',
          },
        ]),
    {
      header: 'Status',
      render: (r) => (
        <Badge variant={r.ativo ? 'success' : 'danger'}>
          {r.ativo ? 'Ativo' : 'Inativo'}
        </Badge>
      ),
    },
    {
      header: 'Ações',
      render: (r) => (
        <div className="table-actions">
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => openEdit(r)}>
            <Edit3 size={15} /> Editar
          </button>
          <button
            type="button"
            className={`btn btn-sm ${r.ativo ? 'btn-outline-danger' : 'btn-outline-success'}`}
            onClick={() => handleToggleAtivo(r)}
          >
            <Power size={15} /> {r.ativo ? 'Desativar' : 'Ativar'}
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Estrutura Organizacional da Empresa</h2>
          <p className="page-subtitle">
            Cadastre as unidades, funções e setores utilizados para vincular colaboradores e configurar a matriz de periodicidade de exames.
          </p>
        </div>
        {(!isAdmin || activeEmpresaId) && (
          <button type="button" className="btn btn-primary" onClick={openCreate}>
            <Plus size={18} /> Novo Registro
          </button>
        )}
      </div>

      <AdminEmpresaSelector />

      <div className="tabs-bar">
        {TABS.map((t) => {
          const Icon = t.icon;
          return (
            <button
              key={t.key}
              type="button"
              className={`tab-btn ${activeTab === t.key ? 'active' : ''}`}
              onClick={() => setActiveTab(t.key)}
            >
              <Icon size={17} />
              <span>{t.label}</span>
            </button>
          );
        })}
      </div>

      <div className="card">
        <Table columns={columns} data={items} loading={loading} />
      </div>

      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={`${editing ? 'Editar' : 'Novo'} — ${currentTabConfig.label}`}
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
            <button type="submit" form="estrutura-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Salvando...' : 'Salvar'}
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

        <form id="estrutura-form" onSubmit={handleSave}>
          <FormField label="Nome" required error={fieldErrors.nome}>
            <input
              type="text"
              className="form-control"
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              required
            />
          </FormField>

          {activeTab === 'unidades' ? (
            <div className="form-row-2">
              <FormField label="Cidade" required error={fieldErrors.cidade}>
                <input
                  type="text"
                  className="form-control"
                  value={form.cidade}
                  onChange={(e) => setForm({ ...form, cidade: e.target.value })}
                  required
                />
              </FormField>
              <FormField label="UF" required error={fieldErrors.uf}>
                <select
                  className="form-control"
                  value={form.uf}
                  onChange={(e) => setForm({ ...form, uf: e.target.value })}
                >
                  {UFS_BRASIL.map((uf) => (
                    <option key={uf} value={uf}>
                      {uf}
                    </option>
                  ))}
                </select>
              </FormField>
            </div>
          ) : (
            <FormField label="Descrição" error={fieldErrors.descricao}>
              <textarea
                className="form-control"
                rows={3}
                value={form.descricao}
                onChange={(e) => setForm({ ...form, descricao: e.target.value })}
              />
            </FormField>
          )}
        </form>
      </Modal>
    </div>
  );
}
