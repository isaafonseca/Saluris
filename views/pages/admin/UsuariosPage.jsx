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
  Info,
  ShieldCheck,
} from 'lucide-react';
import { api } from '../../api/client';
import { useToast } from '../../context/ToastContext';
import { useAuth } from '../../context/AuthContext';
import { PERFIL_LABELS, UFS_BRASIL, formatDateTime } from '../../utils/formatters';
import { Table } from '../../components/ui/Table';
import { Modal } from '../../components/ui/Modal';
import { Badge } from '../../components/ui/Badge';
import { FormField } from '../../components/ui/Common';

const PERFIS_OPERACIONAIS_EMPRESA = ['RH_EMPRESA', 'MEDICO_TRABALHO', 'ESPECIALISTA_SST'];
const PERFIS_EMPRESA_FILTRO = ['ADMIN_EMPRESA', ...PERFIS_OPERACIONAIS_EMPRESA];

export function UsuariosPage() {
  const toast = useToast();
  const { user, isAdmin: isGlobalAdmin, isAdminEmpresa: isCompanyAdmin } = useAuth();

  const [usuarios, setUsuarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filtroPerfil, setFiltroPerfil] = useState('');

  // Modal Criar / Editar
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [form, setForm] = useState({
    nome: '',
    email: '',
    perfil: isGlobalAdmin ? 'ADMINISTRADOR' : 'RH_EMPRESA',
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
        perfil: filtroPerfil || undefined,
      });
      setUsuarios(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar usuários.');
    } finally {
      setLoading(false);
    }
  }, [filtroPerfil, toast]);

  useEffect(() => {
    loadUsuarios();
  }, [loadUsuarios]);

  const openCreateModal = () => {
    setEditingUser(null);
    setForm({
      nome: '',
      email: '',
      perfil: isGlobalAdmin ? 'ADMINISTRADOR' : 'RH_EMPRESA',
      empresaId: isCompanyAdmin && user?.empresaId ? String(user.empresaId) : '',
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
      perfil: u.perfil || (isGlobalAdmin ? 'ADMINISTRADOR' : 'RH_EMPRESA'),
      empresaId: u.empresaId ? String(u.empresaId) : isCompanyAdmin && user?.empresaId ? String(user.empresaId) : '',
      crm: u.crm || '',
      crmUf: u.crmUf || 'SP',
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const isEditingMainCompanyAccount = Boolean(
    isCompanyAdmin && editingUser && editingUser.perfil === 'ADMIN_EMPRESA'
  );
  const effectivePerfil = isGlobalAdmin
    ? 'ADMINISTRADOR'
    : isEditingMainCompanyAccount
      ? 'ADMIN_EMPRESA'
      : form.perfil;
  const requiresEmpresa = effectivePerfil !== 'ADMINISTRADOR';
  const requiresCrm = effectivePerfil === 'MEDICO_TRABALHO';

  const handleSave = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setErrorMsg('');

    const errs = {};
    if (!form.nome.trim()) errs.nome = 'Informe o nome completo.';
    if (!editingUser && !form.email.trim()) errs.email = 'Informe o e-mail.';
    const empresaIdResolvida = requiresEmpresa
      ? Number(form.empresaId || user?.empresaId)
      : null;
    if (requiresEmpresa && !empresaIdResolvida) {
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
      perfil: effectivePerfil,
      empresaId: requiresEmpresa ? empresaIdResolvida : null,
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
          <div className="font-semibold">
            {u.nome}
            {u.id === user?.id && <span className="text-muted"> (você)</span>}
          </div>
          <div className="text-muted text-xs">{u.email}</div>
        </div>
      ),
    },
    {
      header: 'Perfil',
      render: (u) => (
        <div>
          <Badge variant={u.perfil === 'ADMIN_EMPRESA' ? 'warning' : 'info'}>
            {PERFIL_LABELS[u.perfil] || u.perfil}
          </Badge>
          {u.crm && (
            <div className="text-xs text-muted" style={{ marginTop: 4 }}>
              CRM: {u.crm}/{u.crmUf}
            </div>
          )}
        </div>
      ),
    },
    {
      header: 'Escopo / Empresa',
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
      render: (u) => {
        const isSelf = u.id === user?.id;
        const isMainCompanyAccount = u.perfil === 'ADMIN_EMPRESA';
        return (
          <div className="table-actions">
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => openEditModal(u)}
              title="Editar usuário"
            >
              <Edit3 size={15} /> Editar
            </button>
            {!isSelf && (
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => handleResetPassword(u)}
                title="Gerar nova senha temporária"
              >
                <KeyRound size={15} /> Redefinir Senha
              </button>
            )}
            {!isSelf && !isMainCompanyAccount && (
              <button
                type="button"
                className={`btn btn-sm ${u.ativo ? 'btn-outline-danger' : 'btn-outline-success'}`}
                onClick={() => handleToggleAtivo(u)}
              >
                <Power size={15} /> {u.ativo ? 'Desativar' : 'Ativar'}
              </button>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>
            {isGlobalAdmin
              ? 'Administradores (T.I.)'
              : `Usuários da Empresa${user?.empresaNome ? ` — ${user.empresaNome}` : ''}`}
          </h2>
          <p className="page-subtitle">
            {isGlobalAdmin
              ? 'Cadastre e gerencie apenas outros Administradores globais (T.I.). Usuários internos de empresas são criados pela Conta Principal de cada empresa.'
              : 'Gerencie os usuários operacionais da sua empresa (RH da Empresa, Médico do Trabalho e Especialista SST).'}
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={openCreateModal}>
          <UserPlus size={18} />{' '}
          {isGlobalAdmin ? 'Novo Administrador (T.I.)' : 'Novo Usuário da Empresa'}
        </button>
      </div>

      <div className="alert alert-info" style={{ marginBottom: '1rem' }}>
        <Info size={18} style={{ flexShrink: 0, marginTop: 2 }} />
        {isGlobalAdmin ? (
          <div>
            <strong>Escopo exclusivo de T.I.:</strong> aqui você gerencia apenas contas de{' '}
            <strong>Administrador (T.I.)</strong>. Para dar acesso a uma empresa cliente, crie a empresa em{' '}
            <strong>Empresas Clientes</strong> vinculando o e-mail da <strong>Conta Principal da Empresa</strong> &mdash; somente ela pode criar e visualizar os usuários internos dessa empresa, e apenas{' '}
            <strong>RH</strong>, <strong>Médico do Trabalho</strong> e <strong>Especialista SST</strong> acessam os dados de SST da empresa.
          </div>
        ) : (
          <div>
            <strong>Conta Principal da Empresa ({user?.empresaNome || 'Empresa vinculada'}):</strong> você é responsável por criar e gerenciar as contas de{' '}
            <strong>RH da Empresa</strong>, <strong>Médico do Trabalho</strong> e <strong>Especialista SST</strong> desta empresa. Os dados operacionais de SST ficam restritos a esses perfis cadastrados na empresa.
          </div>
        )}
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

      {/* Filtros (apenas Conta Principal da Empresa possui múltiplos perfis operacionais) */}
      {isCompanyAdmin && (
        <div className="card filter-card">
          <div className="filter-row">
            <div className="filter-item">
              <label className="form-label">
                <Filter size={14} /> Filtrar por Perfil da Empresa
              </label>
              <select
                className="form-control"
                value={filtroPerfil}
                onChange={(e) => setFiltroPerfil(e.target.value)}
              >
                <option value="">Todos os perfis da empresa</option>
                {PERFIS_EMPRESA_FILTRO.map((k) => (
                  <option key={k} value={k}>
                    {PERFIL_LABELS[k] || k}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
      )}

      {/* Tabela */}
      <div className="card">
        <Table
          columns={columns}
          data={usuarios}
          loading={loading}
          emptyTitle={
            isGlobalAdmin
              ? 'Nenhum administrador de T.I. encontrado'
              : 'Nenhum usuário da empresa encontrado'
          }
          emptyDescription={
            isGlobalAdmin
              ? 'Cadastre outros Administradores (T.I.) quando necessário.'
              : 'Cadastre contas de RH, Médico do Trabalho ou Especialista SST para sua empresa.'
          }
        />
      </div>

      {/* Modal Criar / Editar */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={
          isGlobalAdmin
            ? editingUser
              ? 'Editar Administrador (T.I.)'
              : 'Novo Administrador (T.I.)'
            : editingUser
              ? 'Editar Usuário da Empresa'
              : 'Novo Usuário da Empresa'
        }
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
              {saving
                ? 'Salvando...'
                : editingUser
                  ? 'Salvar Alterações'
                  : isGlobalAdmin
                    ? 'Criar Administrador'
                    : 'Criar Usuário'}
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

          {isGlobalAdmin ? (
            <FormField
              label="Perfil de Acesso"
              hint="Contas criadas pela T.I. possuem exclusivamente o perfil de Administrador (T.I.), sem acesso a dados operacionais de empresas."
            >
              <div
                className="form-control"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  backgroundColor: 'var(--bg-muted, #f8fafc)',
                }}
              >
                <ShieldCheck size={16} />
                <span>{PERFIL_LABELS.ADMINISTRADOR}</span>
              </div>
            </FormField>
          ) : isEditingMainCompanyAccount ? (
            <FormField
              label="Perfil de Acesso"
              hint="O perfil da Conta Principal da Empresa não pode ser alterado."
            >
              <div
                className="form-control"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  backgroundColor: 'var(--bg-muted, #f8fafc)',
                }}
              >
                <ShieldCheck size={16} />
                <span>{PERFIL_LABELS.ADMIN_EMPRESA}</span>
              </div>
            </FormField>
          ) : (
            <FormField
              label="Perfil Operacional na Empresa"
              required
              error={fieldErrors.perfil}
              hint="Somente estes perfis operacionais acessam os dados de SST da sua empresa."
            >
              <select
                className="form-control"
                value={form.perfil}
                onChange={(e) => setForm({ ...form, perfil: e.target.value })}
              >
                {PERFIS_OPERACIONAIS_EMPRESA.map((k) => (
                  <option key={k} value={k}>
                    {PERFIL_LABELS[k] || k}
                  </option>
                ))}
              </select>
            </FormField>
          )}

          {requiresEmpresa && (
            <FormField
              label="Empresa Vinculada"
              error={fieldErrors.empresaId}
              hint="Vínculo exclusivo e automático à sua empresa."
            >
              <input
                type="text"
                className="form-control"
                value={user?.empresaNome || `Empresa #${user?.empresaId || ''}`}
                disabled
                readOnly
              />
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
