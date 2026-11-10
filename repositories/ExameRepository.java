package br.com.saluris.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.saluris.domain.entity.Exame;
import br.com.saluris.domain.enums.StatusFluxoExame;
import br.com.saluris.domain.enums.StatusValidade;

public interface ExameRepository extends JpaRepository<Exame, Long> {

    Optional<Exame> findByIdAndEmpresaId(Long id, Long empresaId);

    List<Exame> findByColaboradorIdOrderByDataVencimentoAsc(Long colaboradorId);

    List<Exame> findByColaboradorIdInAndStatusFluxoNot(Collection<Long> colaboradorIds, StatusFluxoExame statusExcluido);

    Page<Exame> findByEmpresaId(Long empresaId, Pageable pageable);

    Page<Exame> findByStatusFluxo(StatusFluxoExame statusFluxo, Pageable pageable);

    List<Exame> findByEmpresaIdAndStatusFluxoOrderByCriadoEmAsc(Long empresaId, StatusFluxoExame statusFluxo);

    List<Exame> findByStatusFluxoNot(StatusFluxoExame statusExcluido);

    List<Exame> findByEmpresaIdAndStatusFluxoNotOrderByDataVencimentoAsc(Long empresaId, StatusFluxoExame statusExcluido);

    List<Exame> findByColaboradorIdAndTipoExameIdAndStatusFluxoIn(Long colaboradorId, Long tipoExameId,
            Collection<StatusFluxoExame> status);

    Optional<Exame> findTopByColaboradorIdAndTipoExameIdAndStatusFluxoOrderByDataRealizacaoDesc(
            Long colaboradorId, Long tipoExameId, StatusFluxoExame status);

    boolean existsByColaboradorIdAndTipoExameIdAndStatusFluxoIn(Long colaboradorId, Long tipoExameId,
            Collection<StatusFluxoExame> status);

    @Query("""
            SELECT e FROM Exame e
            WHERE (:empresaId IS NULL OR e.empresaId = :empresaId)
              AND (:colaboradorId IS NULL OR e.colaborador.id = :colaboradorId)
              AND (:tipoExameId IS NULL OR e.tipoExame.id = :tipoExameId)
              AND (:statusValidade IS NULL OR e.statusValidade = :statusValidade)
              AND (:statusFluxo IS NULL OR e.statusFluxo = :statusFluxo)
              AND (:incluirHistorico = true OR e.statusFluxo <> br.com.saluris.domain.enums.StatusFluxoExame.HOMOLOGADO)
            """)
    Page<Exame> filtrar(@Param("empresaId") Long empresaId,
            @Param("colaboradorId") Long colaboradorId,
            @Param("tipoExameId") Long tipoExameId,
            @Param("statusValidade") StatusValidade statusValidade,
            @Param("statusFluxo") StatusFluxoExame statusFluxo,
            @Param("incluirHistorico") boolean incluirHistorico,
            Pageable pageable);
}
