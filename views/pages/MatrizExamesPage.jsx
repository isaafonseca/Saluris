import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Plus,
  Edit3,
  Trash2,
  CalendarClock,
  Users,
  AlertCircle,
  Filter,
} from 'lucide-react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useEmpresa } from '../context/EmpresaContext';
import { useToast } from '../context/ToastContext';
import { formatDateTime } from '../utils/formatters';
import { AdminEmpresaSelector } from '../components/ui/AdminEmpresaSelector';
import { Modal } from '../components/ui/Modal';
import { Badge } from '../components/ui/Badge';
import {
  ClearFiltersButton,
  FormField,
  Loading,
  EmptyState,
  ConfirmDialog,
} from '../components/ui/Common';

export function MatrizExamesPage() {
  const { isAdmin, isEspecialista } = useAuth();
  const { activeEmpresaId } = useEmpresa();
  const toast = useToast();

  const canEdit = isEspecialista;

  const [matriz, setMatriz] = useState([]);
  const [funcoes, setFuncoes] = useState([]);
  const [setores, setSetores] = useState([]);
  const [tiposExame, setTiposExame] = useState([]);
  const [loading, setLoading] = useState(false);

  const [filtroFuncaoIds, setFiltroFuncaoIds] = useState([]);
  const [filtroSetorIds, setFiltroSetorIds] = useState([]);

  // Modal Criar / Editar
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [form, setForm] = useState({
    funcaoId: '',
    setorId: '',
    tipoExameId: '',
    periodicidadeMeses: 12,
    obrigatorio: true,
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMsg, setErrorMsg] = useState('');
  const [saving, setSaving] = useState(false);

  // Tratamento de 409 com existenteId -> Confirmar sobrescrita (PUT)
  const [conflictDialog, setConflictDialog] = useState({
    open: false,
    existenteId: null,
    payload: null,
  });

  // Confirmação de exclusão
  const [deleteDialog, setDeleteDialog] = useState({ open: false, item: null });

  // Resultado de colaboradores afetados após salvar
  const [affectedResult, setAffectedResult] = useState(null);

  const loadSelects = useCallback(async () => {
    try {
      const empParam = isAdmin && activeEmpresaId ? { empresaId: activeEmpresaId, ativos: true } : { ativos: true };
      const [fRes, sRes, tRes] = await Promise.all([
        api.get('/funcoes', empParam),
        api.get('/setores', empParam),
        api.get('/tipos-exame', { ativos: true }),
      ]);
      setFuncoes(Array.isArray(fRes) ? fRes : []);
      setSetores(Array.isArray(sRes) ? sRes : []);
      setTiposExame(Array.isArray(tRes) ? tRes : []);
    } catch (_) {}
  }, [isAdmin, activeEmpresaId]);

  const loadMatriz = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.get('/matriz-exames', {
        empresaId: isAdmin && activeEmpresaId ? activeEmpresaId : undefined,
      });
      setMatriz(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar matriz de periodicidade.');
    } finally {
      setLoading(false);
    }
  }, [isAdmin, activeEmpresaId, toast]);

  useEffect(() => {
    loadSelects();
  }, [loadSelects]);

  useEffect(() => {
    loadMatriz();
  }, [loadMatriz]);

  const matrizFiltrada = useMemo(
    () =>
      matriz.filter(
        (item) =>
          (!filtroFuncaoIds.length || filtroFuncaoIds.includes(String(item.funcaoId))) &&
          (!filtroSetorIds.length || filtroSetorIds.includes(String(item.setorId)))
      ),
    [matriz, filtroFuncaoIds, filtroSetorIds]
  );

  // Agrupar itens por "Função + Setor"
  const groupedMatriz = useMemo(() => {
    const groups = new Map();
    matrizFiltrada.forEach((item) => {
      const key = `${item.funcaoId}-${item.setorId}`;
      if (!groups.has(key)) {
        groups.set(key, {
          key,
          funcaoNome: item.funcaoNome,
          setorNome: item.setorNome,
          items: [],
        });
      }
      groups.get(key).items.push(item);
    });
    return Array.from(groups.values());
  }, [matrizFiltrada]);

  const toggleFiltro = (setFiltro, id) => {
    const valor = String(id);
    setFiltro((selecionados) =>
      selecionados.includes(valor)
        ? selecionados.filter((selecionado) => selecionado !== valor)
        : [...selecionados, valor]
    );
  };

  const openCreateModal = () => {
    setEditingItem(null);
    setForm({
      funcaoId: funcoes[0]?.id ? String(funcoes[0].id) : '',
      setorId: setores[0]?.id ? String(setores[0].id) : '',
      tipoExameId: tiposExame[0]?.id ? String(tiposExame[0].id) : '',
      periodicidadeMeses: 12,
      obrigatorio: true,
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const openEditModal = (item) => {
    setEditingItem(item);
    setForm({
      funcaoId: String(item.funcaoId),
      setorId: String(item.setorId),
      tipoExameId: String(item.tipoExameId),
      periodicidadeMeses: item.periodicidadeMeses,
      obrigatorio: !!item.obrigatorio,
    });
    setFieldErrors({});
    setErrorMsg('');
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setErrorMsg('');

    const meses = Number(form.periodicidadeMeses);
    if (isNaN(meses) || meses < 6 || meses > 24) {
      setFieldErrors({
        periodicidadeMeses: 'A periodicidade deve ser um valor entre 6 e 24 meses.',
      });
      return;
    }

    if (!editingItem && (!form.funcaoId || !form.setorId || !form.tipoExameId)) {
      setErrorMsg('Selecione a Função, o Setor e o Tipo de Exame.');
      return;
    }

    setSaving(true);
    try {
      if (editingItem) {
        const res = await api.put(`/matriz-exames/${editingItem.id}`, {
          periodicidadeMeses: meses,
          obrigatorio: form.obrigatorio,
        });
        toast.success('Periodicidade atualizada com sucesso.');
        setAffectedResult(res);
      } else {
        const res = await api.post('/matriz-exames', {
          funcaoId: Number(form.funcaoId),
          setorId: Number(form.setorId),
          tipoExameId: Number(form.tipoExameId),
          periodicidadeMeses: meses,
          obrigatorio: form.obrigatorio,
        });
        toast.success('Regra adicionada à matriz de periodicidade.');
        setAffectedResult(res);
      }
      setModalOpen(false);
      loadMatriz();
    } catch (err) {
      // Verifica 409 com existenteId
      const existenteId = err.raw?.existenteId || err.erros?.existenteId;
      if (err.status === 409 && existenteId) {
        setConflictDialog({
          open: true,
          existenteId,
          payload: {
            periodicidadeMeses: meses,
            obrigatorio: form.obrigatorio,
          },
        });
      } else {
        setErrorMsg(err.detail || 'Não foi possível salvar a configuração.');
        if (err.erros) setFieldErrors(err.erros);
      }
    } finally {
      setSaving(false);
    }
  };

  const handleConfirmOverwrite = async () => {
    if (!conflictDialog.existenteId) return;
    setSaving(true);
    try {
      const res = await api.put(
        `/matriz-exames/${conflictDialog.existenteId}`,
        conflictDialog.payload
      );
      toast.success('Configuração sobrescrita com sucesso!');
      setAffectedResult(res);
      setConflictDialog({ open: false, existenteId: null, payload: null });
      setModalOpen(false);
      loadMatriz();
    } catch (err) {
      toast.error(err.detail || 'Erro ao sobrescrever configuração.');
    } finally {
      setSaving(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!deleteDialog.item) return;
    setSaving(true);
    try {
      await api.delete(`/matriz-exames/${deleteDialog.item.id}`);
      toast.success('Configuração removida da matriz.');
      setDeleteDialog({ open: false, item: null });
      loadMatriz();
    } catch (err) {
      toast.error(err.detail || 'Erro ao excluir configuração.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Matriz de Periodicidade de Exames (Função + Setor)</h2>
          <p className="page-subtitle">
            Parametrize os intervalos de renovação de exames (6 a 24 meses) de acordo com o risco ocupacional de cada combinação de Função e Setor (NR-07 / PCMSO).
          </p>
        </div>
        {canEdit && (
          <button type="button" className="btn btn-primary" onClick={openCreateModal}>
            <Plus size={18} /> Nova Regra de Periodicidade
          </button>
        )}
      </div>

      <AdminEmpresaSelector />

      {/* Aviso de colaboradores afetados após salvar */}
      {affectedResult && (
        <div className="affected-banner">
          <div className="affected-header">
            <Users size={20} />
            <strong>
              Regra salva: {affectedResult.configuracao?.tipoExameNome} ({affectedResult.configuracao?.periodicidadeMeses} meses) — {affectedResult.configuracao?.funcaoNome} / {affectedResult.configuracao?.setorNome}
            </strong>
            <button
              type="button"
              className="btn-link-sm"
              style={{ marginLeft: 'auto' }}
              onClick={() => setAffectedResult(null)}
            >
              Fechar
            </button>
          </div>
          <p className="text-sm" style={{ marginTop: 4 }}>
            {affectedResult.colaboradoresAfetados?.length > 0 ? (
              <>
                Vencimentos recalculados automaticamente para{' '}
                <strong>{affectedResult.colaboradoresAfetados.length}</strong> colaborador(es):{' '}
                {affectedResult.colaboradoresAfetados.map((c) => c.nome).join(', ')}.
              </>
            ) : (
              'Nenhum colaborador ativo alocado nesta combinação no momento.'
            )}
          </p>
        </div>
      )}

      {/* Filtros */}
      <div className="card filter-card">
        <div className="filter-row">
          <div className="filter-item">
            <label className="form-label">
              <Filter size={14} /> Filtrar por Função
            </label>
            <div className="filter-checkbox-list">
              {funcoes.map((f) => (
                <label key={f.id} className="checkbox-field">
                  <input
                    type="checkbox"
                    checked={filtroFuncaoIds.includes(String(f.id))}
                    onChange={() => toggleFiltro(setFiltroFuncaoIds, f.id)}
                  />
                  <span>{f.nome}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="filter-item">
            <label className="form-label">Filtrar por Setor</label>
            <div className="filter-checkbox-list">
              {setores.map((s) => (
                <label key={s.id} className="checkbox-field">
                  <input
                    type="checkbox"
                    checked={filtroSetorIds.includes(String(s.id))}
                    onChange={() => toggleFiltro(setFiltroSetorIds, s.id)}
                  />
                  <span>{s.nome}</span>
                </label>
              ))}
            </div>
          </div>

        </div>
        <ClearFiltersButton
          active={Boolean(filtroFuncaoIds.length || filtroSetorIds.length)}
          onClick={() => {
            setFiltroFuncaoIds([]);
            setFiltroSetorIds([]);
          }}
        />
      </div>

      {/* Listagem agrupada por Função + Setor */}
      {loading ? (
        <div className="card">
          <Loading />
        </div>
      ) : groupedMatriz.length === 0 ? (
        <div className="card">
          <EmptyState
            icon={CalendarClock}
            title="Nenhuma regra de periodicidade configurada"
            description={
              canEdit
                ? 'Clique em "Nova Regra de Periodicidade" para vincular exames a uma combinação de Função + Setor.'
                : 'O Especialista SST ainda não cadastrou regras para esta empresa.'
            }
          />
        </div>
      ) : (
        <div className="matriz-groups">
          {groupedMatriz.map((group) => (
            <div key={group.key} className="card matriz-group-card">
              <div className="matriz-group-header">
                <div>
                  <span className="matriz-group-tag">Função + Setor</span>
                  <h4>
                    {group.funcaoNome} <span className="text-muted">•</span> {group.setorNome}
                  </h4>
                </div>
                <Badge variant="info">{group.items.length} exame(s)</Badge>
              </div>
              <div className="table-responsive">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Tipo de Exame Ocupacional</th>
                      <th>Periodicidade</th>
                      <th>Obrigatoriedade</th>
                      <th>Configurado por</th>
                      <th>Atualizado em</th>
                      {canEdit && <th style={{ width: 170 }}>Ações</th>}
                    </tr>
                  </thead>
                  <tbody>
                    {group.items.map((item) => (
                      <tr key={item.id}>
                        <td className="font-semibold">{item.tipoExameNome}</td>
                        <td>
                          <Badge variant="info">{item.periodicidadeMeses} meses</Badge>
                        </td>
                        <td>
                          <Badge variant={item.obrigatorio ? 'success' : 'neutral'}>
                            {item.obrigatorio ? 'Obrigatório' : 'Complementar / Opcional'}
                          </Badge>
                        </td>
                        <td>{item.configuradoPorNome || '—'}</td>
                        <td>{formatDateTime(item.atualizadoEm)}</td>
                        {canEdit && (
                          <td>
                            <div className="table-actions">
                              <button
                                type="button"
                                className="btn btn-secondary btn-sm"
                                onClick={() => openEditModal(item)}
                              >
                                <Edit3 size={14} /> Editar
                              </button>
                              <button
                                type="button"
                                className="btn btn-outline-danger btn-sm"
                                onClick={() => setDeleteDialog({ open: true, item })}
                              >
                                <Trash2 size={14} />
                              </button>
                            </div>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal Criar / Editar */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={
          editingItem
            ? 'Editar Periodicidade na Matriz'
            : 'Configurar Periodicidade (Função + Setor)'
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
            <button type="submit" form="matriz-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Salvando...' : 'Salvar Configuração'}
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

        <form id="matriz-form" onSubmit={handleSave}>
          <div className="form-row-2">
            <FormField label="Função / Cargo" required error={fieldErrors.funcaoId}>
              <select
                className="form-control"
                value={form.funcaoId}
                onChange={(e) => setForm({ ...form, funcaoId: e.target.value })}
                disabled={!!editingItem}
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
                disabled={!!editingItem}
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

          <FormField label="Tipo de Exame Ocupacional" required error={fieldErrors.tipoExameId}>
            <select
              className="form-control"
              value={form.tipoExameId}
              onChange={(e) => setForm({ ...form, tipoExameId: e.target.value })}
              disabled={!!editingItem}
              required
            >
              <option value="">-- Selecione --</option>
              {tiposExame.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.nome}
                </option>
              ))}
            </select>
          </FormField>

          <div className="form-row-2">
            <FormField
              label="Periodicidade (em meses)"
              required
              error={fieldErrors.periodicidadeMeses}
              hint="Valor permitido entre 6 e 24 meses (ex: 6, 12, 18, 24)."
            >
              <input
                type="number"
                min={6}
                max={24}
                className="form-control"
                value={form.periodicidadeMeses}
                onChange={(e) => setForm({ ...form, periodicidadeMeses: e.target.value })}
                required
              />
            </FormField>

            <FormField label="Obrigatoriedade" required>
              <label className="checkbox-field" style={{ marginTop: 8 }}>
                <input
                  type="checkbox"
                  checked={form.obrigatorio}
                  onChange={(e) => setForm({ ...form, obrigatorio: e.target.checked })}
                />
                <span>Exame obrigatório para esta Função + Setor</span>
              </label>
            </FormField>
          </div>
        </form>
      </Modal>

      {/* Dialog 409 - Sobrescrever combinação existente */}
      <ConfirmDialog
        isOpen={conflictDialog.open}
        onClose={() => setConflictDialog({ open: false, existenteId: null, payload: null })}
        onConfirm={handleConfirmOverwrite}
        title="Combinação já configurada"
        message="Já existe configuração para esta combinação. Deseja sobrescrever?"
        confirmLabel="Sim, sobrescrever"
        loading={saving}
      />

      {/* Dialog Exclusão */}
      <ConfirmDialog
        isOpen={deleteDialog.open}
        onClose={() => setDeleteDialog({ open: false, item: null })}
        onConfirm={handleConfirmDelete}
        title="Remover regra da matriz"
        message={`Deseja realmente excluir a regra do exame "${deleteDialog.item?.tipoExameNome}" para ${deleteDialog.item?.funcaoNome} + ${deleteDialog.item?.setorNome}?`}
        confirmLabel="Excluir"
        variant="danger"
        loading={saving}
      />
    </div>
  );
}
