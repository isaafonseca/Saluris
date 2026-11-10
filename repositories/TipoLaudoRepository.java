package br.com.saluris.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.TipoLaudo;

public interface TipoLaudoRepository extends JpaRepository<TipoLaudo, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    List<TipoLaudo> findByAtivoTrueOrderByNomeAsc();
}
