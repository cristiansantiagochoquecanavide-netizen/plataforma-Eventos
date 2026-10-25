package com.plataformaeventos.organizaciones.repositorio;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MembresiaOrganizacionRepository extends JpaRepository<MembresiaOrganizacion, MembresiaOrganizacion.Clave> {
    boolean existsByUsuarioCorreoAndOrganizacionIdAndActivoTrue(String correo, Long idOrganizacion);
    boolean existsByUsuarioIdAndOrganizacionIdAndActivoTrue(Long usuario, Long organizacion);
    List<MembresiaOrganizacion> findByUsuarioCorreoAndActivoTrueOrderByOrganizacionNombre(String correo);
    Optional<MembresiaOrganizacion> findByUsuarioCorreoAndOrganizacionId(String correo, Long idOrganizacion);
    @org.springframework.data.jpa.repository.Query("select distinct m from MembresiaOrganizacion m left join fetch m.roles r left join fetch r.permisos where m.usuario.correo = :correo and m.organizacion.id = :organizacion and m.activo = true")
    Optional<MembresiaOrganizacion> findByUsuarioCorreoAndOrganizacionIdAndActivoTrueConRolesYPermisos(@org.springframework.data.repository.query.Param("correo") String correo, @org.springframework.data.repository.query.Param("organizacion") Long organizacion);
    @org.springframework.data.jpa.repository.Query("select m from MembresiaOrganizacion m left join fetch m.roles where m.usuario.id = :usuario and m.organizacion.id = :organizacion")
    Optional<MembresiaOrganizacion> findByUsuarioIdAndOrganizacionIdConRoles(@org.springframework.data.repository.query.Param("usuario") Long usuario, @org.springframework.data.repository.query.Param("organizacion") Long organizacion);
    @org.springframework.data.jpa.repository.Query("select m from MembresiaOrganizacion m join fetch m.usuario u where m.organizacion.id = :organizacion order by u.apellidos, u.nombres")
    List<MembresiaOrganizacion> findByOrganizacionIdOrderByUsuarioApellidosAscUsuarioNombresAsc(@org.springframework.data.repository.query.Param("organizacion") Long idOrganizacion);
    default boolean tienePermisoActivo(String correo, Long organizacion, String permiso) {
        return tienePermisoDirectoActivo(correo, organizacion, permiso)
                || tienePermisoGrupoActivo(correo, organizacion, permiso);
    }

    @org.springframework.data.jpa.repository.Query("select count(m) > 0 from MembresiaOrganizacion m join m.roles r join r.permisos p where m.usuario.correo = :correo and m.organizacion.id = :organizacion and m.activo = true and r.estado = com.plataformaeventos.roles.modelo.EstadoRol.ACTIVO and r.nombre <> 'SAAS_ADMINISTRADOR' and p.nombre = :permiso")
    boolean tienePermisoDirectoActivo(@org.springframework.data.repository.query.Param("correo") String correo, @org.springframework.data.repository.query.Param("organizacion") Long organizacion, @org.springframework.data.repository.query.Param("permiso") String permiso);

    @org.springframework.data.jpa.repository.Query("select count(r) > 0 from GrupoUsuario g join g.miembros u join g.roles r join r.permisos p join MembresiaOrganizacion m on m.usuario = u and m.organizacion = g.organizacion where u.correo = :correo and g.organizacion.id = :organizacion and g.activo = true and m.activo = true and r.estado = com.plataformaeventos.roles.modelo.EstadoRol.ACTIVO and r.nombre <> 'SAAS_ADMINISTRADOR' and p.nombre = :permiso")
    boolean tienePermisoGrupoActivo(@org.springframework.data.repository.query.Param("correo") String correo, @org.springframework.data.repository.query.Param("organizacion") Long organizacion, @org.springframework.data.repository.query.Param("permiso") String permiso);

    default boolean esAdministradorActivo(String correo, Long organizacion) {
        return esAdministradorDirectoActivo(correo, organizacion)
                || esAdministradorGrupoActivo(correo, organizacion);
    }

    @org.springframework.data.jpa.repository.Query("select count(m) > 0 from MembresiaOrganizacion m join m.roles r where m.usuario.correo = :correo and m.organizacion.id = :organizacion and m.activo = true and r.estado = com.plataformaeventos.roles.modelo.EstadoRol.ACTIVO and r.nombre = 'ADMINISTRADOR'")
    boolean esAdministradorDirectoActivo(@org.springframework.data.repository.query.Param("correo") String correo, @org.springframework.data.repository.query.Param("organizacion") Long organizacion);

    @org.springframework.data.jpa.repository.Query("select count(r) > 0 from GrupoUsuario g join g.miembros u join g.roles r join MembresiaOrganizacion m on m.usuario = u and m.organizacion = g.organizacion where u.correo = :correo and g.organizacion.id = :organizacion and g.activo = true and m.activo = true and r.estado = com.plataformaeventos.roles.modelo.EstadoRol.ACTIVO and r.nombre = 'ADMINISTRADOR'")
    boolean esAdministradorGrupoActivo(@org.springframework.data.repository.query.Param("correo") String correo, @org.springframework.data.repository.query.Param("organizacion") Long organizacion);
}
