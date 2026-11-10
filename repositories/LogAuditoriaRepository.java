package br.com.saluris.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.LogAuditoria;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    Page<LogAuditoria> findAllByOrderByCriadoEmDesc(Pageable pageable);

    Page<LogAuditoria> findByEmpresaIdOrderByCriadoEmDesc(Long empresaId, Pageable pageable);
}
