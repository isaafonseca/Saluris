import React from 'react';
import {
  STATUS_VALIDADE_LABELS,
  STATUS_FLUXO_EXAME_LABELS,
  STATUS_LAUDO_LABELS,
  CLASSIFICACAO_IA_LABELS,
} from '../../utils/formatters';

export function Badge({ variant = 'neutral', children }) {
  return <span className={`badge badge-${variant}`}>{children}</span>;
}

export function ValidadeBadge({ status }) {
  if (!status) return <Badge variant="neutral">Sem registro</Badge>;
  const map = {
    EM_DIA: 'success',
    PROXIMO_VENCIMENTO: 'warning',
    VENCIDO: 'danger',
  };
  return (
    <Badge variant={map[status] || 'neutral'}>
      {STATUS_VALIDADE_LABELS[status] || status}
    </Badge>
  );
}

export function FluxoExameBadge({ status }) {
  if (!status) return <Badge variant="neutral">—</Badge>;
  const map = {
    PENDENTE: 'neutral',
    AGENDADO: 'info',
    REALIZADO: 'info',
    AGUARDANDO_ASSINATURA: 'info',
    HOMOLOGADO: 'success',
    REVISADO_PENDENTE: 'orange',
  };
  return (
    <Badge variant={map[status] || 'neutral'}>
      {STATUS_FLUXO_EXAME_LABELS[status] || status}
    </Badge>
  );
}

export function LaudoStatusBadge({ status }) {
  if (!status) return <Badge variant="neutral">—</Badge>;
  const map = {
    AGUARDANDO_ASSINATURA: 'info',
    HOMOLOGADO: 'success',
    REVISADO_PENDENTE: 'orange',
  };
  return (
    <Badge variant={map[status] || 'neutral'}>
      {STATUS_LAUDO_LABELS[status] || status}
    </Badge>
  );
}

export function ClassificacaoIABadge({ classificacao }) {
  if (!classificacao) return null;
  const map = {
    RESPONDIDA: 'success',
    FORA_DO_ESCOPO: 'warning',
    BASE_INSUFICIENTE: 'orange',
    RECUSADA_LAUDO_DIAGNOSTICO: 'danger',
  };
  return (
    <Badge variant={map[classificacao] || 'neutral'}>
      {CLASSIFICACAO_IA_LABELS[classificacao] || classificacao}
    </Badge>
  );
}
