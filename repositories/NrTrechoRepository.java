package br.com.saluris.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.saluris.domain.entity.NrTrecho;

public interface NrTrechoRepository extends JpaRepository<NrTrecho, Long> {

    List<NrTrecho> findByNormaAndAtivoTrue(String norma);

    List<NrTrecho> findByAtivoTrueOrderByNormaAscItemAsc();

    @Query(value = """
            SELECT id, norma, titulo_norma, item, conteudo, ativo, criado_em
            FROM nr_trechos
            WHERE ativo = TRUE
              AND busca @@ to_tsquery('portuguese', :tsQuery)
            ORDER BY ts_rank(busca, to_tsquery('portuguese', :tsQuery)) DESC, norma ASC
            LIMIT 5
            """, nativeQuery = true)
    List<NrTrecho> buscarRelevantes(@Param("tsQuery") String tsQuery);
}
