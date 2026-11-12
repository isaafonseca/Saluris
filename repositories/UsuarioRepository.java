package br.com.saluris.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.saluris.domain.entity.Usuario;
import br.com.saluris.domain.enums.Perfil;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPerfil(Perfil perfil);

    List<Usuario> findByPerfil(Perfil perfil);

    Optional<Usuario> findByIdAndEmpresaId(Long id, Long empresaId);

    List<Usuario> findByEmpresaIdAndPerfilAndAtivoTrue(Long empresaId, Perfil perfil);

    List<Usuario> findByEmpresaIdAndPerfilInAndAtivoTrue(Long empresaId, Collection<Perfil> perfis);

    @Query("""
            SELECT u FROM Usuario u
            WHERE (:empresaId IS NULL OR u.empresaId = :empresaId)
              AND (:perfil IS NULL OR u.perfil = :perfil)
            ORDER BY u.nome ASC
            """)
    List<Usuario> filtrar(@Param("empresaId") Long empresaId, @Param("perfil") Perfil perfil);
}
