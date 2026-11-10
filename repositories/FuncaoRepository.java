package br.com.saluris.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.Funcao;

public interface FuncaoRepository extends JpaRepository<Funcao, Long> {

    Optional<Funcao> findByIdAndEmpresaId(Long id, Long empresaId);

    List<Funcao> findByEmpresaIdOrderByNomeAsc(Long empresaId);

    List<Funcao> findByEmpresaIdAndAtivaTrueOrderByNomeAsc(Long empresaId);

    boolean existsByEmpresaIdAndNomeIgnoreCase(Long empresaId, String nome);
}
