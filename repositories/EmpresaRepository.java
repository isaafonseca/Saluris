package br.com.saluris.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    boolean existsByCnpj(String cnpj);

    Optional<Empresa> findByCnpj(String cnpj);
}
