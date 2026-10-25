package com.plataformaeventos.autenticacion.servicio;

import com.plataformaeventos.autenticacion.dto.InicioSesionSolicitud;
import com.plataformaeventos.autenticacion.dto.RegistroSolicitud;
import com.plataformaeventos.autenticacion.dto.PerfilOrganizacionActivaRespuesta;
import com.plataformaeventos.autenticacion.dto.RestablecerClaveSolicitud;
import com.plataformaeventos.autenticacion.dto.SesionRespuesta;
import com.plataformaeventos.autenticacion.dto.SolicitudRecuperacionClave;
import com.plataformaeventos.autenticacion.dto.TokenRefrescoSolicitud;
import com.plataformaeventos.autenticacion.dto.UsuarioActualRespuesta;
import com.plataformaeventos.excepciones.ExcepcionNoAutorizado;
import com.plataformaeventos.grupos.repositorio.GrupoUsuarioRepository;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.permisos.modelo.Permiso;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.seguridad.JwtService;
import com.plataformaeventos.seguridad.RecuperacionClaveService;
import com.plataformaeventos.seguridad.TokenRefrescoService;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final OrganizacionRepository organizacionRepository;
    private final MembresiaOrganizacionRepository membresiaRepository;
    private final PasswordEncoder codificadorClave;
    private final JwtService jwtService;
    private final TokenRefrescoService tokenRefrescoService;
    private static final String ROL_SAAS_ADMINISTRADOR = "SAAS_ADMINISTRADOR";

    private final RecuperacionClaveService recuperacionClaveService;
    private final MembresiaOrganizacionRepository membresiaOrganizacionRepository;
    private final GrupoUsuarioRepository grupoUsuarioRepository;

    public AutenticacionService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            OrganizacionRepository organizacionRepository,
            MembresiaOrganizacionRepository membresiaRepository,
            PasswordEncoder codificadorClave,
            JwtService jwtService,
            TokenRefrescoService tokenRefrescoService,
            RecuperacionClaveService recuperacionClaveService,
            MembresiaOrganizacionRepository membresiaOrganizacionRepository,
            GrupoUsuarioRepository grupoUsuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.organizacionRepository = organizacionRepository;
        this.membresiaRepository = membresiaRepository;
        this.codificadorClave = codificadorClave;
        this.jwtService = jwtService;
        this.tokenRefrescoService = tokenRefrescoService;
        this.recuperacionClaveService = recuperacionClaveService;
        this.membresiaOrganizacionRepository = membresiaOrganizacionRepository;
        this.grupoUsuarioRepository = grupoUsuarioRepository;
    }

    @Transactional
    public UsuarioActualRespuesta registrarUsuario(RegistroSolicitud solicitud) {
        String correo = normalizarCorreo(solicitud.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado");
        }

        Rol rolParticipante = rolRepository.findByNombre("PARTICIPANTE")
                .orElseThrow(() -> new IllegalStateException("No existe el rol PARTICIPANTE"));
        Usuario usuario = new Usuario(
                solicitud.nombres().trim(),
                solicitud.apellidos().trim(),
                correo,
                codificadorClave.encode(solicitud.clave()),
                EstadoUsuario.ACTIVO
        );
        usuario.asignarRoles(Set.of(rolParticipante));
        Usuario guardado = usuarioRepository.save(usuario);

        organizacionRepository.findBySlug("principal").ifPresent(org -> {
            MembresiaOrganizacion membresia = new MembresiaOrganizacion(guardado, org);
            membresia.activar();
            membresia.getRoles().add(rolParticipante);
            membresiaRepository.save(membresia);
        });

        return convertirUsuario(guardado, null);
    }

    @Transactional
    public SesionRespuesta iniciarSesion(InicioSesionSolicitud solicitud) {
        Usuario usuario = usuarioRepository.findByCorreo(normalizarCorreo(solicitud.correo()))
                .orElseThrow(() -> new ExcepcionNoAutorizado("Credenciales invalidas"));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ExcepcionNoAutorizado("La cuenta se encuentra inactiva");
        }
        if (!codificadorClave.matches(solicitud.clave(), usuario.getClaveHash())) {
            throw new ExcepcionNoAutorizado("Credenciales invalidas");
        }
        return crearSesion(usuario);
    }

    @Transactional
    public SesionRespuesta refrescarSesion(TokenRefrescoSolicitud solicitud) {
        TokenRefrescoService.TokenRenovado tokenRenovado;
        try {
            tokenRenovado = tokenRefrescoService.renovarToken(solicitud.tokenRefresco());
        } catch (IllegalArgumentException excepcion) {
            throw new ExcepcionNoAutorizado("Token de refresco invalido");
        }
        if (tokenRenovado.usuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new ExcepcionNoAutorizado("La cuenta se encuentra inactiva");
        }
        return new SesionRespuesta(
                jwtService.generarTokenAcceso(tokenRenovado.usuario().getCorreo()),
                tokenRenovado.token(),
                "Bearer"
        );
    }

    @Transactional
    public void cerrarSesion(TokenRefrescoSolicitud solicitud) {
        tokenRefrescoService.revocarToken(solicitud.tokenRefresco());
    }

    @Transactional
    public void solicitarRecuperacionClave(SolicitudRecuperacionClave solicitud) {
        Usuario usuario = usuarioRepository.findByCorreo(normalizarCorreo(solicitud.correo()))
                .filter(cuenta -> cuenta.getEstado() == EstadoUsuario.ACTIVO)
                .orElseThrow(() -> new IllegalArgumentException("No existe una cuenta asociada a este correo."));
        recuperacionClaveService.crearYEnviarToken(usuario);
    }

    @Transactional
    public void restablecerClave(RestablecerClaveSolicitud solicitud) {
        Usuario usuario = recuperacionClaveService.consumirToken(solicitud.token());
        usuario.actualizarClaveHash(codificadorClave.encode(solicitud.nuevaClave()));
        tokenRefrescoService.revocarTokensDelUsuario(usuario.getId());
    }

    @Transactional(readOnly = true)
    public UsuarioActualRespuesta obtenerUsuarioActual(String correo) {
        return obtenerUsuarioActual(correo, null);
    }

    @Transactional(readOnly = true)
    public UsuarioActualRespuesta obtenerUsuarioActual(String correo, Long idOrganizacionActiva) {
        String correoNormalizado = normalizarCorreo(correo);
        Usuario usuario = usuarioRepository.findByCorreoConRolesYPermisos(correoNormalizado)
                .orElseThrow(() -> new ExcepcionNoAutorizado("Usuario no encontrado"));
        return convertirUsuario(usuario, resolverPerfilOrganizacionActiva(usuario, idOrganizacionActiva));
    }

    private SesionRespuesta crearSesion(Usuario usuario) {
        return new SesionRespuesta(
                jwtService.generarTokenAcceso(usuario.getCorreo()),
                tokenRefrescoService.crearToken(usuario),
                "Bearer"
        );
    }

    private UsuarioActualRespuesta convertirUsuario(Usuario usuario, PerfilOrganizacionActivaRespuesta perfilOrganizacionActiva) {
        List<String> roles = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .sorted()
                .toList();
        return new UsuarioActualRespuesta(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getEstado().name(),
                roles,
                obtenerPermisos(usuario),
                perfilOrganizacionActiva
        );
    }

    private PerfilOrganizacionActivaRespuesta resolverPerfilOrganizacionActiva(Usuario usuario, Long idOrganizacionActiva) {
        if (idOrganizacionActiva == null) return null;
        return membresiaOrganizacionRepository.findByUsuarioCorreoAndOrganizacionIdAndActivoTrueConRolesYPermisos(usuario.getCorreo(), idOrganizacionActiva)
                .map(this::convertirPerfilOrganizacionActiva)
                .orElseGet(() -> perfilAdministradorSaasEnOrganizacion(usuario, idOrganizacionActiva));
    }

    private PerfilOrganizacionActivaRespuesta perfilAdministradorSaasEnOrganizacion(Usuario usuario, Long idOrganizacionActiva) {
        boolean administradorSaas = usuario.getRoles().stream()
                .anyMatch(rol -> rol.getEstado() == EstadoRol.ACTIVO && ROL_SAAS_ADMINISTRADOR.equals(rol.getNombre()));
        if (!administradorSaas) return null;

        Rol administradorTenant = rolRepository.findByNombre("ADMINISTRADOR")
                .filter(rol -> rol.getEstado() == EstadoRol.ACTIVO)
                .orElseThrow(() -> new IllegalStateException("No existe el rol ADMINISTRADOR activo"));
        List<String> permisos = administradorTenant.getPermisos().stream()
                .map(Permiso::getNombre)
                .distinct()
                .sorted()
                .toList();
        return new PerfilOrganizacionActivaRespuesta(idOrganizacionActiva, List.of("ADMINISTRADOR"), permisos, true);
    }

    private PerfilOrganizacionActivaRespuesta convertirPerfilOrganizacionActiva(MembresiaOrganizacion membresia) {
        Set<Rol> rolesEfectivos = resolverRolesTenantEfectivos(membresia);
        List<String> roles = rolesEfectivos.stream()
                .map(Rol::getNombre)
                .distinct()
                .sorted()
                .toList();
        List<String> permisos = rolesEfectivos.stream()
                .flatMap(rol -> rol.getPermisos().stream())
                .map(Permiso::getNombre)
                .distinct()
                .sorted()
                .toList();
        return new PerfilOrganizacionActivaRespuesta(
                membresia.getOrganizacion().getId(),
                roles,
                permisos,
                roles.contains("ADMINISTRADOR")
        );
    }

    private Set<Rol> resolverRolesTenantEfectivos(MembresiaOrganizacion membresia) {
        Set<Rol> rolesEfectivos = new HashSet<>();
        membresia.getRoles().stream()
                .filter(this::esRolTenantActivo)
                .forEach(rolesEfectivos::add);
        grupoUsuarioRepository.findRolesActivosHeredadosPorUsuarioYOrganizacion(
                        membresia.getUsuario().getCorreo(),
                        membresia.getOrganizacion().getId()
                ).stream()
                .filter(this::esRolTenantActivo)
                .forEach(rolesEfectivos::add);
        return rolesEfectivos;
    }

    private boolean esRolTenantActivo(Rol rol) {
        return rol.getEstado() == EstadoRol.ACTIVO
                && !ROL_SAAS_ADMINISTRADOR.equals(rol.getNombre());
    }

    private List<String> obtenerPermisos(Usuario usuario) {
        return usuario.getRoles().stream()
                .filter(rol -> rol.getEstado() == EstadoRol.ACTIVO)
                .flatMap(rol -> rol.getPermisos().stream())
                .map(Permiso::getNombre)
                .distinct()
                .sorted()
                .toList();
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
