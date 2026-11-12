import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { KeyRound, AlertCircle, ShieldAlert, LogOut } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { api } from '../api/client';
import { getHomeRouteForPerfil } from '../utils/formatters';
import { FormField } from '../components/ui/Common';

export function TrocarSenhaPage() {
  const { user, refreshUser, logout } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const [senhaAtual, setSenhaAtual] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const obrigatoria = !!user?.deveTrocarSenha;

  const validatePassword = (pwd) => {
    if (pwd.length < 10 || pwd.length > 72) {
      return 'A nova senha deve ter entre 10 e 72 caracteres.';
    }
    if (!/[a-zA-Z]/.test(pwd) || !/\d/.test(pwd)) {
      return 'A nova senha deve conter letras e números.';
    }
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setFieldErrors({});

    const pwdError = validatePassword(novaSenha);
    if (pwdError) {
      setFieldErrors({ novaSenha: pwdError });
      return;
    }

    if (novaSenha !== confirmarSenha) {
      setFieldErrors({ confirmarSenha: 'A confirmação de senha não confere.' });
      return;
    }

    setLoading(true);
    try {
      await api.post('/auth/trocar-senha', { senhaAtual, novaSenha });
      const updatedUser = await refreshUser();
      toast.success('Senha atualizada com sucesso!');
      navigate(getHomeRouteForPerfil(updatedUser?.perfil || user?.perfil), { replace: true });
    } catch (err) {
      setErrorMsg(err.detail || 'Não foi possível alterar a senha.');
      if (err.erros) {
        setFieldErrors(err.erros);
      }
    } finally {
      setLoading(false);
    }
  };

  const content = (
    <div className="card" style={{ maxWidth: 520, margin: obrigatoria ? '0 auto' : '0' }}>
      <div className="card-header">
        <div className="card-title-group">
          <KeyRound size={22} className="text-primary" />
          <div>
            <h3>{obrigatoria ? 'Troca de Senha Obrigatória' : 'Alterar Senha de Acesso'}</h3>
            <p className="text-muted">
              Escolha uma senha forte com no mínimo 10 caracteres, contendo letras e números.
            </p>
          </div>
        </div>
      </div>

      <div className="card-body">
        {obrigatoria && (
          <div className="alert alert-warning" style={{ marginBottom: '1rem' }}>
            <ShieldAlert size={18} />
            <span>
              Você está acessando com uma senha temporária. Por segurança, defina uma nova senha pessoal antes de continuar.
            </span>
          </div>
        )}

        {errorMsg && (
          <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
            <AlertCircle size={18} />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <FormField label="Senha Atual (ou Temporária)" required error={fieldErrors.senhaAtual}>
            <input
              type="password"
              className="form-control"
              value={senhaAtual}
              onChange={(e) => setSenhaAtual(e.target.value)}
              required
              autoFocus
            />
          </FormField>

          <FormField
            label="Nova Senha"
            required
            error={fieldErrors.novaSenha}
            hint="Deve ter entre 10 e 72 caracteres, com pelo menos uma letra e um número."
          >
            <input
              type="password"
              className="form-control"
              value={novaSenha}
              onChange={(e) => setNovaSenha(e.target.value)}
              required
            />
          </FormField>

          <FormField label="Confirmar Nova Senha" required error={fieldErrors.confirmarSenha}>
            <input
              type="password"
              className="form-control"
              value={confirmarSenha}
              onChange={(e) => setConfirmarSenha(e.target.value)}
              required
            />
          </FormField>

          <div className="form-actions" style={{ marginTop: '1.5rem' }}>
            {obrigatoria && (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => {
                  logout();
                  navigate('/login');
                }}
              >
                <LogOut size={16} /> Sair
              </button>
            )}
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Salvando...' : 'Salvar Nova Senha'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );

  if (obrigatoria) {
    return (
      <div className="auth-screen">
        <div style={{ width: '100%', maxWidth: 520 }}>{content}</div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Segurança da Conta</h2>
          <p className="page-subtitle">Atualize sua credencial de acesso ao Saluris IA.</p>
        </div>
      </div>
      {content}
    </div>
  );
}
