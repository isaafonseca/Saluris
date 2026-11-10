package br.com.saluris.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.TipoExame;

public interface TipoExameRepository extends JpaRepository<TipoExame, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    List<TipoExame> findByAtivoTrueOrderByNomeAsc();
}
