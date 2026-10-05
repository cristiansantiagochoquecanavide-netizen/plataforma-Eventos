package com.plataformaeventos.seguridad;

import com.plataformaeventos.permisos.modelo.Permiso;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

@Service
public class DetallesUsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public DetallesUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoConRolesYPermisos(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getClaveHash())
                .disabled(usuario.getEstado() != EstadoUsuario.ACTIVO)
                .authorities(usuario.getRoles().stream()
                        .filter(rol -> rol.getEstado() == EstadoRol.ACTIVO)
                        .flatMap(rol -> Stream.concat(
                                Stream.of(rol.getNombre()),
                                rol.getPermisos().stream().map(Permiso::getNombre)
                        ))
                        .distinct()
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .build();
    }
}
