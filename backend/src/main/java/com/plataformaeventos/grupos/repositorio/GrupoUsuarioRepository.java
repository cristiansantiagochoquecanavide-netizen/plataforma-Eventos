package com.plataformaeventos.grupos.repositorio;

import com.plataformaeventos.grupos.modelo.GrupoUsuario;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.usuarios.modelo.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GrupoUsuarioRepository extends JpaRepository<GrupoUsuario, Long> {
    boolean existsByOrganizacionIdAndNombreIgnoreCase(Long idOrganizacion, String nombre);

    List<GrupoUsuario> findByOrganizacionIdOrderByNombreAsc(Long idOrganizacion);

    Optional<GrupoUsuario> findByIdAndOrganizacionId(Long idGrupo, Long idOrganizacion);

    @EntityGraph(attributePaths = {"miembros", "roles"})
    @Query("select g from GrupoUsuario g where g.id = :idGrupo and g.organizacion.id = :idOrganizacion")
    Optional<GrupoUsuario> findByIdAndOrganizacionIdConMiembrosYRoles(
            @Param("idGrupo") Long idGrupo,
            @Param("idOrganizacion") Long idOrganizacion
    );

    @Query("select u from MembresiaOrganizacion m join m.usuario u "
            + "where m.organizacion.id = :idOrganizacion and u.id in :idsUsuarios "
            + "and m.activo = true "
            + "and u.estado = com.plataformaeventos.usuarios.modelo.EstadoUsuario.ACTIVO")
    List<Usuario> findUsuariosConMembresiaActivaYCuentaActiva(
            @Param("idOrganizacion") Long idOrganizacion,
            @Param("idsUsuarios") Collection<Long> idsUsuarios
    );

    @Query("select distinct r from GrupoUsuario g join g.miembros u join g.roles r left join fetch r.permisos "
            + "join MembresiaOrganizacion m on m.usuario = u and m.organizacion = g.organizacion "
            + "where u.correo = :correo and g.organizacion.id = :organizacion "
            + "and g.activo = true and m.activo = true "
            + "and r.estado = com.plataformaeventos.roles.modelo.EstadoRol.ACTIVO "
            + "and r.nombre <> 'SAAS_ADMINISTRADOR'")
    Set<Rol> findRolesActivosHeredadosPorUsuarioYOrganizacion(
            @Param("correo") String correo,
            @Param("organizacion") Long organizacion
    );
}
