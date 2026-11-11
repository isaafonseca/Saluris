import React from 'react';
import { Loader2, Inbox, AlertTriangle, XCircle } from 'lucide-react';
import { Modal } from './Modal';

export function ClearFiltersButton({ active, onClick, disabled = false }) {
  if (!active) return null;

  return (
    <div className="filter-footer">
      <button
        type="button"
        className="btn btn-secondary"
        onClick={onClick}
        disabled={disabled}
      >
        <XCircle size={16} /> Limpar Filtros
      </button>
    </div>
  );
}

export function Loading({ text = 'Carregando dados...' }) {
  return (
    <div className="loading-state">
      <Loader2 className="spin" size={28} />
      <span>{text}</span>
    </div>
  );
}

export function EmptyState({ icon: Icon = Inbox, title = 'Nenhum registro encontrado', description, action }) {
  return (
    <div className="empty-state">
      <div className="empty-state-icon">
        <Icon size={36} />
      </div>
      <h4 className="empty-state-title">{title}</h4>
      {description && <p className="empty-state-desc">{description}</p>}
      {action && <div className="empty-state-action">{action}</div>}
    </div>
  );
}

export function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title = 'Confirmar ação',
  message,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  variant = 'primary',
  loading = false,
}) {
  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={title}
      size="sm"
      footer={
        <>
          <button type="button" className="btn btn-secondary" onClick={onClose} disabled={loading}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={`btn btn-${variant}`}
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? 'Processando...' : confirmLabel}
          </button>
        </>
      }
    >
      <div className="confirm-dialog-body">
        <AlertTriangle size={24} className="confirm-icon" />
        <div className="confirm-message">{message}</div>
      </div>
    </Modal>
  );
}

export function FormField({ label, required, error, hint, children }) {
  return (
    <div className={`form-group ${error ? 'has-error' : ''}`}>
      {label && (
        <label className="form-label">
          {label} {required && <span className="required-star">*</span>}
        </label>
      )}
      {children}
      {hint && !error && <small className="form-hint">{hint}</small>}
      {error && <span className="form-error">{error}</span>}
    </div>
  );
}

export function Pagination({ page = 0, totalPages = 0, totalElements = 0, onPageChange }) {
  if (totalPages <= 1) return null;
  return (
    <div className="pagination-bar">
      <span className="pagination-info">
        Página <strong>{page + 1}</strong> de <strong>{totalPages}</strong> ({totalElements} registros)
      </span>
      <div className="pagination-actions">
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
        >
          Anterior
        </button>
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Próxima
        </button>
      </div>
    </div>
  );
}
