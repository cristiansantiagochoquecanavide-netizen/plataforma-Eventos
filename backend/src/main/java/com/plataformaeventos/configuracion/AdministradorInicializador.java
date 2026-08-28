package com.plataformaeventos.configuracion;

import com.plataformaeventos.roles.Rol;
import com.plataformaeventos.roles.RolRepository;
import com.plataformaeventos.usuarios.EstadoUsuario;
import com.plataformaeventos.usuarios.Usuario;
import com.plataformaeventos.usuarios.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Configuration
public class AdministradorInicializador {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdministradorInicializador.class);

    @Bean
    ApplicationRunner inicializarAdministrador(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder codificadorClave,
            @Value("${ADMIN_CORREO:}") String correoAdministrador,
            @Value("${ADMIN_CLAVE:}") String claveAdministrador
    ) {
        return argumentos -> crearAdministrador(
                usuarioRepository,
                rolRepository,
                codificadorClave,
                correoAdministrador,
                claveAdministrador
        );
    }

    @Transactional
    void crearAdministrador(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder codificadorClave,
            String correoAdministrador,
            String claveAdministrador
    ) {
        if (correoAdministrador.isBlank() || claveAdministrador.isBlank()) {
            LOGGER.warn("ADMIN_CORREO y ADMIN_CLAVE no estan definidos; no se creo el administrador inicial");
            return;
        }

        String correo = correoAdministrador.trim().toLowerCase(Locale.ROOT);
        Rol rolAdministrador = rolRepository.findByNombre("ADMINISTRADOR")
                .orElseThrow(() -> new IllegalStateException("No existe el rol ADMINISTRADOR"));
        usuarioRepository.findByCorreoConRoles(correo).ifPresentOrElse(
                administradorExistente -> {
                    administradorExistente.actualizarClaveHash(codificadorClave.encode(claveAdministrador));
                    administradorExistente.actualizarEstado(EstadoUsuario.ACTIVO);
                    administradorExistente.getRoles().add(rolAdministrador);
                    usuarioRepository.save(administradorExistente);
                    LOGGER.info("Administrador inicial sincronizado");
                },
                () -> crearNuevoAdministrador(usuarioRepository, codificadorClave, correo, claveAdministrador, rolAdministrador)
        );
    }

    private void crearNuevoAdministrador(
            UsuarioRepository usuarioRepository,
            PasswordEncoder codificadorClave,
            String correo,
            String claveAdministrador,
            Rol rolAdministrador
    ) {
        Usuario administrador = new Usuario(
                "Administrador",
                "Inicial",
                correo,
                codificadorClave.encode(claveAdministrador),
                EstadoUsuario.ACTIVO
        );
        administrador.asignarRoles(Set.of(rolAdministrador));
        usuarioRepository.save(administrador);
        LOGGER.info("Administrador inicial creado");
    }
}
