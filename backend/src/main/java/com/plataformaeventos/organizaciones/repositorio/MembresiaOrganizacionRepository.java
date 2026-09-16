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
    @org.springframework.data.jpa.repository.Query("select m from MembresiaOrganizacion m join fetch m.usuario u where m.organizacion.id = :organizacion order by u.apellidos, u.nombres")
    List<MembresiaOrganizacion> findByOrganizacionIdOrderByUsuarioApellidosAscUsuarioNombresAsc(@org.springframework.data.repository.query.Param("organizacion") Long idOrganizacion);
    @org.springframework.data.jpa.repository.Query("select count(m) > 0 from MembresiaOrganizacion m join m.roles r join r.permisos p where m.usuario.correo = :correo and m.organizacion.id = :organizacion and m.activo = true and r.estado = com.plataformaeventos.roles.modelo.EstadoRol.ACTIVO and p.nombre = :permiso")
    boolean tienePermisoActivo(@org.springframework.data.repository.query.Param("correo") String correo, @org.springframework.data.repository.query.Param("organizacion") Long organizacion, @org.springframework.data.repository.query.Param("permiso") String permiso);
    @org.springframework.data.jpa.repository.Query("select count(m) > 0 from MembresiaOrganizacion m join m.roles r where m.usuario.correo = :correo and m.organizacion.id = :organizacion and m.activo = true and r.estado = com.plataformaeventos.roles.modelo.EstadoRol.ACTIVO and r.nombre = 'ADMINISTRADOR'")
    boolean esAdministradorActivo(@org.springframework.data.repository.query.Param("correo") String correo, @org.springframework.data.repository.query.Param("organizacion") Long organizacion);
}
