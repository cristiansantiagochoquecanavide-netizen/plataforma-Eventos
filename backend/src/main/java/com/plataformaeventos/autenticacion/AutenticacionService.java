package com.plataformaeventos.autenticacion;
 
import com.plataformaeventos.autenticacion.dto.*;

import com.plataformaeventos.excepciones.ExcepcionNoAutorizado;
import com.plataformaeventos.permisos.Permiso;
import com.plataformaeventos.roles.EstadoRol;
import com.plataformaeventos.roles.Rol;
import com.plataformaeventos.roles.RolRepository;
import com.plataformaeventos.seguridad.JwtService;
import com.plataformaeventos.seguridad.RecuperacionClaveService;
import com.plataformaeventos.seguridad.TokenRefrescoService;
import com.plataformaeventos.usuarios.EstadoUsuario;
import com.plataformaeventos.usuarios.Usuario;
import com.plataformaeventos.usuarios.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder codificadorClave;
    private final JwtService jwtService;
    private final TokenRefrescoService tokenRefrescoService;
    private final RecuperacionClaveService recuperacionClaveService;

    public AutenticacionService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder codificadorClave,
            JwtService jwtService,
            TokenRefrescoService tokenRefrescoService,
            RecuperacionClaveService recuperacionClaveService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.codificadorClave = codificadorClave;
        this.jwtService = jwtService;
        this.tokenRefrescoService = tokenRefrescoService;
        this.recuperacionClaveService = recuperacionClaveService;
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
        return convertirUsuario(usuarioRepository.save(usuario));
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
        Usuario usuario = usuarioRepository.findByCorreoConRolesYPermisos(normalizarCorreo(correo))
                .orElseThrow(() -> new ExcepcionNoAutorizado("Usuario no encontrado"));
        return convertirUsuario(usuario);
    }

    private SesionRespuesta crearSesion(Usuario usuario) {
        return new SesionRespuesta(
                jwtService.generarTokenAcceso(usuario.getCorreo()),
                tokenRefrescoService.crearToken(usuario),
                "Bearer"
        );
    }

    private UsuarioActualRespuesta convertirUsuario(Usuario usuario) {
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
                obtenerPermisos(usuario)
        );
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
