import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  Bot,
  Send,
  Plus,
  Trash2,
  BookOpen,
  ShieldAlert,
  MessageSquare,
  Sparkles,
} from 'lucide-react';
import { api } from '../api/client';
import { useToast } from '../context/ToastContext';
import { formatDateTime } from '../utils/formatters';
import { Badge, ClassificacaoIABadge } from '../components/ui/Badge';
import { ConfirmDialog } from '../components/ui/Common';

export function AgenteIAPage() {
  const toast = useToast();

  const [iaStatus, setIaStatus] = useState({ modo: 'OFFLINE', modelo: '' });
  const [conversas, setConversas] = useState([]);
  const [activeConversaId, setActiveConversaId] = useState(null);
  const [mensagens, setMensagens] = useState([]);
  const [pergunta, setPergunta] = useState('');
  const [loadingMsgs, setLoadingMsgs] = useState(false);
  const [sending, setSending] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState({ open: false, conversa: null });

  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [mensagens, sending]);

  useEffect(() => {
    api
      .get('/ia/status')
      .then((res) => {
        if (res) setIaStatus(res);
      })
      .catch(() => {});
  }, []);

  const loadConversas = useCallback(async () => {
    try {
      const list = await api.get('/ia/conversas');
      setConversas(Array.isArray(list) ? list : []);
    } catch (_) {
      setConversas([]);
    }
  }, []);

  useEffect(() => {
    loadConversas();
  }, [loadConversas]);

  const handleSelectConversa = async (id) => {
    setActiveConversaId(id);
    setLoadingMsgs(true);
    try {
      const msgs = await api.get(`/ia/conversas/${id}/mensagens`);
      setMensagens(Array.isArray(msgs) ? msgs : []);
    } catch (err) {
      toast.error(err.detail || 'Erro ao carregar histórico da conversa.');
    } finally {
      setLoadingMsgs(false);
    }
  };

  const handleNewChat = () => {
    setActiveConversaId(null);
    setMensagens([]);
    setPergunta('');
  };

  const handleConfirmDelete = async () => {
    if (!deleteDialog.conversa) return;
    try {
      await api.delete(`/ia/conversas/${deleteDialog.conversa.id}`);
      toast.success('Conversa excluída.');
      if (activeConversaId === deleteDialog.conversa.id) {
        handleNewChat();
      }
      setDeleteDialog({ open: false, conversa: null });
      loadConversas();
    } catch (err) {
      toast.error(err.detail || 'Erro ao excluir conversa.');
    }
  };

  const handleSend = async (e) => {
    e.preventDefault();
    const text = pergunta.trim();
    if (!text || sending) return;
    if (text.length > 2000) {
      toast.warning('A pergunta deve ter no máximo 2000 caracteres.');
      return;
    }

    setPergunta('');
    setSending(true);

    // Mensagem otimista do usuário na tela
    const tempUserMsg = {
      id: `temp-${Date.now()}`,
      papel: 'USUARIO',
      conteudo: text,
      criadoEm: new Date().toISOString(),
    };
    setMensagens((prev) => [...prev, tempUserMsg]);

    try {
      const res = await api.post('/ia/perguntar', {
        conversaId: activeConversaId || undefined,
        pergunta: text,
      });

      if (res) {
        setActiveConversaId(res.conversaId);
        setMensagens((prev) => {
          const withoutTemp = prev.filter((m) => m.id !== tempUserMsg.id);
          return [...withoutTemp, res.pergunta, res.resposta].filter(Boolean);
        });
        loadConversas();
      }
    } catch (err) {
      toast.error(err.detail || 'Erro ao consultar o assistente normativo.');
      setMensagens((prev) => prev.filter((m) => m.id !== tempUserMsg.id));
      setPergunta(text);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="page-container ia-page">
      <div className="page-header">
        <div>
          <h2>Assistente Normativo de SST (Saluris IA)</h2>
          <p className="page-subtitle">
            Tire dúvidas técnicas com base nas Normas Regulamentadoras (NR-01, NR-07, NR-09, NR-15) e diretrizes de Saúde e Segurança do Trabalho.
          </p>
        </div>
        <div className="ia-status-pill">
          <Sparkles size={16} />
          <span>Modo:</span>
          <Badge variant={iaStatus.modo === 'OPENAI' ? 'success' : 'warning'}>
            {iaStatus.modo || 'OFFLINE'}
          </Badge>
          {iaStatus.modelo && <span className="text-xs text-muted">({iaStatus.modelo})</span>}
        </div>
      </div>

      {/* Aviso fixo obrigatório */}
      <div className="ia-disclaimer-banner">
        <ShieldAlert size={18} />
        <span>
          <strong>Aviso Importante:</strong> O assistente não emite laudos, diagnósticos ou pareceres médicos. Todas as respostas têm caráter estritamente informativo e baseiam-se nas Normas Regulamentadoras (NRs).
        </span>
      </div>

      <div className="ia-layout">
        {/* Lista de conversas à esquerda */}
        <aside className="ia-sidebar card">
          <div className="ia-sidebar-top">
            <button
              type="button"
              className="btn btn-primary btn-block"
              onClick={handleNewChat}
            >
              <Plus size={17} /> Nova Conversa
            </button>
          </div>
          <div className="ia-conversa-list">
            {conversas.length === 0 ? (
              <div className="ia-empty-conversas">Nenhuma conversa anterior.</div>
            ) : (
              conversas.map((c) => (
                <div
                  key={c.id}
                  className={`ia-conversa-item ${activeConversaId === c.id ? 'active' : ''}`}
                  onClick={() => handleSelectConversa(c.id)}
                >
                  <MessageSquare size={16} className="ia-conversa-icon" />
                  <div className="ia-conversa-info">
                    <div className="ia-conversa-title">{c.titulo || 'Conversa sobre SST'}</div>
                    <div className="ia-conversa-date">{formatDateTime(c.atualizadoEm)}</div>
                  </div>
                  <button
                    type="button"
                    className="ia-delete-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      setDeleteDialog({ open: true, conversa: c });
                    }}
                    title="Excluir conversa"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              ))
            )}
          </div>
        </aside>

        {/* Área do Chat */}
        <section className="ia-chat-panel card">
          <div className="ia-messages-area">
            {loadingMsgs ? (
              <div className="ia-welcome">Carregando mensagens...</div>
            ) : mensagens.length === 0 ? (
              <div className="ia-welcome">
                <div className="ia-welcome-icon">
                  <Bot size={40} />
                </div>
                <h3>Como posso ajudar na gestão de SST hoje?</h3>
                <p>
                  Faça perguntas sobre prazos do PCMSO (NR-07), gerenciamento de riscos no PGR (NR-01), avaliação de exposições ocupacionais (NR-09) ou atividades insalubres (NR-15).
                </p>
                <div className="ia-suggestions">
                  {[
                    'Qual a periodicidade máxima dos exames periódicos segundo a NR-07?',
                    'Como o PGR da NR-01 se relaciona com a elaboração do PCMSO?',
                    'Quais são os requisitos da NR-07 para exames de retorno ao trabalho?',
                  ].map((sug, i) => (
                    <button
                      key={i}
                      type="button"
                      className="ia-suggestion-chip"
                      onClick={() => setPergunta(sug)}
                    >
                      {sug}
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              mensagens.map((m) => {
                const isUser = m.papel === 'USUARIO';
                return (
                  <div
                    key={m.id}
                    className={`ia-msg-row ${isUser ? 'user-row' : 'assistant-row'}`}
                  >
                    <div className={`ia-msg-bubble ${isUser ? 'user-bubble' : 'assistant-bubble'}`}>
                      {!isUser && (
                        <div className="ia-msg-meta">
                          <span className="ia-assistant-name">
                            <Bot size={15} /> Assistente Saluris IA
                          </span>
                          {m.classificacao && (
                            <ClassificacaoIABadge classificacao={m.classificacao} />
                          )}
                        </div>
                      )}

                      <div className="ia-msg-text">{m.conteudo}</div>

                      {!isUser && Array.isArray(m.fontes) && m.fontes.length > 0 && (
                        <div className="ia-sources-box">
                          <div className="ia-sources-title">
                            <BookOpen size={14} /> Fontes Normativas Consultadas:
                          </div>
                          <div className="ia-sources-list">
                            {m.fontes.map((f, idx) => (
                              <span key={f.trechoId || idx} className="ia-source-tag">
                                <strong>{f.norma}</strong>
                                {f.item ? ` • Item ${f.item}` : ''}
                                {f.titulo ? ` — ${f.titulo}` : ''}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}

                      <div className="ia-msg-time">{formatDateTime(m.criadoEm)}</div>
                    </div>
                  </div>
                );
              })
            )}

            {sending && (
              <div className="ia-msg-row assistant-row">
                <div className="ia-msg-bubble assistant-bubble">
                  <span className="text-muted text-sm">
                    Consultando base normativa das NRs e elaborando resposta...
                  </span>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          <form onSubmit={handleSend} className="ia-input-bar">
            <textarea
              className="form-control ia-textarea"
              rows={2}
              maxLength={2000}
              placeholder="Digite sua dúvida sobre as Normas Regulamentadoras (NR-01, NR-07, NR-09, NR-15)..."
              value={pergunta}
              onChange={(e) => setPergunta(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleSend(e);
                }
              }}
              disabled={sending}
            />
            <button
              type="submit"
              className="btn btn-primary ia-send-btn"
              disabled={sending || !pergunta.trim()}
            >
              <Send size={18} />
              <span>Enviar</span>
            </button>
          </form>
        </section>
      </div>

      <ConfirmDialog
        isOpen={deleteDialog.open}
        onClose={() => setDeleteDialog({ open: false, conversa: null })}
        onConfirm={handleConfirmDelete}
        title="Excluir conversa"
        message={`Deseja excluir permanentemente a conversa "${
          deleteDialog.conversa?.titulo || 'selecionada'
        }"?`}
        confirmLabel="Excluir"
        variant="danger"
      />
    </div>
  );
}
