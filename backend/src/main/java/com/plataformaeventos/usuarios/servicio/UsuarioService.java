package com.plataformaeventos.usuarios.servicio;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.seguridad.TokenRefrescoService;
import com.plataformaeventos.usuarios.dto.ActualizarUsuarioSolicitud;
import com.plataformaeventos.usuarios.dto.CrearUsuarioSolicitud;
import com.plataformaeventos.usuarios.dto.PaginaUsuariosRespuesta;
import com.plataformaeventos.usuarios.dto.UsuarioRespuesta;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder codificadorClave;
    private final TokenRefrescoService tokenRefrescoService;
    private final MembresiaOrganizacionRepository membresiaRepository;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder codificadorClave,
            TokenRefrescoService tokenRefrescoService
    ) {
        this(usuarioRepository, rolRepository, codificadorClave, tokenRefrescoService, null);
    }

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                           PasswordEncoder codificadorClave, TokenRefrescoService tokenRefrescoService,
                           MembresiaOrganizacionRepository membresiaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.codificadorClave = codificadorClave;
        this.tokenRefrescoService = tokenRefrescoService;
        this.membresiaRepository = membresiaRepository;
    }

    @Transactional(readOnly = true)
    public PaginaUsuariosRespuesta listarUsuarios(int pagina, int tamano) {
        Pageable paginacion = PageRequest.of(pagina, tamano, Sort.by("id").ascending());
        Page<Usuario> usuarios = usuarioRepository.findAll(paginacion);
        return new PaginaUsuariosRespuesta(
            usuarios.map(this::convertirUsuario).getContent(),
                usuarios.getNumber(),
                usuarios.getSize(),
                usuarios.getTotalElements(),
                usuarios.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PaginaUsuariosRespuesta listarUsuarios(int pagina, int tamano, Organizacion organizacion) {
        Pageable paginacion = PageRequest.of(pagina, tamano, Sort.by("id").ascending());
        return convertirPagina(usuarioRepository.findAllByOrganizacionId(organizacion.getId(), paginacion), organizacion);
    }

    @Transactional(readOnly = true)
    public UsuarioRespuesta obtenerUsuario(Long idUsuario) {
        return convertirUsuario(buscarUsuario(idUsuario));
    }

    @Transactional(readOnly = true)
    public UsuarioRespuesta obtenerUsuario(Long idUsuario, Organizacion organizacion) {
        Usuario usuario = usuarioRepository.findByIdAndOrganizacionId(idUsuario, organizacion.getId()).orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado"));
        return convertirUsuario(usuario, organizacion);
    }

    @Transactional
    public UsuarioRespuesta crearUsuario(CrearUsuarioSolicitud solicitud) {
        String correo = normalizarCorreo(solicitud.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado");
        }
        Usuario usuario = new Usuario(
                solicitud.nombres().trim(),
                solicitud.apellidos().trim(),
                correo,
                codificadorClave.encode(solicitud.clave()),
                EstadoUsuario.ACTIVO
        );
        usuario.asignarRoles(obtenerRolesActivos(solicitud.roles()));
        return convertirUsuario(usuarioRepository.save(usuario));
    }

    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "USUARIOS", entidad = "USUARIO", accion = "CREAR")
    public UsuarioRespuesta crearUsuario(CrearUsuarioSolicitud solicitud, Organizacion organizacion) {
        String correo = normalizarCorreo(solicitud.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado");
        }
        Usuario usuario = new Usuario(
                solicitud.nombres().trim(),
                solicitud.apellidos().trim(),
                correo,
                codificadorClave.encode(solicitud.clave()),
                EstadoUsuario.ACTIVO
        );
        usuario = usuarioRepository.save(usuario);
        MembresiaOrganizacion membresia = new MembresiaOrganizacion(usuario, organizacion);
        membresia.asignarRoles(obtenerRolesActivos(solicitud.roles()));
        membresiaRepository.save(membresia);
        return convertirUsuario(usuario, organizacion);
    }

    @Transactional
    public UsuarioRespuesta actualizarUsuario(Long idUsuario, ActualizarUsuarioSolicitud solicitud) {
        Usuario usuario = buscarUsuario(idUsuario);
        String correo = normalizarCorreo(solicitud.correo());
        if (!usuario.getCorreo().equals(correo) && usuarioRepository.existsByCorreo(correo)) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado");
        }
        boolean correoCambio = !usuario.getCorreo().equals(correo);
        usuario.actualizarDatos(solicitud.nombres().trim(), solicitud.apellidos().trim(), correo);
        usuario.asignarRoles(obtenerRolesActivos(solicitud.roles()));
        if (correoCambio) {
            tokenRefrescoService.revocarTokensDelUsuario(usuario.getId());
        }
        return convertirUsuario(usuario);
    }

    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "USUARIOS", entidad = "USUARIO", accion = "ACTUALIZAR")
    public UsuarioRespuesta actualizarUsuario(Long idUsuario, ActualizarUsuarioSolicitud solicitud, Organizacion organizacion) {
        Usuario usuario = usuarioRepository.findByIdAndOrganizacionId(idUsuario, organizacion.getId()).orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado"));
        MembresiaOrganizacion membresia = membresiaRepository.findByUsuarioCorreoAndOrganizacionId(usuario.getCorreo(), organizacion.getId()).orElseThrow();
        String correo = normalizarCorreo(solicitud.correo());
        if (!usuario.getCorreo().equals(correo) && usuarioRepository.existsByCorreo(correo)) throw new IllegalArgumentException("El correo ya se encuentra registrado");
        boolean correoCambio = !usuario.getCorreo().equals(correo);
        usuario.actualizarDatos(solicitud.nombres().trim(), solicitud.apellidos().trim(), correo);
        membresia.asignarRoles(obtenerRolesActivos(solicitud.roles()));
        if (correoCambio) tokenRefrescoService.revocarTokensDelUsuario(usuario.getId());
        return convertirUsuario(usuario, organizacion);
    }

    @Transactional
    public UsuarioRespuesta cambiarEstado(Long idUsuario, EstadoUsuario estado, String correoSolicitante) {
        Usuario usuario = buscarUsuario(idUsuario);
        if (usuario.getCorreo().equals(normalizarCorreo(correoSolicitante)) && estado != usuario.getEstado()) {
            throw new IllegalArgumentException("No puedes cambiar el estado de tu propia cuenta");
        }
        usuario.actualizarEstado(estado);
        if (estado == EstadoUsuario.INACTIVO) {
            tokenRefrescoService.revocarTokensDelUsuario(usuario.getId());
        }
        return convertirUsuario(usuario);
    }

    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "USUARIOS", entidad = "USUARIO", accion = "CAMBIAR_ESTADO")
    public UsuarioRespuesta cambiarEstado(Long idUsuario, EstadoUsuario estado, String correoSolicitante, Organizacion organizacion) {
        Usuario usuario = usuarioRepository.findByIdAndOrganizacionId(idUsuario, organizacion.getId()).orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado"));
        if (usuario.getCorreo().equals(normalizarCorreo(correoSolicitante)) && estado != usuario.getEstado()) throw new IllegalArgumentException("No puedes cambiar el estado de tu propia cuenta");
        usuario.actualizarEstado(estado);
        if (estado == EstadoUsuario.INACTIVO) tokenRefrescoService.revocarTokensDelUsuario(usuario.getId());
        return convertirUsuario(usuario, organizacion);
    }

    private Usuario buscarUsuario(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado"));
    }

    private Set<Rol> obtenerRolesActivos(Set<String> nombresRoles) {
        Set<String> nombresNormalizados = (nombresRoles == null || nombresRoles.isEmpty() ? Set.of("PARTICIPANTE") : nombresRoles).stream()
                .map(nombre -> nombre.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<Rol> roles = rolRepository.findByNombreIn(nombresNormalizados);
        if (roles.size() != nombresNormalizados.size() || roles.stream().anyMatch(rol -> rol.getEstado() != EstadoRol.ACTIVO)) {
            throw new IllegalArgumentException("Uno o mas roles no existen o estan inactivos");
        }
        return Set.copyOf(roles);
    }

    private UsuarioRespuesta convertirUsuario(Usuario usuario) {
        List<String> roles = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .sorted()
                .toList();
        return new UsuarioRespuesta(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getEstado().name(),
                roles
        );
    }

    private UsuarioRespuesta convertirUsuario(Usuario usuario, Organizacion organizacion) {
        MembresiaOrganizacion membresia = membresiaRepository.findByUsuarioCorreoAndOrganizacionId(usuario.getCorreo(), organizacion.getId()).orElseThrow();
        return new UsuarioRespuesta(usuario.getId(), usuario.getNombres(), usuario.getApellidos(), usuario.getCorreo(), usuario.getEstado().name(), membresia.getRoles().stream().map(Rol::getNombre).sorted().toList());
    }

    private PaginaUsuariosRespuesta convertirPagina(Page<Usuario> usuarios, Organizacion organizacion) {
        return new PaginaUsuariosRespuesta(usuarios.map(usuario -> convertirUsuario(usuario, organizacion)).getContent(), usuarios.getNumber(), usuarios.getSize(), usuarios.getTotalElements(), usuarios.getTotalPages());
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
