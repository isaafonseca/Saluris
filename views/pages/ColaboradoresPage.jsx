import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  UserPlus,
  Search,
  Edit3,
  Eye,
  AlertTriangle,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useEmpresa } from '../context/EmpresaContext';
import { useToast } from '../context/ToastContext';
import { formatCPF, isValidCPF, formatDate } from '../utils/formatters';
import { AdminEmpresaSelector } from '../components/ui/AdminEmpresaSelector';
import { Table } from '../components/ui/Table';
import { Modal } from '../components/ui/Modal';
import { Badge, ValidadeBadge } from '../components/ui/Badge';
import { FormField, Pagination, EmptyState } from '../components/ui/Common';

export function ColaboradoresPage() {
  const { isAdmin, isRh } = useAuth();
  const { activeEmpresaId } = useEmpresa();
  const toast = useToast();
  const navigate = useNavigate();

  const canWrite = isRh;

  const [colaboradores, setColaboradores] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [busca, setBusca] = useState('');
  const [buscaInput, setBuscaInput] = useState('');
  const [loading, setLoading] = useState(false);

  const [unidades, setUnidades] = useState([]);
  const [funcoes, setFuncoes] = useState([]);
  const [setores, setSetores] = useState([]);

  // Modal Criar / Editar
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({
    nome: '',
    cpf: '',
    dataNascimento: '',
    dataAdmissao: '',
    dataDemissao: '',
    email: '',
    unidadeId: '',
    funcaoId: '',
    setorId: '',
    ativo: true,
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMsg, setErrorMsg] = useState('');
  const [saving, setSaving] = useState(false);

  // Resultado após criar/editar colaborador (examesGerados + periodicidadeConfigurada)
  const [resultadoCriacao, setResultadoCriacao] = useState(null);

  const loadEstrutura = useCallback(async () => {
    try {
      const empParam = isAdmin && activeEmpresaId ? { empresaId: activeEmpresaId, ativos: true } : { ativos: true };
      const [uRes, fRes, sRes] = await Promise.all([
        api.get('/unidades', empParam),
        api.get('/funcoes', empParam),
        api.get('/setores', empParam),
      ]);
      setUnidades(Array.isArray(uRes) ? uRes : []);
      setFuncoes(Array.isArray(fRes) ? fRes : []);
      setSetores(Array.isArray(sRes) ? sRes : []);
    } catch (_) {}
  }, [isAdmin, activeEmpresaId]);

  const loadColaboradores = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get('/colaboradores', {
        empresaId: isAdmin && activeEmpresaId ? activeEmpresaId : undefined,
        busca: busca || undefined,
        page,
        size: 15,
      });
      setColaboradores(res?.content || []);
      setTotalPages(res?.totalPages || 0);
      setTotalElements(res?.totalElements || 0);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar colaboradores.');
    } finally {
      setLoading(false);
    }
  }, [isAdmin, activeEmpresaId, busca, page, toast]);

  useEffect(() => {
    loadEstrutura();
  }, [loadEstrutura]);

  useEffect(() => {
    loadColaboradores();
  }, [loadColaboradores]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    setBusca(buscaInput.trim());
  };

  const openCreateModal = () => {
    setEditing(null);
    const today = new Date().toISOString().slice(0, 10);
    setForm({
      nome: '',
      cpf: '',
      dataNascimento: '',
      dataAdmissao: today,
      dataDemissao: '',
      email: '',
      unidadeId: unidades[0]?.id ? String(unidades[0].id) : '',
      funcaoId: funcoes[0]?.id ? String(funcoes[0].id) : '',
      setorId: setores[0]?.id ? String(setores[0].id) : '',
      ativo: true,
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const openEditModal = (c) => {
    setEditing(c);
    setForm({
      nome: c.nome || '',
      cpf: formatCPF(c.cpf || ''),
      dataNascimento: c.dataNascimento || '',
      dataAdmissao: c.dataAdmissao || '',
      dataDemissao: c.dataDemissao || '',
      email: c.email || '',
      unidadeId: c.unidadeId ? String(c.unidadeId) : '',
      funcaoId: c.funcaoId ? String(c.funcaoId) : '',
      setorId: c.setorId ? String(c.setorId) : '',
      ativo: c.ativo !== false,
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setErrorMsg('');

    const errs = {};
    if (!form.nome.trim()) errs.nome = 'Informe o nome completo.';
    if (!isValidCPF(form.cpf)) {
      errs.cpf = 'CPF inválido (verifique os dígitos verificadores).';
    }
    if (!form.dataAdmissao) errs.dataAdmissao = 'Informe a data de admissão.';
    if (!form.unidadeId) errs.unidadeId = 'Selecione a unidade.';
    if (!form.funcaoId) errs.funcaoId = 'Selecione a função.';
    if (!form.setorId) errs.setorId = 'Selecione o setor.';

    if (Object.keys(errs).length > 0) {
      setFieldErrors(errs);
      return;
    }

    setSaving(true);
    try {
      const payload = {
        nome: form.nome.trim(),
        cpf: form.cpf,
        dataNascimento: form.dataNascimento || null,
        dataAdmissao: form.dataAdmissao,
        email: form.email.trim() || null,
        unidadeId: Number(form.unidadeId),
        funcaoId: Number(form.funcaoId),
        setorId: Number(form.setorId),
      };

      if (editing) {
        const res = await api.put(`/colaboradores/${editing.id}`, {
          ...payload,
          dataDemissao: form.dataDemissao || null,
          ativo: form.ativo,
        });
        toast.success('Colaborador atualizado com sucesso.');
        setResultadoCriacao(res);
      } else {
        const res = await api.post('/colaboradores', payload);
        toast.success('Colaborador cadastrado com sucesso.');
        setResultadoCriacao(res);
      }
      setModalOpen(false);
      loadColaboradores();
    } catch (err) {
      setErrorMsg(err.detail || 'Erro ao salvar colaborador.');
      if (err.erros) setFieldErrors(err.erros);
    } finally {
      setSaving(false);
    }
  };

  const columns = [
    {
      header: 'Colaborador / CPF',
      render: (c) => (
        <div>
          <div className="font-semibold">{c.nome}</div>
          <div className="text-muted text-xs mono">{formatCPF(c.cpf)}</div>
        </div>
      ),
    },
    {
      header: 'Unidade',
      render: (c) => c.unidadeNome || '—',
    },
    {
      header: 'Função / Setor',
      render: (c) => (
        <div>
          <div className="font-semibold">{c.funcaoNome || '—'}</div>
          <div className="text-xs text-muted">{c.setorNome || '—'}</div>
        </div>
      ),
    },
    {
      header: 'Admissão',
      render: (c) => formatDate(c.dataAdmissao),
    },
    {
      header: 'Conformidade de Exames',
      render: (c) => <ValidadeBadge status={c.statusConformidade} />,
    },
    {
      header: 'Vínculo',
      render: (c) => (
        <Badge variant={c.ativo ? 'success' : 'neutral'}>
          {c.ativo ? 'Ativo' : 'Desligado'}
        </Badge>
      ),
    },
    {
      header: 'Ações',
      render: (c) => (
        <div className="table-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => navigate(`/colaboradores/${c.id}`)}
          >
            <Eye size={15} /> Prontuário / Exames
          </button>
          {canWrite && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => openEditModal(c)}
            >
              <Edit3 size={15} /> Editar
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Colaboradores</h2>
          <p className="page-subtitle">
            Gerencie o quadro de colaboradores e acompanhe o status de conformidade ocupacional individual.
          </p>
        </div>
        {canWrite && (
          <button type="button" className="btn btn-primary" onClick={openCreateModal}>
            <UserPlus size={18} /> Novo Colaborador
          </button>
        )}
      </div>

      <AdminEmpresaSelector />

      {/* Banner de resultado após criar/atualizar colaborador */}
      {resultadoCriacao && (
        <div
          className={`result-banner ${
            resultadoCriacao.periodicidadeConfigurada === false ? 'warning' : 'success'
          }`}
        >
          <div className="result-banner-header">
            {resultadoCriacao.periodicidadeConfigurada === false ? (
              <AlertTriangle size={20} />
            ) : (
              <CheckCircle2 size={20} />
            )}
            <strong>
              Colaborador: {resultadoCriacao.colaborador?.nome} (
              {resultadoCriacao.colaborador?.funcaoNome} • {resultadoCriacao.colaborador?.setorNome})
            </strong>
            <button
              type="button"
              className="btn-link-sm"
              style={{ marginLeft: 'auto' }}
              onClick={() => setResultadoCriacao(null)}
            >
              Fechar
            </button>
          </div>
          {resultadoCriacao.periodicidadeConfigurada === false ? (
            <p className="text-sm" style={{ marginTop: 6 }}>
              <strong>Atenção:</strong> Periodicidade não configurada para esta função + setor; o Especialista SST foi notificado.
            </p>
          ) : (
            <div className="text-sm" style={{ marginTop: 6 }}>
              {resultadoCriacao.examesGerados?.length > 0 ? (
                <>
                  <span>
                    Foram gerados automaticamente{' '}
                    <strong>{resultadoCriacao.examesGerados.length}</strong> exame(s) para este colaborador:
                  </span>
                  <ul className="compact-list">
                    {resultadoCriacao.examesGerados.map((ex) => (
                      <li key={ex.id}>
                        <strong>{ex.tipoExameNome}</strong> — Vencimento:{' '}
                        {formatDate(ex.dataVencimento)} ({ex.periodicidadeMeses} meses)
                      </li>
                    ))}
                  </ul>
                </>
              ) : (
                <span>Periodicidade configurada e vínculos de exames atualizados.</span>
              )}
            </div>
          )}
        </div>
      )}

      {/* Busca */}
      <div className="card filter-card">
        <form onSubmit={handleSearchSubmit} className="filter-row">
          <div className="filter-item" style={{ flex: 2 }}>
            <label className="form-label">Buscar por Nome ou CPF</label>
            <div className="input-with-icon">
              <Search size={17} className="input-icon" />
              <input
                type="text"
                className="form-control"
                placeholder="Digite o nome ou CPF do colaborador..."
                value={buscaInput}
                onChange={(e) => setBuscaInput(e.target.value)}
              />
            </div>
          </div>
          <div className="filter-actions">
            <button type="submit" className="btn btn-primary">
              Buscar
            </button>
            {busca && (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  setBuscaInput('');
                  setBusca('');
                  setPage(0);
                }}
              >
                Limpar
              </button>
            )}
          </div>
        </form>
      </div>

      {/* Tabela */}
      <div className="card">
        <Table
          columns={columns}
          data={colaboradores}
          loading={loading}
          emptyTitle="Nenhum colaborador encontrado"
        />
        <Pagination
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={setPage}
        />
      </div>

      {/* Modal Criar / Editar Colaborador */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? 'Editar Colaborador' : 'Cadastrar Novo Colaborador'}
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
            <button type="submit" form="colab-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Salvando...' : editing ? 'Salvar Alterações' : 'Cadastrar Colaborador'}
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

        <form id="colab-form" onSubmit={handleSave}>
          <div className="form-row-2">
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
              label="CPF"
              required
              error={fieldErrors.cpf}
              hint="Validação de dígitos verificadores automática."
            >
              <input
                type="text"
                className="form-control mono"
                placeholder="000.000.000-00"
                value={form.cpf}
                onChange={(e) => setForm({ ...form, cpf: formatCPF(e.target.value) })}
                required
              />
            </FormField>
          </div>

          <div className="form-row-3">
            <FormField label="Data de Nascimento" error={fieldErrors.dataNascimento}>
              <input
                type="date"
                className="form-control"
                value={form.dataNascimento}
                onChange={(e) => setForm({ ...form, dataNascimento: e.target.value })}
              />
            </FormField>

            <FormField label="Data de Admissão" required error={fieldErrors.dataAdmissao}>
              <input
                type="date"
                className="form-control"
                value={form.dataAdmissao}
                onChange={(e) => setForm({ ...form, dataAdmissao: e.target.value })}
                required
              />
            </FormField>

            <FormField label="E-mail (opcional)" error={fieldErrors.email}>
              <input
                type="email"
                className="form-control"
                placeholder="colaborador@empresa.com.br"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </FormField>
          </div>

          <div className="form-row-3">
            <FormField label="Unidade" required error={fieldErrors.unidadeId}>
              <select
                className="form-control"
                value={form.unidadeId}
                onChange={(e) => setForm({ ...form, unidadeId: e.target.value })}
                required
              >
                <option value="">-- Selecione --</option>
                {unidades.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.nome}
                  </option>
                ))}
              </select>
            </FormField>

            <FormField label="Função / Cargo" required error={fieldErrors.funcaoId}>
              <select
                className="form-control"
                value={form.funcaoId}
                onChange={(e) => setForm({ ...form, funcaoId: e.target.value })}
                required
              >
                <option value="">-- Selecione --</option>
                {funcoes.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.nome}
                  </option>
                ))}
              </select>
            </FormField>

            <FormField label="Setor / Ambiente" required error={fieldErrors.setorId}>
              <select
                className="form-control"
                value={form.setorId}
                onChange={(e) => setForm({ ...form, setorId: e.target.value })}
                required
              >
                <option value="">-- Selecione --</option>
                {setores.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nome}
                  </option>
                ))}
              </select>
            </FormField>
          </div>

          {editing && (
            <div className="form-row-2" style={{ marginTop: '0.5rem' }}>
              <FormField label="Data de Demissão (se desligado)" error={fieldErrors.dataDemissao}>
                <input
                  type="date"
                  className="form-control"
                  value={form.dataDemissao}
                  onChange={(e) => setForm({ ...form, dataDemissao: e.target.value })}
                />
              </FormField>

              <FormField label="Situação do Vínculo">
                <label className="checkbox-field" style={{ marginTop: 8 }}>
                  <input
                    type="checkbox"
                    checked={form.ativo}
                    onChange={(e) => setForm({ ...form, ativo: e.target.checked })}
                  />
                  <span>Colaborador Ativo</span>
                </label>
              </FormField>
            </div>
          )}
        </form>
      </Modal>
    </div>
  );
}
