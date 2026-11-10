package br.com.saluris.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.TipoDocumento;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsBySiglaIgnoreCase(String sigla);

    Optional<TipoDocumento> findBySiglaIgnoreCase(String sigla);

    List<TipoDocumento> findByAtivoTrueOrderBySiglaAsc();
}
