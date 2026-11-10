package br.com.saluris.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.saluris.domain.entity.Documento;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    Optional<Documento> findByIdAndEmpresaId(Long id, Long empresaId);

    List<Documento> findByEmpresaIdOrderByDataValidadeAsc(Long empresaId);

    /** Última versão de um tipo de documento da empresa (para numerar a próxima versão). */
    Optional<Documento> findTopByEmpresaIdAndTipoDocumentoIdOrderByVersaoDesc(Long empresaId, Long tipoDocumentoId);
}
