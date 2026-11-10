package br.com.saluris.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.saluris.domain.entity.Colaborador;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {

    Optional<Colaborador> findByIdAndEmpresaId(Long id, Long empresaId);

    boolean existsByEmpresaIdAndCpf(Long empresaId, String cpf);

    Optional<Colaborador> findByEmpresaIdAndCpf(Long empresaId, String cpf);

    Page<Colaborador> findByEmpresaId(Long empresaId, Pageable pageable);

    @Query("""
            SELECT c FROM Colaborador c
            WHERE (:empresaId IS NULL OR c.empresaId = :empresaId)
              AND (LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR c.cpf LIKE CONCAT('%', :buscaDigitos, '%'))
            """)
    Page<Colaborador> buscarPorEmpresa(@Param("empresaId") Long empresaId,
            @Param("busca") String busca,
            @Param("buscaDigitos") String buscaDigitos,
            Pageable pageable);

    List<Colaborador> findByEmpresaIdAndFuncaoIdAndSetorIdAndAtivoTrue(Long empresaId, Long funcaoId, Long setorId);
}
