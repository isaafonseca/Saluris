import React, { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { ShieldCheck, Lock, Mail, AlertCircle, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { getHomeRouteForPerfil } from '../utils/formatters';
import { FormField } from '../components/ui/Common';

const DEMO_ACCOUNTS = [
  { label: 'Admin (T.I.)', email: 'admin@saluris.com.br' },
  { label: 'Conta Principal Empresa', email: 'empresa@saluris.com.br' },
  { label: 'Especialista SST', email: 'especialista@saluris.com.br' },
  { label: 'Médico do Trabalho', email: 'medico@saluris.com.br' },
  { label: 'RH / Empresa', email: 'rh@saluris.com.br' },
];

export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);

  if (user) {
    if (user.deveTrocarSenha) {
      return <Navigate to="/trocar-senha" replace />;
    }
    return <Navigate to={getHomeRouteForPerfil(user.perfil)} replace />;
  }

  const performLogin = async (emailVal, senhaVal) => {
    setErrorMsg('');
    setFieldErrors({});
    const cleanEmail = (emailVal || '').trim();
    const cleanSenha = (senhaVal || '').trim();
    if (!cleanEmail || !cleanSenha) {
      setFieldErrors({
        email: !cleanEmail ? 'Informe o seu e-mail.' : undefined,
        senha: !cleanSenha ? 'Informe a sua senha.' : undefined,
      });
      return;
    }
    setLoading(true);
    try {
      const usuario = await login(cleanEmail, cleanSenha);
      if (usuario?.deveTrocarSenha) {
        navigate('/trocar-senha', { replace: true });
      } else {
        navigate(getHomeRouteForPerfil(usuario?.perfil), { replace: true });
      }
    } catch (err) {
      if (err?.erros && Object.keys(err.erros).length > 0) {
        setFieldErrors(err.erros);
      }
      setErrorMsg(err?.detail || err?.message || 'E-mail ou senha inválidos.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    await performLogin(email, senha);
  };

  const handleQuickDemo = (demoEmail) => {
    setEmail(demoEmail);
    setSenha('Saluris@2026!');
    performLogin(demoEmail, 'Saluris@2026!');
  };

  return (
    <div className="auth-screen">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-logo">
            <ShieldCheck size={34} />
          </div>
          <h1>Saluris IA</h1>
          <p>Sistema Inteligente de Gestão de Saúde e Segurança do Trabalho (SST)</p>
        </div>

        {errorMsg && (
          <div className="alert alert-danger">
            <AlertCircle size={18} />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          <FormField label="E-mail corporativo" error={fieldErrors.email}>
            <div className="input-with-icon">
              <Mail size={18} className="input-icon" />
              <input
                type="email"
                className="form-control"
                placeholder="nome@empresa.com.br"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoFocus
                required
              />
            </div>
          </FormField>

          <FormField label="Senha" error={fieldErrors.senha}>
            <div className="input-with-icon" style={{ position: 'relative' }}>
              <Lock size={18} className="input-icon" />
              <input
                type={mostrarSenha ? 'text' : 'password'}
                className="form-control"
                style={{ paddingRight: '2.5rem' }}
                placeholder="Digite sua senha"
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                required
              />
              <button
                type="button"
                onClick={() => setMostrarSenha((v) => !v)}
                title={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
                style={{
                  position: 'absolute',
                  right: '0.6rem',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  color: '#64748b',
                  display: 'flex',
                  alignItems: 'center',
                }}
              >
                {mostrarSenha ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </FormField>

          <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={loading}>
            {loading ? 'Autenticando...' : 'Entrar no Sistema'}
          </button>
        </form>

        <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid #e2e8f0' }}>
          <div style={{ fontSize: '0.78rem', color: '#64748b', marginBottom: '0.5rem', textAlign: 'center', fontWeight: 500 }}>
            Acesso rápido de demonstração (1 clique):
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.4rem' }}>
            {DEMO_ACCOUNTS.map((acc) => (
              <button
                key={acc.email}
                type="button"
                className="btn btn-secondary btn-sm"
                style={{ fontSize: '0.75rem', padding: '0.4rem 0.5rem', justifyContent: 'center' }}
                disabled={loading}
                onClick={() => handleQuickDemo(acc.email)}
              >
                {acc.label}
              </button>
            ))}
          </div>
        </div>

        <div className="auth-footer">
          <span>Conformidade com NRs (NR-01, NR-07, NR-09, NR-15)</span>
        </div>
      </div>
    </div>
  );
}
