package com.plataformaeventos.configuracion;

import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Set;

@Configuration
public class AdministradorInicializador {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdministradorInicializador.class);

    @Bean
    ApplicationRunner inicializarAdministrador(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            OrganizacionRepository organizacionRepository,
            MembresiaOrganizacionRepository membresiaRepository,
            PasswordEncoder codificadorClave,
            TransactionTemplate transactionTemplate,
            @Value("${ADMIN_CORREO:AdminEventos@gmail.com}") String correoAdministrador,
            @Value("${ADMIN_CLAVE:Admin123}") String claveAdministrador
    ) {
        return argumentos -> transactionTemplate.executeWithoutResult(status -> crearAdministrador(
                usuarioRepository,
                rolRepository,
                organizacionRepository,
                membresiaRepository,
                codificadorClave,
                correoAdministrador,
                claveAdministrador
        ));
    }

    @Transactional
    void crearAdministrador(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            OrganizacionRepository organizacionRepository,
            MembresiaOrganizacionRepository membresiaRepository,
            PasswordEncoder codificadorClave,
            String correoAdministrador,
            String claveAdministrador
    ) {
        if (correoAdministrador.isBlank() || claveAdministrador.isBlank()) {
            LOGGER.warn("ADMIN_CORREO y ADMIN_CLAVE no estan definidos; no se creo el administrador inicial");
            crearUsuariosDePrueba(usuarioRepository, rolRepository, organizacionRepository, membresiaRepository, codificadorClave);
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
                    asegurarMembresiaPrincipal(administradorExistente, rolAdministrador, organizacionRepository, membresiaRepository);
                    LOGGER.info("Administrador inicial sincronizado");
                },
                () -> crearNuevoAdministrador(usuarioRepository, organizacionRepository, membresiaRepository, codificadorClave, correo, claveAdministrador, rolAdministrador)
        );

        crearUsuariosDePrueba(usuarioRepository, rolRepository, organizacionRepository, membresiaRepository, codificadorClave);
    }

    private void crearNuevoAdministrador(
            UsuarioRepository usuarioRepository,
            OrganizacionRepository organizacionRepository,
            MembresiaOrganizacionRepository membresiaRepository,
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
        asegurarMembresiaPrincipal(administrador, rolAdministrador, organizacionRepository, membresiaRepository);
        LOGGER.info("Administrador inicial creado");
    }

    private void crearUsuariosDePrueba(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            OrganizacionRepository organizacionRepository,
            MembresiaOrganizacionRepository membresiaRepository,
            PasswordEncoder codificadorClave
    ) {
        crearUsuarioSiNoExiste(usuarioRepository, rolRepository, organizacionRepository, membresiaRepository, codificadorClave, "Organizador", "Logistica", "OrganizadorEventos@gmail.com", "Organizador123", "ORGANIZADOR");
        crearUsuarioSiNoExiste(usuarioRepository, rolRepository, organizacionRepository, membresiaRepository, codificadorClave, "Staff", "Apoyo", "StaffEventos@gmail.com", "Staff123", "STAFF");
        crearUsuarioSiNoExiste(usuarioRepository, rolRepository, organizacionRepository, membresiaRepository, codificadorClave, "Participante", "Publico", "ParticipanteEvento@gmail.com", "Participante123", "PARTICIPANTE");
    }

    private void crearUsuarioSiNoExiste(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            OrganizacionRepository organizacionRepository,
            MembresiaOrganizacionRepository membresiaRepository,
            PasswordEncoder codificadorClave,
            String nombres,
            String apellidos,
            String correoRaw,
            String claveRaw,
            String nombreRol
    ) {
        String correo = correoRaw.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByCorreo(correo)) {
            usuarioRepository.findByCorreo(correo).ifPresent(usuario -> asegurarMembresiaPrincipal(
                    usuario,
                    rolRepository.findByNombre(nombreRol).orElseThrow(() -> new IllegalStateException("No existe el rol " + nombreRol)),
                    organizacionRepository,
                    membresiaRepository
            ));
            return;
        }
        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + nombreRol));
        Usuario usuario = new Usuario(
                nombres,
                apellidos,
                correo,
                codificadorClave.encode(claveRaw),
                EstadoUsuario.ACTIVO
        );
        usuario.asignarRoles(Set.of(rol));
        usuarioRepository.save(usuario);
        asegurarMembresiaPrincipal(usuario, rol, organizacionRepository, membresiaRepository);
        LOGGER.info("Usuario de prueba inicial creado con rol: {}", nombreRol);
    }

    private void asegurarMembresiaPrincipal(Usuario usuario, Rol rol, OrganizacionRepository organizaciones, MembresiaOrganizacionRepository membresias) {
        Organizacion principal = organizaciones.findBySlug("principal").orElseThrow(() -> new IllegalStateException("No existe la organizacion principal"));
        MembresiaOrganizacion membresia = membresias.findByUsuarioIdAndOrganizacionIdConRoles(usuario.getId(), principal.getId())
                .orElseGet(() -> new MembresiaOrganizacion(usuario, principal));
        membresia.activar();
        membresia.getRoles().add(rol);
        membresias.save(membresia);
    }
}
