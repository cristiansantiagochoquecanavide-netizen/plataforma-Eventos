package com.plataformaeventos.usuarios;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    @EntityGraph(attributePaths = "roles")
    @Query("select usuario from Usuario usuario where usuario.correo = :correo")
    Optional<Usuario> findByCorreoConRoles(@Param("correo") String correo);

    @EntityGraph(attributePaths = {"roles", "roles.permisos"})
    @Query("select usuario from Usuario usuario where usuario.correo = :correo")
    Optional<Usuario> findByCorreoConRolesYPermisos(@Param("correo") String correo);

    @EntityGraph(attributePaths = "roles")
    @Query("""
            select distinct usuario from Usuario usuario
            join usuario.roles rol
            where usuario.estado = com.plataformaeventos.usuarios.EstadoUsuario.ACTIVO
              and rol.estado = com.plataformaeventos.roles.EstadoRol.ACTIVO
              and rol.nombre in :roles
            order by usuario.apellidos asc, usuario.nombres asc
            """)
    List<Usuario> findResponsablesDisponibles(@Param("roles") Set<String> roles);

    boolean existsByCorreo(String correo);

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findById(Long id);

    @EntityGraph(attributePaths = "roles")
    Page<Usuario> findAll(Pageable pageable);
}
