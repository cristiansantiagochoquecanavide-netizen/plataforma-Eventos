package com.plataformaeventos.usuarios.repositorio;

import com.plataformaeventos.usuarios.modelo.Usuario;
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
            where usuario.estado = com.plataformaeventos.usuarios.modelo.EstadoUsuario.ACTIVO
              and rol.estado = com.plataformaeventos.roles.modelo.EstadoRol.ACTIVO
              and rol.nombre in :roles
            order by usuario.apellidos asc, usuario.nombres asc
            """)
    List<Usuario> findResponsablesDisponibles(@Param("roles") Set<String> roles);

    @EntityGraph(attributePaths = "roles")
    @Query("select distinct usuario from Usuario usuario join MembresiaOrganizacion membresia on membresia.usuario.id = usuario.id join membresia.roles rol where membresia.organizacion.id = :org and membresia.activo = true and usuario.estado = com.plataformaeventos.usuarios.modelo.EstadoUsuario.ACTIVO and rol.estado = com.plataformaeventos.roles.modelo.EstadoRol.ACTIVO and rol.nombre in :roles order by usuario.apellidos asc, usuario.nombres asc")
    List<Usuario> findResponsablesDisponiblesPorOrganizacion(@Param("roles") Set<String> roles, @Param("org") Long org);

    @EntityGraph(attributePaths = "roles")
    @Query("select usuario from Usuario usuario join MembresiaOrganizacion membresia on membresia.usuario.id = usuario.id where usuario.id = :id and membresia.organizacion.id = :org and membresia.activo = true")
    Optional<Usuario> findByIdAndOrganizacionId(@Param("id") Long id, @Param("org") Long org);

    @EntityGraph(attributePaths = "roles")
    @Query("select distinct usuario from Usuario usuario join MembresiaOrganizacion membresia on membresia.usuario.id = usuario.id join membresia.roles rol where usuario.id = :id and membresia.organizacion.id = :org and membresia.activo = true and usuario.estado = com.plataformaeventos.usuarios.modelo.EstadoUsuario.ACTIVO and rol.estado = com.plataformaeventos.roles.modelo.EstadoRol.ACTIVO and rol.nombre in :roles")
    Optional<Usuario> findResponsableActivoPorOrganizacionYRol(@Param("id") Long id, @Param("org") Long org, @Param("roles") Set<String> roles);

    @Query("select distinct rol.nombre from MembresiaOrganizacion membresia join membresia.roles rol where membresia.usuario.id = :id and membresia.organizacion.id = :org and membresia.activo = true and membresia.usuario.estado = com.plataformaeventos.usuarios.modelo.EstadoUsuario.ACTIVO and rol.estado = com.plataformaeventos.roles.modelo.EstadoRol.ACTIVO order by rol.nombre asc")
    List<String> findNombresRolesActivosPorUsuarioYOrganizacion(@Param("id") Long id, @Param("org") Long org);

    boolean existsByCorreo(String correo);

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findById(Long id);

    @EntityGraph(attributePaths = "roles")
    Page<Usuario> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    @Query("select distinct usuario from Usuario usuario join MembresiaOrganizacion membresia on membresia.usuario.id = usuario.id where membresia.organizacion.id = :org and membresia.activo = true")
    Page<Usuario> findAllByOrganizacionId(@Param("org") Long org, Pageable pageable);
}
