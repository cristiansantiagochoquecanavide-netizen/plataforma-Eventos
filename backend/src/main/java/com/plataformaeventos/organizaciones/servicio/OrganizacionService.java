package com.plataformaeventos.organizaciones.servicio;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.organizaciones.dto.AgregarMembresiaSolicitud;
import com.plataformaeventos.organizaciones.dto.CrearOrganizacionSolicitud;
import com.plataformaeventos.organizaciones.dto.OrganizacionRespuesta;
import com.plataformaeventos.organizaciones.dto.MembresiaRespuesta;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.excepciones.ExcepcionNoAutorizado;
import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.audit.servicio.AuditoriaContexto;
import com.plataformaeventos.saas.servicio.SuscripcionOrganizacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class OrganizacionService {
    private static final String ROL_SAAS_ADMINISTRADOR = "SAAS_ADMINISTRADOR";

    private final OrganizacionRepository organizaciones;
    private final MembresiaOrganizacionRepository membresias;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final ContextoOrganizacion contexto;
    private final BitacoraService bitacora;
    private final SuscripcionOrganizacionService suscripciones;

    public OrganizacionService(OrganizacionRepository organizaciones,
                               MembresiaOrganizacionRepository membresias,
                               UsuarioRepository usuarios, RolRepository roles, ContextoOrganizacion contexto) {
        this(organizaciones, membresias, usuarios, roles, contexto, null, null);
    }

    public OrganizacionService(OrganizacionRepository organizaciones, MembresiaOrganizacionRepository membresias,
                               UsuarioRepository usuarios, RolRepository roles, ContextoOrganizacion contexto,
                               BitacoraService bitacora) {
        this(organizaciones, membresias, usuarios, roles, contexto, bitacora, null);
    }

    @Autowired
    public OrganizacionService(OrganizacionRepository organizaciones, MembresiaOrganizacionRepository membresias,
                               UsuarioRepository usuarios, RolRepository roles, ContextoOrganizacion contexto,
                               BitacoraService bitacora, SuscripcionOrganizacionService suscripciones) {
        this.organizaciones = organizaciones;
        this.membresias = membresias;
        this.usuarios = usuarios;
        this.roles = roles;
        this.contexto = contexto;
        this.bitacora = bitacora;
        this.suscripciones = suscripciones;
    }

    @Transactional(readOnly = true)
    public List<OrganizacionRespuesta> listarMias(String correo) {
        return membresias.findByUsuarioCorreoAndActivoTrueOrderByOrganizacionNombre(correo)
                .stream().map(MembresiaOrganizacion::getOrganizacion)
                .map(OrganizacionRespuesta::desde).toList();
    }

    @Transactional
    public OrganizacionRespuesta crear(CrearOrganizacionSolicitud solicitud, String correoAdministrador) {
        return crear(solicitud, correoAdministrador, null);
    }

    @Transactional
    public OrganizacionRespuesta crear(CrearOrganizacionSolicitud solicitud, String correoAdministrador, HttpServletRequest request) {
        if (organizaciones.findBySlug(solicitud.slug()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una organizacion con ese slug");
        }
        var administrador = usuarios.findByCorreo(correoAdministrador)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Administrador no encontrado"));
        Organizacion organizacion = organizaciones.save(new Organizacion(solicitud.nombre().trim(), solicitud.slug()));
        if (suscripciones != null) suscripciones.crearPlanBaseSiFalta(organizacion);
        MembresiaOrganizacion membresia = new MembresiaOrganizacion(administrador, organizacion);
        membresia.asignarRoles(Set.of(roles.findByNombre("ADMINISTRADOR").orElseThrow()));
        membresias.save(membresia);
        if (bitacora != null) bitacora.registrar(administrador, organizacion, "CREAR", "ORGANIZACIONES", "ORGANIZACION",
                organizacion.getId(), AuditoriaContexto.ip(request), true, "Organizacion creada");
        return OrganizacionRespuesta.desde(organizacion);
    }

    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "ORGANIZACIONES", entidad = "MEMBRESIA", accion = "AGREGAR")
    public void agregarMembresia(Long idOrganizacion, AgregarMembresiaSolicitud solicitud, String correoActor, org.springframework.security.core.Authentication auth, jakarta.servlet.http.HttpServletRequest request) {
        Organizacion activa = contexto.resolver(auth, request);
        if (!activa.getId().equals(idOrganizacion) || !contexto.esAdministrador(auth, request)) throw new ExcepcionNoAutorizado("No puedes gestionar membresias de esta organizacion");
        Organizacion organizacion = organizaciones.findById(idOrganizacion)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Organizacion no encontrada"));
        var usuario = usuarios.findById(solicitud.idUsuario())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado"));
        MembresiaOrganizacion.Clave clave = new MembresiaOrganizacion.Clave();
        clave.usuario = usuario.getId();
        clave.organizacion = organizacion.getId();
        if (membresias.existsById(clave)) throw new IllegalArgumentException("El usuario ya tiene una membresia");
        MembresiaOrganizacion membresia = new MembresiaOrganizacion(usuario, organizacion);
        membresia.asignarRoles(resolverRoles(solicitud.roles()));
        membresias.save(membresia);
    }
    @Transactional(readOnly = true)
    public List<MembresiaRespuesta> listarMiembros(org.springframework.security.core.Authentication auth, jakarta.servlet.http.HttpServletRequest request) {
        Organizacion organizacion = contexto.resolver(auth, request);
        return membresias.findByOrganizacionIdOrderByUsuarioApellidosAscUsuarioNombresAsc(organizacion.getId()).stream().map(this::respuestaMembresia).toList();
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "ORGANIZACIONES", entidad = "MEMBRESIA", accion = "CAMBIAR_ESTADO")
    public void cambiarEstadoMembresia(Long idOrganizacion, Long idUsuario, boolean activo, org.springframework.security.core.Authentication auth, jakarta.servlet.http.HttpServletRequest request) {
        Organizacion organizacion = contexto.resolver(auth, request);
        if (!organizacion.getId().equals(idOrganizacion) || !contexto.esAdministrador(auth, request)) throw new ExcepcionNoAutorizado("No puedes gestionar membresias de esta organizacion");
        MembresiaOrganizacion.Clave clave = new MembresiaOrganizacion.Clave(); clave.usuario = idUsuario; clave.organizacion = idOrganizacion;
        MembresiaOrganizacion membresia = membresias.findById(clave).orElseThrow(() -> new ExcepcionNoEncontrado("Membresia no encontrada"));
        if (activo) membresia.activar(); else membresia.inactivar();
    }
    private Set<Rol> resolverRoles(Set<String> nombres) {
        Set<String> solicitados = nombres == null || nombres.isEmpty() ? Set.of("PARTICIPANTE") : nombres.stream().map(s -> s.trim().toUpperCase()).collect(java.util.stream.Collectors.toSet());
        if (solicitados.contains(ROL_SAAS_ADMINISTRADOR)) throw new IllegalArgumentException("SAAS_ADMINISTRADOR no puede asignarse como rol de membresia");
        List<Rol> encontrados = roles.findByNombreIn(solicitados);
        if (encontrados.size() != solicitados.size() || encontrados.stream().anyMatch(r -> r.getEstado() != EstadoRol.ACTIVO)) throw new IllegalArgumentException("Uno o mas roles no existen o estan inactivos");
        return Set.copyOf(encontrados);
    }
    private MembresiaRespuesta respuestaMembresia(MembresiaOrganizacion m) { var u = m.getUsuario(); return new MembresiaRespuesta(u.getId(), u.getNombres(), u.getApellidos(), u.getCorreo(), m.estaActiva(), u.getEstado() == EstadoUsuario.ACTIVO, m.getRoles().stream().map(Rol::getNombre).sorted().toList()); }
}
