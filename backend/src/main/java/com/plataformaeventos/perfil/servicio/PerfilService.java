package com.plataformaeventos.perfil.servicio;

import com.plataformaeventos.perfil.dto.*;

import com.plataformaeventos.excepciones.ExcepcionNoAutorizado;
import com.plataformaeventos.permisos.modelo.Permiso;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.seguridad.TokenRefrescoService;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRefrescoService tokenRefrescoService;
    private final PasswordEncoder codificadorClave;

    public PerfilService(
            UsuarioRepository usuarioRepository,
            TokenRefrescoService tokenRefrescoService,
            PasswordEncoder codificadorClave
    ) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRefrescoService = tokenRefrescoService;
        this.codificadorClave = codificadorClave;
    }

    @Transactional(readOnly = true)
    public PerfilRespuesta obtenerPerfil(String correo) {
        return convertirPerfil(buscarUsuario(correo));
    }

    @Transactional
    public PerfilRespuesta actualizarPerfil(String correoActual, ActualizarPerfilSolicitud solicitud) {
        Usuario usuario = buscarUsuario(correoActual);
        String nuevoCorreo = normalizarCorreo(solicitud.correo());
        if (!usuario.getCorreo().equals(nuevoCorreo) && usuarioRepository.existsByCorreo(nuevoCorreo)) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado");
        }

        boolean correoCambio = !usuario.getCorreo().equals(nuevoCorreo);
        usuario.actualizarDatos(solicitud.nombres().trim(), solicitud.apellidos().trim(), nuevoCorreo);
        if (correoCambio) {
            tokenRefrescoService.revocarTokensDelUsuario(usuario.getId());
        }
        return convertirPerfil(usuario);
    }

    @Transactional
    public void cambiarClave(String correo, CambiarClaveSolicitud solicitud) {
        Usuario usuario = buscarUsuario(correo);
        if (!codificadorClave.matches(solicitud.claveActual(), usuario.getClaveHash())) {
            throw new IllegalArgumentException("La clave actual es incorrecta");
        }
        usuario.actualizarClaveHash(codificadorClave.encode(solicitud.nuevaClave()));
        tokenRefrescoService.revocarTokensDelUsuario(usuario.getId());
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreoConRolesYPermisos(normalizarCorreo(correo))
                .orElseThrow(() -> new ExcepcionNoAutorizado("Usuario no encontrado"));
    }

    private PerfilRespuesta convertirPerfil(Usuario usuario) {
        List<String> roles = usuario.getRoles().stream()
                .map(Rol::getNombre)
                .sorted()
                .toList();
        return new PerfilRespuesta(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
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
