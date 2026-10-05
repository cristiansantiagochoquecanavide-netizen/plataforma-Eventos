package com.plataformaeventos.configuracion;

import com.plataformaeventos.asignacionesstaff.modelo.AsignacionStaff;
import com.plataformaeventos.asignacionesstaff.modelo.EstadoAsignacionStaff;
import com.plataformaeventos.asignacionesstaff.repositorio.AsignacionStaffRepository;
import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.convocatorias.modelo.Convocatoria;
import com.plataformaeventos.convocatorias.modelo.TipoPersonal;
import com.plataformaeventos.convocatorias.repositorio.ConvocatoriaRepository;
import com.plataformaeventos.espacios.modelo.Espacio;
import com.plataformaeventos.espacios.repositorio.EspacioRepository;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.postulacionesstaff.modelo.PostulacionStaff;
import com.plataformaeventos.postulacionesstaff.repositorio.PostulacionStaffRepository;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.saas.servicio.SuscripcionOrganizacionService;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;

@Configuration
@ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true")
public class DemoDatosInicializador {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDatosInicializador.class);
    static final String CLAVE_DEMO = "Demo123!";

    @Bean
    ApplicationRunner inicializarDatosDemo(
            OrganizacionRepository organizaciones,
            UsuarioRepository usuarios,
            RolRepository roles,
            MembresiaOrganizacionRepository membresias,
            CategoriaRepository categorias,
            EspacioRepository espacios,
            EventoRepository eventos,
            ConvocatoriaRepository convocatorias,
            PostulacionStaffRepository postulaciones,
            AsignacionStaffRepository asignaciones,
            InscripcionRepository inscripciones,
            PasswordEncoder codificadorClave,
            ObjectProvider<SuscripcionOrganizacionService> suscripciones,
            TransactionTemplate transactionTemplate
    ) {
        return argumentos -> transactionTemplate.executeWithoutResult(status -> crearDatosDemo(
                organizaciones,
                usuarios,
                roles,
                membresias,
                categorias,
                espacios,
                eventos,
                convocatorias,
                postulaciones,
                asignaciones,
                inscripciones,
                codificadorClave,
                suscripciones.getIfAvailable()
        ));
    }

    @Transactional
    void crearDatosDemo(
            OrganizacionRepository organizaciones,
            UsuarioRepository usuarios,
            RolRepository roles,
            MembresiaOrganizacionRepository membresias,
            CategoriaRepository categorias,
            EspacioRepository espacios,
            EventoRepository eventos,
            ConvocatoriaRepository convocatorias,
            PostulacionStaffRepository postulaciones,
            AsignacionStaffRepository asignaciones,
            InscripcionRepository inscripciones,
            PasswordEncoder codificadorClave
    ) {
        crearDatosDemo(organizaciones, usuarios, roles, membresias, categorias, espacios, eventos,
                convocatorias, postulaciones, asignaciones, inscripciones, codificadorClave, null);
    }

    @Transactional
    void crearDatosDemo(
            OrganizacionRepository organizaciones,
            UsuarioRepository usuarios,
            RolRepository roles,
            MembresiaOrganizacionRepository membresias,
            CategoriaRepository categorias,
            EspacioRepository espacios,
            EventoRepository eventos,
            ConvocatoriaRepository convocatorias,
            PostulacionStaffRepository postulaciones,
            AsignacionStaffRepository asignaciones,
            InscripcionRepository inscripciones,
            PasswordEncoder codificadorClave,
            SuscripcionOrganizacionService suscripciones
    ) {
        Rol administrador = rol(roles, "ADMINISTRADOR");
        Rol organizador = rol(roles, "ORGANIZADOR");
        Rol staff = rol(roles, "STAFF");
        Rol participante = rol(roles, "PARTICIPANTE");

        sembrarTenantDemo(new TenantDemo(
                        "Universidad Norte Demo",
                        "demo-universidad-norte",
                        "Demo Norte",
                        "demo.norte.organizador@plataformaeventos.local",
                        "demo.norte.staff@plataformaeventos.local",
                        "demo.norte.participante@plataformaeventos.local",
                        "Capacitacion Academica",
                        "Aula Magna Norte",
                        "Campus Norte - Bloque A",
                        "Jornada Docente Demo Norte",
                        "Formacion academica multi-tenant para la organizacion norte",
                        "Apoyo en acreditacion y orientacion de asistentes",
                        "Acreditacion de asistentes Norte",
                        0),
                organizaciones, usuarios, membresias, categorias, espacios, eventos, convocatorias,
                postulaciones, asignaciones, inscripciones, codificadorClave, administrador, organizador, staff, participante);

        sembrarTenantDemo(new TenantDemo(
                        "Centro Cultural Sur Demo",
                        "demo-centro-cultural-sur",
                        "Demo Sur",
                        "demo.sur.organizador@plataformaeventos.local",
                        "demo.sur.staff@plataformaeventos.local",
                        "demo.sur.participante@plataformaeventos.local",
                        "Cultura Comunitaria",
                        "Sala Experimental Sur",
                        "Centro Cultural Sur - Sala 2",
                        "Festival Comunitario Demo Sur",
                        "Evento cultural multi-tenant para la organizacion sur",
                        "Apoyo de sala y coordinacion de acceso",
                        "Recepcion de sala Sur",
                        3),
                organizaciones, usuarios, membresias, categorias, espacios, eventos, convocatorias,
                postulaciones, asignaciones, inscripciones, codificadorClave, administrador, organizador, staff, participante);

        if (suscripciones != null) {
            organizaciones.findBySlug("demo-universidad-norte").ifPresent(suscripciones::crearPlanBaseSiFalta);
            organizaciones.findBySlug("demo-centro-cultural-sur").ifPresent(suscripciones::crearPlanBaseSiFalta);
        }

        LOGGER.info("Datos demo multi-tenant sincronizados");
    }

    private void sembrarTenantDemo(
            TenantDemo demo,
            OrganizacionRepository organizaciones,
            UsuarioRepository usuarios,
            MembresiaOrganizacionRepository membresias,
            CategoriaRepository categorias,
            EspacioRepository espacios,
            EventoRepository eventos,
            ConvocatoriaRepository convocatorias,
            PostulacionStaffRepository postulaciones,
            AsignacionStaffRepository asignaciones,
            InscripcionRepository inscripciones,
            PasswordEncoder codificadorClave,
            Rol administrador,
            Rol organizador,
            Rol staff,
            Rol participante
    ) {
        Organizacion organizacion = organizaciones.findBySlug(demo.slug())
                .orElseGet(() -> organizaciones.save(new Organizacion(demo.nombreOrganizacion(), demo.slug())));

        Usuario usuarioOrganizador = asegurarUsuario(usuarios, codificadorClave, demo.organizadorCorreo(), demo.prefijo(), "Organizador", organizador);
        Usuario usuarioStaff = asegurarUsuario(usuarios, codificadorClave, demo.staffCorreo(), demo.prefijo(), "Staff", staff);
        Usuario usuarioParticipante = asegurarUsuario(usuarios, codificadorClave, demo.participanteCorreo(), demo.prefijo(), "Participante", participante);

        asegurarMembresia(membresias, usuarioOrganizador, organizacion, Set.of(administrador, organizador));
        asegurarMembresia(membresias, usuarioStaff, organizacion, Set.of(staff));
        asegurarMembresia(membresias, usuarioParticipante, organizacion, Set.of(participante));

        Categoria categoria = categorias.findFirstByNombreIgnoreCaseAndOrganizacionId(demo.nombreCategoria(), organizacion.getId())
                .orElseGet(() -> categorias.save(new Categoria(demo.nombreCategoria(), "Categoria local para datos demo", organizacion)));
        Espacio espacio = espacios.findFirstByNombreIgnoreCaseAndOrganizacionId(demo.nombreEspacio(), organizacion.getId())
                .orElseGet(() -> {
                    Espacio nuevo = new Espacio(demo.nombreEspacio(), demo.ubicacionEspacio(), 120);
                    nuevo.asignarOrganizacion(organizacion);
                    return espacios.save(nuevo);
                });

        OffsetDateTime inicio = OffsetDateTime.now(ZoneOffset.UTC)
                .plusMonths(6)
                .plusDays(demo.desplazamientoDias())
                .withHour(14)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        OffsetDateTime fin = inicio.plusHours(4);
        Evento evento = eventos.findFirstByNombreIgnoreCaseAndOrganizacionId(demo.nombreEvento(), organizacion.getId())
                .orElseGet(() -> {
                    Evento nuevo = new Evento(demo.nombreEvento(), demo.descripcionEvento(), inicio, fin, demo.nombreEspacio(),
                            EstadoEvento.PUBLICADO, categoria, espacio, 80);
                    nuevo.asignarOrganizacion(organizacion);
                    nuevo.asignarResponsables(Set.of(usuarioOrganizador));
                    return eventos.save(nuevo);
                });

        Convocatoria convocatoria = convocatorias.findFirstByEventoIdAndOrganizacionIdAndTipoPersonal(evento.getId(), organizacion.getId(), TipoPersonal.STAFF)
                .orElseGet(() -> {
                    Convocatoria nueva = new Convocatoria(evento, TipoPersonal.STAFF, demo.descripcionConvocatoria(), 2, LocalDate.now().plusMonths(5));
                    nueva.asignarOrganizacion(organizacion);
                    return convocatorias.save(nueva);
                });

        PostulacionStaff postulacion = postulaciones.findByConvocatoriaIdAndUsuarioIdAndOrganizacionId(convocatoria.getId(), usuarioStaff.getId(), organizacion.getId())
                .orElseGet(() -> {
                    PostulacionStaff nueva = new PostulacionStaff(convocatoria, usuarioStaff, organizacion, "Postulacion demo aceptada para mostrar asignacion de staff");
                    nueva.aceptar();
                    return postulaciones.save(nueva);
                });

        if (!asignaciones.existsByPostulacionIdAndOrganizacionIdAndEstado(postulacion.getId(), organizacion.getId(), EstadoAsignacionStaff.ASIGNADA)) {
            asignaciones.save(new AsignacionStaff(postulacion, organizacion, demo.tareaAsignacion(), inicio.minusHours(1), inicio.plusHours(1), "Asignacion demo local"));
        }

        inscripciones.findByEventoIdAndParticipanteIdAndOrganizacionId(evento.getId(), usuarioParticipante.getId(), organizacion.getId())
                .ifPresentOrElse(
                        inscripcion -> {
                            if (!inscripcion.estaConfirmada()) {
                                inscripcion.reactivar(codigoQr(demo.slug(), usuarioParticipante));
                                inscripciones.save(inscripcion);
                            }
                        },
                        () -> {
                            Inscripcion nueva = new Inscripcion(evento, usuarioParticipante, codigoQr(demo.slug(), usuarioParticipante));
                            nueva.asignarOrganizacion(organizacion);
                            inscripciones.save(nueva);
                        });
    }

    private Usuario asegurarUsuario(UsuarioRepository usuarios, PasswordEncoder codificadorClave, String correoRaw, String prefijo, String rolTexto, Rol rol) {
        String correo = correoRaw.trim().toLowerCase(Locale.ROOT);
        return usuarios.findByCorreoConRoles(correo)
                .map(existente -> {
                    existente.actualizarClaveHash(codificadorClave.encode(CLAVE_DEMO));
                    existente.actualizarEstado(EstadoUsuario.ACTIVO);
                    existente.getRoles().add(rol);
                    return usuarios.save(existente);
                })
                .orElseGet(() -> {
                    Usuario nuevo = new Usuario(prefijo, rolTexto, correo, codificadorClave.encode(CLAVE_DEMO), EstadoUsuario.ACTIVO);
                    nuevo.asignarRoles(Set.of(rol));
                    return usuarios.save(nuevo);
                });
    }

    private void asegurarMembresia(MembresiaOrganizacionRepository membresias, Usuario usuario, Organizacion organizacion, Set<Rol> roles) {
        MembresiaOrganizacion membresia = membresias.findByUsuarioIdAndOrganizacionIdConRoles(usuario.getId(), organizacion.getId())
                .orElseGet(() -> new MembresiaOrganizacion(usuario, organizacion));
        membresia.activar();
        membresia.getRoles().addAll(roles);
        membresias.save(membresia);
    }

    private Rol rol(RolRepository roles, String nombre) {
        return roles.findByNombre(nombre).orElseThrow(() -> new IllegalStateException("No existe el rol " + nombre));
    }

    private String codigoQr(String slug, Usuario participante) {
        return "demo-" + slug + "-" + participante.getCorreo();
    }

    private record TenantDemo(
            String nombreOrganizacion,
            String slug,
            String prefijo,
            String organizadorCorreo,
            String staffCorreo,
            String participanteCorreo,
            String nombreCategoria,
            String nombreEspacio,
            String ubicacionEspacio,
            String nombreEvento,
            String descripcionEvento,
            String descripcionConvocatoria,
            String tareaAsignacion,
            int desplazamientoDias
    ) {
    }
}
