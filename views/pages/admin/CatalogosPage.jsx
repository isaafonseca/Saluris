import React, { useState, useEffect, useCallback } from 'react';
import { Plus, Edit3, Power, AlertCircle, Stethoscope, FileText, FileCheck2 } from 'lucide-react';
import { api } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import { Table } from '../../components/ui/Table';
import { Modal } from '../../components/ui/Modal';
import { Badge } from '../../components/ui/Badge';
import { FormField } from '../../components/ui/Common';

const TABS = [
  { key: 'exames', label: 'Tipos de Exame', endpoint: '/tipos-exame', icon: Stethoscope },
  { key: 'documentos', label: 'Tipos de Documento SST', endpoint: '/tipos-documento', icon: FileText },
  { key: 'laudos', label: 'Tipos de Laudo Técnico', endpoint: '/tipos-laudo', icon: FileCheck2 },
];

export function CatalogosPage() {
  const toast = useToast();
  const [activeTab, setActiveTab] = useState('exames');
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({
    sigla: '',
    nome: '',
    descricao: '',
    observacoesTecnicas: '',
    observacoes: '',
    periodicidadePadraoMeses: 12,
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMsg, setErrorMsg] = useState('');
  const [saving, setSaving] = useState(false);

  const currentTabConfig = TABS.find((t) => t.key === activeTab);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.get(currentTabConfig.endpoint);
      setItems(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar catálogo.');
    } finally {
      setLoading(false);
    }
  }, [currentTabConfig.endpoint, toast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const openCreate = () => {
    setEditing(null);
    setForm({
      sigla: '',
      nome: '',
      descricao: '',
      observacoesTecnicas: '',
      observacoes: '',
      periodicidadePadraoMeses: 12,
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const openEdit = (item) => {
    setEditing(item);
    setForm({
      sigla: item.sigla || '',
      nome: item.nome || '',
      descricao: item.descricao || '',
      observacoesTecnicas: item.observacoesTecnicas || '',
      observacoes: item.observacoes || '',
      periodicidadePadraoMeses: item.periodicidadePadraoMeses || 12,
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
      let payload = {};
      if (activeTab === 'exames') {
        payload = {
          nome: form.nome.trim(),
          descricao: form.descricao.trim() || null,
          observacoesTecnicas: form.observacoesTecnicas.trim() || null,
        };
      } else if (activeTab === 'documentos') {
        payload = {
          sigla: form.sigla.trim().toUpperCase(),
          nome: form.nome.trim(),
          descricao: form.descricao.trim() || null,
          periodicidadePadraoMeses: Number(form.periodicidadePadraoMeses),
        };
      } else {
        payload = {
          nome: form.nome.trim(),
          descricao: form.descricao.trim() || null,
          observacoes: form.observacoes.trim() || null,
        };
      }

      if (editing) {
        await api.put(`${currentTabConfig.endpoint}/${editing.id}`, payload);
        toast.success('Registro atualizado com sucesso.');
      } else {
        await api.post(currentTabConfig.endpoint, payload);
        toast.success('Registro criado com sucesso.');
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
      toast.success(`Item ${!item.ativo ? 'ativado' : 'desativado'} com sucesso.`);
      loadData();
    } catch (err) {
      toast.error(err.detail || 'Erro ao alterar status.');
    }
  };

  const columns = [
    ...(activeTab === 'documentos'
      ? [
          {
            header: 'Sigla',
            width: '100px',
            render: (r) => <Badge variant="info">{r.sigla}</Badge>,
          },
        ]
      : []),
    {
      header: 'Nome',
      render: (r) => <span className="font-semibold">{r.nome}</span>,
    },
    {
      header: 'Descrição / Observações',
      render: (r) => (
        <div>
          <div>{r.descricao || '—'}</div>
          {r.observacoesTecnicas && (
            <div className="text-xs text-muted">Obs. Técnica: {r.observacoesTecnicas}</div>
          )}
          {r.observacoes && <div className="text-xs text-muted">Obs.: {r.observacoes}</div>}
        </div>
      ),
    },
    ...(activeTab === 'documentos'
      ? [
          {
            header: 'Periodicidade Padrão',
            render: (r) => `${r.periodicidadePadraoMeses} meses`,
          },
        ]
      : []),
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
          <h2>Catálogos Globais de SST</h2>
          <p className="page-subtitle">
            Padronize os tipos de exames ocupacionais, programas/documentos (PGR, PCMSO) e laudos técnicos disponíveis para todas as empresas.
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={openCreate}>
          <Plus size={18} /> Novo Registro
        </button>
      </div>

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
            <button type="submit" form="catalogo-form" className="btn btn-primary" disabled={saving}>
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

        <form id="catalogo-form" onSubmit={handleSave}>
          {activeTab === 'documentos' && (
            <div className="form-row-2">
              <FormField label="Sigla (ex: PGR, PCMSO)" required error={fieldErrors.sigla}>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Ex: PGR"
                  maxLength={20}
                  value={form.sigla}
                  onChange={(e) => setForm({ ...form, sigla: e.target.value.toUpperCase() })}
                  required
                />
              </FormField>

              <FormField
                label="Periodicidade Padrão"
                required
                error={fieldErrors.periodicidadePadraoMeses}
              >
                <select
                  className="form-control"
                  value={form.periodicidadePadraoMeses}
                  onChange={(e) =>
                    setForm({ ...form, periodicidadePadraoMeses: Number(e.target.value) })
                  }
                >
                  <option value={12}>12 meses (Anual)</option>
                  <option value={24}>24 meses (Bienal)</option>
                </select>
              </FormField>
            </div>
          )}

          <FormField label="Nome" required error={fieldErrors.nome}>
            <input
              type="text"
              className="form-control"
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              required
            />
          </FormField>

          <FormField label="Descrição" error={fieldErrors.descricao}>
            <textarea
              className="form-control"
              rows={3}
              value={form.descricao}
              onChange={(e) => setForm({ ...form, descricao: e.target.value })}
            />
          </FormField>

          {activeTab === 'exames' && (
            <FormField label="Observações Técnicas / Preparo" error={fieldErrors.observacoesTecnicas}>
              <textarea
                className="form-control"
                rows={2}
                placeholder="Ex: Repouso auditivo de 14 horas..."
                value={form.observacoesTecnicas}
                onChange={(e) => setForm({ ...form, observacoesTecnicas: e.target.value })}
              />
            </FormField>
          )}

          {activeTab === 'laudos' && (
            <FormField label="Observações Normativas" error={fieldErrors.observacoes}>
              <textarea
                className="form-control"
                rows={2}
                placeholder="Ex: Baseado na NR-15 / NR-16..."
                value={form.observacoes}
                onChange={(e) => setForm({ ...form, observacoes: e.target.value })}
              />
            </FormField>
          )}
        </form>
      </Modal>
    </div>
  );
}
