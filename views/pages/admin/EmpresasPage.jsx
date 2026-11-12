import React, { useState, useEffect, useCallback } from 'react';
import {
  Building2,
  Plus,
  Edit3,
  Power,
  AlertCircle,
  KeyRound,
  Copy,
  Check,
  Info,
  ShieldCheck,
} from 'lucide-react';
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
    adminEmail: '',
    adminNome: '',
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMsg, setErrorMsg] = useState('');
  const [saving, setSaving] = useState(false);
  const [createdAdminInfo, setCreatedAdminInfo] = useState(null);
  const [copied, setCopied] = useState(false);

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
      adminEmail: '',
      adminNome: '',
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
      adminEmail: emp.adminEmail || '',
      adminNome: emp.adminNome || '',
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const copyTempPassword = () => {
    if (createdAdminInfo?.senhaTemporaria) {
      navigator.clipboard?.writeText(createdAdminInfo.senhaTemporaria);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setErrorMsg('');

    const errs = {};
    if (!isValidCNPJ(form.cnpj)) {
      errs.cnpj = 'Informe um CNPJ válido com 14 dígitos.';
    }
    if (!editing && !form.adminEmail.trim()) {
      errs.adminEmail = 'Informe o e-mail da Conta Principal da Empresa.';
    }
    if (Object.keys(errs).length > 0) {
      setFieldErrors(errs);
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

      if (!editing) {
        payload.adminEmail = form.adminEmail.trim();
        payload.adminNome = form.adminNome.trim() || null;
      }

      if (editing) {
        await api.put(`/empresas/${editing.id}`, payload);
        toast.success('Empresa atualizada com sucesso.');
      } else {
        const res = await api.post('/empresas', payload);
        toast.success('Empresa cadastrada e Conta Principal vinculada com sucesso.');
        if (res?.adminSenhaTemporaria) {
          setCreatedAdminInfo({
            empresa: res.nomeFantasia || res.razaoSocial || payload.razaoSocial,
            nome: res.adminNome || form.adminNome.trim() || 'Administrador da Empresa',
            email: res.adminEmail || form.adminEmail.trim(),
            senhaTemporaria: res.adminSenhaTemporaria,
          });
          setCopied(false);
        }
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
      header: 'Conta Principal (Admin)',
      render: (e) =>
        e.adminEmail ? (
          <div>
            <div style={{ fontWeight: 500 }}>{e.adminNome || 'Conta Principal da Empresa'}</div>
            <div className="text-xs text-muted">{e.adminEmail}</div>
          </div>
        ) : (
          <span className="badge badge-warning">Sem vínculo</span>
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
            Cadastre e verifique empresas clientes. Cada empresa é obrigatoriamente vinculada a um e-mail de Conta Principal da Empresa.
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={openCreate}>
          <Plus size={18} /> Nova Empresa
        </button>
      </div>

      <div className="alert alert-info" style={{ marginBottom: '1rem' }}>
        <Info size={18} style={{ flexShrink: 0, marginTop: 2 }} />
        <div>
          <strong>Separação de responsabilidades:</strong> a T.I. (Administrador Global) cadastra empresas vinculando um e-mail de{' '}
          <strong>Conta Principal da Empresa</strong> e gerencia apenas outros administradores de T.I. Os usuários internos da empresa (
          <strong>RH</strong>, <strong>Médico do Trabalho</strong> e <strong>Especialista SST</strong>) são criados exclusivamente pela{' '}
          <strong>Conta Principal da Empresa</strong>, e apenas esses perfis operacionais têm acesso aos dados de SST da empresa.
        </div>
      </div>

      {createdAdminInfo && (
        <div className="temp-password-banner">
          <div className="temp-password-content">
            <div className="temp-password-header">
              <KeyRound size={20} />
              <strong>Conta Principal da Empresa — Credencial Temporária Gerada</strong>
            </div>
            <p>
              Empresa: <strong>{createdAdminInfo.empresa}</strong> ({createdAdminInfo.nome} &mdash;{' '}
              <code>{createdAdminInfo.email}</code>). Compartilhe a senha temporária com o responsável; ela também foi enfileirada por e-mail e deverá ser alterada no primeiro login.
            </p>
            <div className="temp-password-box">
              <code>{createdAdminInfo.senhaTemporaria}</code>
              <button type="button" className="btn btn-primary btn-sm" onClick={copyTempPassword}>
                {copied ? <Check size={16} /> : <Copy size={16} />}
                {copied ? 'Copiado!' : 'Copiar Senha'}
              </button>
            </div>
          </div>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => setCreatedAdminInfo(null)}
          >
            Fechar aviso
          </button>
        </div>
      )}

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

          <div
            style={{
              marginTop: '1.25rem',
              paddingTop: '1rem',
              borderTop: '1px solid var(--border-color, #e2e8f0)',
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                fontWeight: 600,
                marginBottom: '0.5rem',
              }}
            >
              <ShieldCheck size={18} />
              <span>Conta Principal da Empresa (Administrador Vinculado)</span>
            </div>

            {editing ? (
              <div className="alert alert-info" style={{ marginBottom: 0 }}>
                <Info size={16} style={{ flexShrink: 0, marginTop: 2 }} />
                <div>
                  <strong>E-mail vinculado:</strong>{' '}
                  {form.adminEmail ? (
                    <>
                      <code>{form.adminEmail}</code>
                      {form.adminNome ? ` (${form.adminNome})` : ''}
                    </>
                  ) : (
                    'Nenhum e-mail vinculado'
                  )}
                  <div className="text-xs" style={{ marginTop: 4 }}>
                    Somente esta Conta Principal cria e gerencia os usuários internos da empresa (RH, Médico do Trabalho e Especialista SST).
                  </div>
                </div>
              </div>
            ) : (
              <>
                <p className="text-xs text-muted" style={{ marginBottom: '0.75rem' }}>
                  Ao cadastrar a empresa, será criada automaticamente a Conta Principal (perfil Conta Principal da Empresa) vinculada a este e-mail. Somente ela poderá criar os usuários de RH, Médico do Trabalho e Especialista SST da empresa.
                </p>
                <div className="form-row">
                  <FormField
                    label="E-mail da Conta Principal"
                    required
                    error={fieldErrors.adminEmail}
                    hint="Receberá a senha temporária de acesso principal da empresa."
                  >
                    <input
                      type="email"
                      className="form-control"
                      placeholder="admin@empresa.com.br"
                      value={form.adminEmail}
                      onChange={(e) => setForm({ ...form, adminEmail: e.target.value })}
                      required
                    />
                  </FormField>

                  <FormField
                    label="Nome do Responsável (Conta Principal)"
                    error={fieldErrors.adminNome}
                  >
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Ex: Maria Oliveira"
                      value={form.adminNome}
                      onChange={(e) => setForm({ ...form, adminNome: e.target.value })}
                    />
                  </FormField>
                </div>
              </>
            )}
          </div>
        </form>
      </Modal>
    </div>
  );
}
