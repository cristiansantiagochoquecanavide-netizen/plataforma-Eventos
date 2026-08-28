package com.plataformaeventos.usuarios;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    @EntityGraph(attributePaths = "roles")
    @Query("select usuario from Usuario usuario where usuario.correo = :correo")
    Optional<Usuario> findByCorreoConRoles(@Param("correo") String correo);

    boolean existsByCorreo(String correo);

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findById(Long id);

    @EntityGraph(attributePaths = "roles")
    Page<Usuario> findAll(Pageable pageable);
}
