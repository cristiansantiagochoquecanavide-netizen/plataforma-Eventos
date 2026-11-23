package com.plataformaeventos.configuracion;

import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.saas.servicio.SuscripcionOrganizacionService;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Optional;
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
            ObjectProvider<SuscripcionOrganizacionService> suscripciones,
            TransactionTemplate transactionTemplate,
            @Value("${ADMIN_CORREO:AdminSaaS@gmail.com}") String correoAdministrador,
            @Value("${ADMIN_CLAVE:Admin123}") String claveAdministrador
    ) {
        return argumentos -> transactionTemplate.executeWithoutResult(status -> crearAdministrador(
                usuarioRepository,
                rolRepository,
                organizacionRepository,
                membresiaRepository,
                codificadorClave,
                correoAdministrador,
                claveAdministrador,
                suscripciones.getIfAvailable()
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
        crearAdministrador(
                usuarioRepository,
                rolRepository,
                organizacionRepository,
                membresiaRepository,
                codificadorClave,
                correoAdministrador,
                claveAdministrador,
                null
        );
    }

    @Transactional
    void crearAdministrador(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            OrganizacionRepository organizacionRepository,
            MembresiaOrganizacionRepository membresiaRepository,
            PasswordEncoder codificadorClave,
            String correoAdministrador,
            String claveAdministrador,
            SuscripcionOrganizacionService suscripcionService
    ) {
        if (correoAdministrador.isBlank() || claveAdministrador.isBlank()) {
            LOGGER.warn("ADMIN_CORREO y ADMIN_CLAVE no estan definidos; no se creo el administrador inicial");
            crearUsuariosDePrueba(usuarioRepository, rolRepository, organizacionRepository, membresiaRepository, codificadorClave, suscripcionService);
            return;
        }

        String correo = correoAdministrador.trim().toLowerCase(Locale.ROOT);
        Rol rolSaasAdministrador = rolRepository.findByNombre("SAAS_ADMINISTRADOR")
                .orElseThrow(() -> new IllegalStateException("No existe el rol SAAS_ADMINISTRADOR"));
        Rol rolAdministradorTenant = rolRepository.findByNombre("ADMINISTRADOR")
                .orElseThrow(() -> new IllegalStateException("No existe el rol ADMINISTRADOR"));
        usuarioRepository.findByCorreoConRoles(correo).ifPresentOrElse(
                administradorExistente -> {
                    administradorExistente.actualizarClaveHash(codificadorClave.encode(claveAdministrador));
                    administradorExistente.actualizarEstado(EstadoUsuario.ACTIVO);
                    administradorExistente.getRoles().add(rolSaasAdministrador);
                    usuarioRepository.save(administradorExistente);
                    asegurarMembresiaPrincipal(administradorExistente, rolAdministradorTenant, organizacionRepository, membresiaRepository);
                    LOGGER.info("Administrador inicial sincronizado");
                },
                () -> crearNuevoAdministrador(usuarioRepository, organizacionRepository, membresiaRepository, codificadorClave, correo, claveAdministrador, rolSaasAdministrador, rolAdministradorTenant)
        );

        crearUsuariosDePrueba(usuarioRepository, rolRepository, organizacionRepository, membresiaRepository, codificadorClave, suscripcionService);
    }

    private void crearNuevoAdministrador(
            UsuarioRepository usuarioRepository,
            OrganizacionRepository organizacionRepository,
            MembresiaOrganizacionRepository membresiaRepository,
            PasswordEncoder codificadorClave,
            String correo,
            String claveAdministrador,
            Rol rolSaasAdministrador,
            Rol rolAdministradorTenant
    ) {
        Usuario administrador = new Usuario(
                "Administrador",
                "SaaS",
                correo,
                codificadorClave.encode(claveAdministrador),
                EstadoUsuario.ACTIVO
        );
        administrador.asignarRoles(Set.of(rolSaasAdministrador));
        usuarioRepository.save(administrador);
        asegurarMembresiaPrincipal(administrador, rolAdministradorTenant, organizacionRepository, membresiaRepository);
        LOGGER.info("Administrador inicial creado");
    }

    private void crearUsuariosDePrueba(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            OrganizacionRepository organizacionRepository,
            MembresiaOrganizacionRepository membresiaRepository,
            PasswordEncoder codificadorClave,
            SuscripcionOrganizacionService suscripcionService
    ) {
        Organizacion principal = organizacionRepository.findBySlug("principal")
                .orElseThrow(() -> new IllegalStateException("No existe la organizacion principal"));
        if (suscripcionService != null && principal.getId() != null) {
            suscripcionService.crearPlanBaseSiFalta(principal);
        }

        // Tenant 1: Organizacion principal
        crearUsuarioEnOrganizacionSiNoExiste(usuarioRepository, rolRepository, membresiaRepository, codificadorClave, principal, "Administrador", "Principal", "AdminPrincipal@gmail.com", "Principal123", "ADMINISTRADOR");
        crearUsuarioEnOrganizacionSiNoExiste(usuarioRepository, rolRepository, membresiaRepository, codificadorClave, principal, "Organizador", "Principal", "OrganizadorPrincipal@gmail.com", "OrganizadorPrincipal123", "ORGANIZADOR");
        crearUsuarioEnOrganizacionSiNoExiste(usuarioRepository, rolRepository, membresiaRepository, codificadorClave, principal, "Staff", "Principal", "StaffPrincipal@gmail.com", "StaffPrincipal123", "STAFF");

        // Tenant 2: We Are One World
        Optional<Organizacion> waowOpt = asegurarOrganizacion(organizacionRepository, "We Are One World", "waow-conciertos");
        if (waowOpt.isPresent()) {
            Organizacion waow = waowOpt.get();
            if (suscripcionService != null && waow.getId() != null) {
                suscripcionService.crearPlanBaseSiFalta(waow);
            }
            crearUsuarioEnOrganizacionSiNoExiste(usuarioRepository, rolRepository, membresiaRepository, codificadorClave, waow, "Matteo", "Espinoza", "AdminWaow@gmail.com", "Waow123", "ADMINISTRADOR");
            crearUsuarioEnOrganizacionSiNoExiste(usuarioRepository, rolRepository, membresiaRepository, codificadorClave, waow, "Organizador", "Waow", "OrganizadorWaow@gmail.com", "OrganizadorWaow123", "ORGANIZADOR");
            crearUsuarioEnOrganizacionSiNoExiste(usuarioRepository, rolRepository, membresiaRepository, codificadorClave, waow, "Staff", "Waow", "StaffWaow@gmail.com", "StaffWaow123", "STAFF");
        }

        // Cuenta unica de participante compartida para el publico general
        crearUsuarioEnOrganizacionSiNoExiste(usuarioRepository, rolRepository, membresiaRepository, codificadorClave, principal, "Participante", "Eventos", "ParticipanteEventos@gmail.com", "Participante123", "PARTICIPANTE");
        if (waowOpt.isPresent()) {
            Rol rolParticipante = rolRepository.findByNombre("PARTICIPANTE")
                    .orElseThrow(() -> new IllegalStateException("No existe el rol PARTICIPANTE"));
            usuarioRepository.findByCorreo("participanteeventos@gmail.com")
                    .ifPresent(u -> asegurarMembresiaOrganizacion(u, waowOpt.get(), rolParticipante, membresiaRepository));
        }
    }

    private Optional<Organizacion> asegurarOrganizacion(OrganizacionRepository organizaciones, String nombre, String slug) {
        Optional<Organizacion> existente = organizaciones.findBySlug(slug);
        if (existente.isPresent()) {
            return existente;
        }
        return Optional.ofNullable(organizaciones.save(new Organizacion(nombre, slug)));
    }

    private void crearUsuarioEnOrganizacionSiNoExiste(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            MembresiaOrganizacionRepository membresiaRepository,
            PasswordEncoder codificadorClave,
            Organizacion organizacion,
            String nombres,
            String apellidos,
            String correoRaw,
            String claveRaw,
            String nombreRol
    ) {
        String correo = correoRaw.trim().toLowerCase(Locale.ROOT);
        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + nombreRol));
        if (usuarioRepository.existsByCorreo(correo)) {
            usuarioRepository.findByCorreo(correo)
                    .ifPresent(usuario -> asegurarMembresiaOrganizacion(usuario, organizacion, rol, membresiaRepository));
            return;
        }
        Usuario usuario = new Usuario(
                nombres,
                apellidos,
                correo,
                codificadorClave.encode(claveRaw),
                EstadoUsuario.ACTIVO
        );
        usuario.asignarRoles(Set.of(rol));
        usuarioRepository.save(usuario);
        asegurarMembresiaOrganizacion(usuario, organizacion, rol, membresiaRepository);
        LOGGER.info("Usuario inicial creado para {}: {} ({})", organizacion.getSlug(), correo, nombreRol);
    }

    private void asegurarMembresiaPrincipal(Usuario usuario, Rol rol, OrganizacionRepository organizaciones, MembresiaOrganizacionRepository membresias) {
        Organizacion principal = organizaciones.findBySlug("principal").orElseThrow(() -> new IllegalStateException("No existe la organizacion principal"));
        asegurarMembresiaOrganizacion(usuario, principal, rol, membresias);
    }

    private void asegurarMembresiaOrganizacion(Usuario usuario, Organizacion organizacion, Rol rol, MembresiaOrganizacionRepository membresias) {
        MembresiaOrganizacion membresia = membresias.findByUsuarioIdAndOrganizacionIdConRoles(usuario.getId(), organizacion.getId())
                .orElseGet(() -> new MembresiaOrganizacion(usuario, organizacion));
        membresia.activar();
        membresia.getRoles().add(rol);
        membresias.save(membresia);
    }
}
