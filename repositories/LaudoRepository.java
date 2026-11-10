package br.com.saluris.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.Laudo;
import br.com.saluris.domain.enums.StatusLaudo;

public interface LaudoRepository extends JpaRepository<Laudo, Long> {

    Optional<Laudo> findByIdAndEmpresaId(Long id, Long empresaId);

    Page<Laudo> findByEmpresaId(Long empresaId, Pageable pageable);

    List<Laudo> findByEmpresaIdOrderByCriadoEmDesc(Long empresaId);

    List<Laudo> findByEmpresaIdAndStatusOrderByCriadoEmDesc(Long empresaId, StatusLaudo status);

    Page<Laudo> findByStatus(StatusLaudo status, Pageable pageable);
}
