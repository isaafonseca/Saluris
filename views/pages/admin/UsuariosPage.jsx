import React, { useState, useEffect, useCallback } from 'react';
import {
  UserPlus,
  Edit3,
  KeyRound,
  Copy,
  Check,
  Power,
  AlertCircle,
  Filter,
} from 'lucide-react';
import { api } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import { useEmpresa } from '../../context/EmpresaContext';
import { PERFIL_LABELS, UFS_BRASIL, formatDateTime } from '../../utils/formatters';
import { Table } from '../../components/ui/Table';
import { Modal } from '../../components/ui/Modal';
import { Badge } from '../../components/ui/Badge';
import {
  ClearFiltersButton,
  FormField,
} from '../../components/ui/Common';

export function UsuariosPage() {
  const toast = useToast();
  const { empresas, reloadEmpresas } = useEmpresa();

  const [usuarios, setUsuarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filtroEmpresaId, setFiltroEmpresaId] = useState('');
  const [filtroPerfil, setFiltroPerfil] = useState('');

  // Modal Criar / Editar
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [form, setForm] = useState({
    nome: '',
    email: '',
    perfil: 'RH_EMPRESA',
    empresaId: '',
    crm: '',
    crmUf: 'SP',
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMsg, setErrorMsg] = useState('');
  const [saving, setSaving] = useState(false);

  // Caixa destacada de Senha Temporária
  const [tempPasswordInfo, setTempPasswordInfo] = useState(null);
  const [copied, setCopied] = useState(false);

  const loadUsuarios = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.get('/usuarios', {
        empresaId: filtroEmpresaId || undefined,
        perfil: filtroPerfil || undefined,
      });
      setUsuarios(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar usuários.');
    } finally {
      setLoading(false);
    }
  }, [filtroEmpresaId, filtroPerfil, toast]);

  useEffect(() => {
    reloadEmpresas();
  }, [reloadEmpresas]);

  useEffect(() => {
    loadUsuarios();
  }, [loadUsuarios]);

  const openCreateModal = () => {
    setEditingUser(null);
    setForm({
      nome: '',
      email: '',
      perfil: 'RH_EMPRESA',
      empresaId: empresas[0]?.id ? String(empresas[0].id) : '',
      crm: '',
      crmUf: 'SP',
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const openEditModal = (u) => {
    setEditingUser(u);
    setForm({
      nome: u.nome || '',
      email: u.email || '',
      perfil: u.perfil || 'RH_EMPRESA',
      empresaId: u.empresaId ? String(u.empresaId) : '',
      crm: u.crm || '',
      crmUf: u.crmUf || 'SP',
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const requiresEmpresa = form.perfil !== 'ADMINISTRADOR';
  const requiresCrm = form.perfil === 'MEDICO_TRABALHO';

  const handleSave = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setErrorMsg('');

    const errs = {};
    if (!form.nome.trim()) errs.nome = 'Informe o nome completo.';
    if (!editingUser && !form.email.trim()) errs.email = 'Informe o e-mail.';
    if (requiresEmpresa && !form.empresaId) {
      errs.empresaId = 'Este perfil exige vínculo com uma empresa.';
    }
    if (requiresCrm) {
      if (!form.crm.trim()) errs.crm = 'O CRM é obrigatório para Médico do Trabalho.';
      if (!form.crmUf) errs.crmUf = 'Selecione a UF do CRM.';
    }
    if (Object.keys(errs).length > 0) {
      setFieldErrors(errs);
      return;
    }

    const payload = {
      nome: form.nome.trim(),
      perfil: form.perfil,
      empresaId: requiresEmpresa ? Number(form.empresaId) : null,
      crm: requiresCrm ? form.crm.trim() : null,
      crmUf: requiresCrm ? form.crmUf : null,
    };

    setSaving(true);
    try {
      if (editingUser) {
        await api.put(`/usuarios/${editingUser.id}`, payload);
        toast.success('Usuário atualizado com sucesso.');
      } else {
        const res = await api.post('/usuarios', {
          ...payload,
          email: form.email.trim(),
        });
        toast.success('Usuário criado com sucesso.');
        if (res?.senhaTemporaria) {
          setTempPasswordInfo({
            nome: res.usuario?.nome || form.nome,
            email: res.usuario?.email || form.email,
            senhaTemporaria: res.senhaTemporaria,
            acao: 'Criação de Usuário',
          });
          setCopied(false);
        }
      }
      setModalOpen(false);
      loadUsuarios();
    } catch (err) {
      setErrorMsg(err.detail || 'Erro ao salvar usuário.');
      if (err.erros) setFieldErrors(err.erros);
    } finally {
      setSaving(false);
    }
  };

  const handleToggleAtivo = async (u) => {
    try {
      await api.patch(`/usuarios/${u.id}/ativo`, { ativo: !u.ativo });
      toast.success(`Usuário ${!u.ativo ? 'ativado' : 'desativado'} com sucesso.`);
      loadUsuarios();
    } catch (err) {
      toast.error(err.detail || 'Não foi possível alterar o status do usuário.');
    }
  };

  const handleResetPassword = async (u) => {
    try {
      const res = await api.post(`/usuarios/${u.id}/redefinir-senha`);
      if (res?.senhaTemporaria) {
        setTempPasswordInfo({
          nome: u.nome,
          email: u.email,
          senhaTemporaria: res.senhaTemporaria,
          acao: 'Redefinição de Senha',
        });
        setCopied(false);
      }
      toast.success('Senha temporária gerada com sucesso.');
      loadUsuarios();
    } catch (err) {
      toast.error(err.detail || 'Falha ao redefinir a senha.');
    }
  };

  const copyTempPassword = () => {
    if (!tempPasswordInfo?.senhaTemporaria) return;
    navigator.clipboard.writeText(tempPasswordInfo.senhaTemporaria);
    setCopied(true);
    toast.info('Senha temporária copiada para a área de transferência!');
  };

  const columns = [
    {
      header: 'Nome / E-mail',
      render: (u) => (
        <div>
          <div className="font-semibold">{u.nome}</div>
          <div className="text-muted text-xs">{u.email}</div>
        </div>
      ),
    },
    {
      header: 'Perfil',
      render: (u) => (
        <div>
          <Badge variant="info">{PERFIL_LABELS[u.perfil] || u.perfil}</Badge>
          {u.crm && (
            <div className="text-xs text-muted" style={{ marginTop: 4 }}>
              CRM: {u.crm}/{u.crmUf}
            </div>
          )}
        </div>
      ),
    },
    {
      header: 'Empresa',
      render: (u) => u.empresaNome || <span className="text-muted">Global (T.I.)</span>,
    },
    {
      header: 'Status',
      render: (u) => (
        <div className="flex-gap-sm">
          <Badge variant={u.ativo ? 'success' : 'danger'}>
            {u.ativo ? 'Ativo' : 'Inativo'}
          </Badge>
          {u.deveTrocarSenha && <Badge variant="warning">Troca de senha pendente</Badge>}
        </div>
      ),
    },
    {
      header: 'Último Acesso',
      render: (u) => formatDateTime(u.ultimoAcesso),
    },
    {
      header: 'Ações',
      render: (u) => (
        <div className="table-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => openEditModal(u)}
            title="Editar usuário"
          >
            <Edit3 size={15} /> Editar
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleResetPassword(u)}
            title="Gerar nova senha temporária"
          >
            <KeyRound size={15} /> Redefinir Senha
          </button>
          <button
            type="button"
            className={`btn btn-sm ${u.ativo ? 'btn-outline-danger' : 'btn-outline-success'}`}
            onClick={() => handleToggleAtivo(u)}
          >
            <Power size={15} /> {u.ativo ? 'Desativar' : 'Ativar'}
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Gestão de Usuários</h2>
          <p className="page-subtitle">
            Cadastre e gerencie contas de acesso por empresa e perfil (Administrador, Especialista SST, Médico do Trabalho e RH).
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={openCreateModal}>
          <UserPlus size={18} /> Novo Usuário
        </button>
      </div>

      {/* Caixa destacada de Senha Temporária */}
      {tempPasswordInfo && (
        <div className="temp-password-banner">
          <div className="temp-password-content">
            <div className="temp-password-header">
              <KeyRound size={20} />
              <strong>{tempPasswordInfo.acao} — Credencial Temporária Gerada</strong>
            </div>
            <p>
              Envie a senha temporária abaixo para <strong>{tempPasswordInfo.nome}</strong> ({tempPasswordInfo.email}). No primeiro login, o sistema exigirá a troca imediata da senha.
            </p>
            <div className="temp-password-box">
              <code>{tempPasswordInfo.senhaTemporaria}</code>
              <button type="button" className="btn btn-primary btn-sm" onClick={copyTempPassword}>
                {copied ? <Check size={16} /> : <Copy size={16} />}
                {copied ? 'Copiado!' : 'Copiar Senha'}
              </button>
            </div>
          </div>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => setTempPasswordInfo(null)}
          >
            Fechar aviso
          </button>
        </div>
      )}

      {/* Filtros */}
      <div className="card filter-card">
        <div className="filter-row">
          <div className="filter-item">
            <label className="form-label">
              <Filter size={14} /> Filtrar por Empresa
            </label>
            <select
              className="form-control"
              value={filtroEmpresaId}
              onChange={(e) => setFiltroEmpresaId(e.target.value)}
            >
              <option value="">Todas as empresas</option>
              {empresas.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  {emp.nomeFantasia || emp.razaoSocial}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-item">
            <label className="form-label">Filtrar por Perfil</label>
            <select
              className="form-control"
              value={filtroPerfil}
              onChange={(e) => setFiltroPerfil(e.target.value)}
            >
              <option value="">Todos os perfis</option>
              {Object.entries(PERFIL_LABELS).map(([k, label]) => (
                <option key={k} value={k}>
                  {label}
                </option>
              ))}
            </select>
          </div>

        </div>
        <ClearFiltersButton
          active={Boolean(filtroEmpresaId || filtroPerfil)}
          onClick={() => {
            setFiltroEmpresaId('');
            setFiltroPerfil('');
          }}
          disabled={loading}
        />
      </div>

      {/* Tabela */}
      <div className="card">
        <Table
          columns={columns}
          data={usuarios}
          loading={loading}
          emptyTitle="Nenhum usuário encontrado"
          emptyDescription="Tente ajustar os filtros ou cadastre um novo usuário."
        />
      </div>

      {/* Modal Criar / Editar */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editingUser ? 'Editar Usuário' : 'Novo Usuário'}
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
            <button type="submit" form="user-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Salvando...' : editingUser ? 'Salvar Alterações' : 'Criar Usuário'}
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

        <form id="user-form" onSubmit={handleSave}>
          <FormField label="Nome Completo" required error={fieldErrors.nome}>
            <input
              type="text"
              className="form-control"
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              required
            />
          </FormField>

          <FormField
            label="E-mail"
            required={!editingUser}
            error={fieldErrors.email}
            hint={editingUser ? 'O e-mail não pode ser alterado após o cadastro.' : undefined}
          >
            <input
              type="email"
              className="form-control"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              disabled={!!editingUser}
              required
            />
          </FormField>

          <FormField label="Perfil de Acesso" required error={fieldErrors.perfil}>
            <select
              className="form-control"
              value={form.perfil}
              onChange={(e) => setForm({ ...form, perfil: e.target.value })}
            >
              {Object.entries(PERFIL_LABELS).map(([k, label]) => (
                <option key={k} value={k}>
                  {label}
                </option>
              ))}
            </select>
          </FormField>

          {requiresEmpresa && (
            <FormField label="Empresa Vinculada" required error={fieldErrors.empresaId}>
              <select
                className="form-control"
                value={form.empresaId}
                onChange={(e) => setForm({ ...form, empresaId: e.target.value })}
                required
              >
                <option value="">-- Selecione a empresa --</option>
                {empresas.map((emp) => (
                  <option key={emp.id} value={emp.id}>
                    {emp.nomeFantasia || emp.razaoSocial}
                  </option>
                ))}
              </select>
            </FormField>
          )}

          {requiresCrm && (
            <div className="form-row-2">
              <FormField label="Número do CRM" required error={fieldErrors.crm}>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Ex: 123456"
                  value={form.crm}
                  onChange={(e) => setForm({ ...form, crm: e.target.value })}
                  required
                />
              </FormField>
              <FormField label="UF do CRM" required error={fieldErrors.crmUf}>
                <select
                  className="form-control"
                  value={form.crmUf}
                  onChange={(e) => setForm({ ...form, crmUf: e.target.value })}
                >
                  {UFS_BRASIL.map((uf) => (
                    <option key={uf} value={uf}>
                      {uf}
                    </option>
                  ))}
                </select>
              </FormField>
            </div>
          )}
        </form>
      </Modal>
    </div>
  );
}
