package br.com.saluris.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.saluris.domain.entity.Notificacao;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    Page<Notificacao> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId, Pageable pageable);

    long countByUsuarioIdAndLidaFalse(Long usuarioId);

    Optional<Notificacao> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndExameIdAndMarcoDiasAndDataReferencia(Long usuarioId, Long exameId, Integer marcoDias,
            LocalDate dataReferencia);

    boolean existsByUsuarioIdAndDocumentoIdAndMarcoDiasAndDataReferencia(Long usuarioId, Long documentoId,
            Integer marcoDias, LocalDate dataReferencia);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = true, n.lidaEm = :agora WHERE n.usuario.id = :usuarioId AND n.lida = false")
    int marcarTodasComoLidas(@Param("usuarioId") Long usuarioId, @Param("agora") java.time.OffsetDateTime agora);
}
