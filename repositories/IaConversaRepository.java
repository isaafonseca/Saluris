package br.com.saluris.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.IaConversa;

public interface IaConversaRepository extends JpaRepository<IaConversa, Long> {

    List<IaConversa> findByUsuarioIdOrderByAtualizadoEmDesc(Long usuarioId);

    Optional<IaConversa> findByIdAndUsuarioId(Long id, Long usuarioId);
}
