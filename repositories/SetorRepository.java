package br.com.saluris.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.Setor;

public interface SetorRepository extends JpaRepository<Setor, Long> {

    Optional<Setor> findByIdAndEmpresaId(Long id, Long empresaId);

    List<Setor> findByEmpresaIdOrderByNomeAsc(Long empresaId);

    List<Setor> findByEmpresaIdAndAtivoTrueOrderByNomeAsc(Long empresaId);

    boolean existsByEmpresaIdAndNomeIgnoreCase(Long empresaId, String nome);
}
