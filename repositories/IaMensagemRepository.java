package br.com.saluris.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.IaMensagem;

public interface IaMensagemRepository extends JpaRepository<IaMensagem, Long> {

    List<IaMensagem> findByConversaIdOrderByCriadoEmAsc(Long conversaId);
}
