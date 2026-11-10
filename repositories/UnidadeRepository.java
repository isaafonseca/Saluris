package br.com.saluris.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.Unidade;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    Optional<Unidade> findByIdAndEmpresaId(Long id, Long empresaId);

    List<Unidade> findByEmpresaIdOrderByNomeAsc(Long empresaId);

    List<Unidade> findByEmpresaIdAndAtivaTrueOrderByNomeAsc(Long empresaId);

    boolean existsByEmpresaIdAndNomeIgnoreCase(Long empresaId, String nome);
}
