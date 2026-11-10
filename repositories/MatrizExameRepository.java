package br.com.saluris.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.MatrizExame;

public interface MatrizExameRepository extends JpaRepository<MatrizExame, Long> {

    Optional<MatrizExame> findByIdAndEmpresaId(Long id, Long empresaId);

    @EntityGraph(attributePaths = { "funcao", "setor", "tipoExame", "configuradoPor" })
    List<MatrizExame> findByEmpresaIdOrderByFuncaoNomeAscSetorNomeAscTipoExameNomeAsc(Long empresaId);

    @EntityGraph(attributePaths = { "tipoExame" })
    List<MatrizExame> findByEmpresaIdAndFuncaoIdAndSetorId(Long empresaId, Long funcaoId, Long setorId);

    Optional<MatrizExame> findByFuncaoIdAndSetorIdAndTipoExameId(Long funcaoId, Long setorId, Long tipoExameId);

    Page<MatrizExame> findByEmpresaId(Long empresaId, Pageable pageable);
}
