import React from 'react';
import { EmptyState, Loading } from './Common';

export function Table({
  columns = [],
  data = [],
  rowKey = 'id',
  loading = false,
  emptyTitle = 'Nenhum registro encontrado',
  emptyDescription = 'Não há itens para exibir com os filtros selecionados.',
  onRowClick,
}) {
  if (loading) {
    return <Loading />;
  }

  if (!data || data.length === 0) {
    return <EmptyState title={emptyTitle} description={emptyDescription} />;
  }

  return (
    <div className="table-responsive">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((col, idx) => (
              <th key={col.key || idx} style={col.width ? { width: col.width } : undefined}>
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row, rIdx) => (
            <tr
              key={row[rowKey] ?? rIdx}
              className={onRowClick ? 'clickable-row' : ''}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
            >
              {columns.map((col, cIdx) => (
                <td key={col.key || cIdx}>
                  {col.render ? col.render(row) : row[col.key] ?? '—'}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
