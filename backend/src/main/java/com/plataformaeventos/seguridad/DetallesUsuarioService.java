package com.plataformaeventos.seguridad;

import com.plataformaeventos.permisos.Permiso;
import com.plataformaeventos.roles.EstadoRol;
import com.plataformaeventos.roles.Rol;
import com.plataformaeventos.usuarios.EstadoUsuario;
import com.plataformaeventos.usuarios.Usuario;
import com.plataformaeventos.usuarios.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetallesUsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public DetallesUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getClaveHash())
                .disabled(usuario.getEstado() != EstadoUsuario.ACTIVO)
                .authorities(usuario.getRoles().stream()
                        .filter(rol -> rol.getEstado() == EstadoRol.ACTIVO)
                        .flatMap(rol -> rol.getPermisos().stream())
                        .map(Permiso::getNombre)
                        .distinct()
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .build();
    }
}
