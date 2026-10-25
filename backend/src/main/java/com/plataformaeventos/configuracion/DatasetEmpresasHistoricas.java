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
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.postulacionesstaff.modelo.EstadoPostulacionStaff;
import com.plataformaeventos.postulacionesstaff.modelo.PostulacionStaff;
import com.plataformaeventos.postulacionesstaff.repositorio.PostulacionStaffRepository;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.saas.servicio.SuscripcionOrganizacionService;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Seed opt-in: cinco organizaciones ficticias con actividad en siete meses calendario. */
final class DatasetEmpresasHistoricas {
    static final int MESES_HISTORICOS = 7;
    static final int EVENTOS_POR_EMPRESA = MESES_HISTORICOS * 2;
    private static final ZoneOffset ZONA_BOLIVIA = ZoneOffset.ofHours(-4);
    private static final Locale ESPANOL_BOLIVIA = Locale.forLanguageTag("es-BO");

    private static final List<Persona> PARTICIPANTES = List.of(
            new Persona("Ana", "Quispe"), new Persona("Mateo", "Rojas"),
            new Persona("Camila", "Flores"), new Persona("Diego", "Mamani"),
            new Persona("Lucía", "Paredes"), new Persona("Bruno", "Vargas"),
            new Persona("Valeria", "Gutiérrez"), new Persona("Andrés", "Salazar"),
            new Persona("Daniela", "Arce"), new Persona("Joaquín", "Suárez"),
            new Persona("Fernanda", "Limachi"), new Persona("Nicolás", "Céspedes"),
            new Persona("Mariana", "Torrez"), new Persona("Sebastián", "Copa"),
            new Persona("Carla", "Montaño"), new Persona("Rodrigo", "Villca"),
            new Persona("Natalia", "Soria"), new Persona("Mauricio", "Alarcón"),
            new Persona("Paola", "Loayza"), new Persona("Álvaro", "Cárdenas"),
            new Persona("Gabriela", "Medina"), new Persona("Tomás", "Rivero"),
            new Persona("Micaela", "Paz"), new Persona("René", "Choque")
    );

    private static final List<Empresa> EMPRESAS = List.of(
            new Empresa("Fundación Suma Qamaña", "fundacion-suma-qamana", "sumaqamana.local",
                    List.of("Innovación social", "Formación comunitaria"),
                    List.of(new Sede("Casa Kullaka", "Sopocachi, La Paz", 180),
                            new Sede("Auditorio Illimani", "Centro, La Paz", 260)),
                    List.of(new Persona("Lucía", "Arce"), new Persona("Andrés", "Mercado")),
                    List.of(new Persona("Marco", "Nina"), new Persona("Daniela", "Choque"), new Persona("Jaime", "Villarroel")),
                    List.of(new Serie("Encuentro de Liderazgo Comunitario", 0,
                                    "Diálogo de organizaciones vecinales y equipos de trabajo comunitario."),
                            new Serie("Taller de Economía Solidaria", 1,
                                    "Jornada práctica de intercambio, producción y redes locales."))),
            new Empresa("Nexo Andino Tecnología", "nexo-andino-tecnologia", "nexoandino.local",
                    List.of("Tecnología", "Emprendimiento"),
                    List.of(new Sede("Hub Sopocachi", "Av. 20 de Octubre, La Paz", 150),
                            new Sede("Sala Illa", "Calacoto, La Paz", 90)),
                    List.of(new Persona("Matías", "Céspedes"), new Persona("Paola", "Ribera")),
                    List.of(new Persona("Gabriel", "Poma"), new Persona("Mara", "Soliz"), new Persona("Iván", "Calle")),
                    List.of(new Serie("Foro de Innovación Andina", 0,
                                    "Conversación entre empresas, universidades y comunidades tecnológicas."),
                            new Serie("Laboratorio de Producto Digital", 1,
                                    "Taller aplicado de diseño, pruebas y aprendizaje de productos digitales."))),
            new Empresa("Casa Cultural La Estación", "casa-cultural-la-estacion", "laestacion.local",
                    List.of("Artes escénicas", "Memoria y patrimonio"),
                    List.of(new Sede("Teatro La Estación", "Recoleta, Cochabamba", 220),
                            new Sede("Sala Tunari", "Queru Queru, Cochabamba", 110)),
                    List.of(new Persona("Mariela", "Vargas"), new Persona("Bruno", "Alarcón")),
                    List.of(new Persona("Elena", "Camacho"), new Persona("Fabián", "Cruz"), new Persona("Rocío", "Terceros")),
                    List.of(new Serie("Ciclo de Cine y Memoria", 1,
                                    "Proyección y conversación sobre historias y memoria local."),
                            new Serie("Escena Abierta del Valle", 0,
                                    "Muestra de propuestas escénicas y encuentro con artistas."))),
            new Empresa("Cámara Creativa del Valle", "camara-creativa-del-valle", "camaravalle.local",
                    List.of("Negocios creativos", "Comunicación"),
                    List.of(new Sede("Centro de Convenciones Tunari", "Queru Queru, Cochabamba", 300),
                            new Sede("Aula Empresarial", "Centro, Cochabamba", 80)),
                    List.of(new Persona("Sofía", "Rocha"), new Persona("Gonzalo", "Herrera")),
                    List.of(new Persona("Marcelo", "Durán"), new Persona("Irene", "Paz"), new Persona("Raúl", "Navia")),
                    List.of(new Serie("Ronda de Negocios Creativos", 0,
                                    "Reunión de emprendimientos culturales, proveedores y aliados."),
                            new Serie("Clínica de Comunicación para Marcas", 1,
                                    "Sesión práctica para mejorar mensajes y presentaciones comerciales."))),
            new Empresa("Club Deportivo Illimani", "club-deportivo-illimani", "clubillimani.local",
                    List.of("Deporte y bienestar", "Formación deportiva"),
                    List.of(new Sede("Complejo Miraflores", "Miraflores, La Paz", 240),
                            new Sede("Cancha San Jorge", "San Jorge, La Paz", 140)),
                    List.of(new Persona("Javier", "Cárdenas"), new Persona("Paula", "Condori")),
                    List.of(new Persona("Héctor", "Yujra"), new Persona("Belén", "Alba"), new Persona("Óscar", "Fernández")),
                    List.of(new Serie("Jornada Deportiva Familiar", 0,
                                    "Encuentro recreativo para familias y escuelas deportivas."),
                            new Serie("Encuentro de Preparación Física", 1,
                                    "Sesión guiada sobre preparación, recuperación y hábitos saludables.")))
    );

    private DatasetEmpresasHistoricas() { }

    static List<Empresa> empresas() {
        return EMPRESAS;
    }

    static void sembrar(
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
        Rol admin = rol(roles, "ADMINISTRADOR");
        Rol organizador = rol(roles, "ORGANIZADOR");
        Rol staff = rol(roles, "STAFF");
        Rol participante = rol(roles, "PARTICIPANTE");
        String claveDemoHash = codificadorClave.encode(DemoDatosInicializador.CLAVE_DEMO);
        OffsetDateTime ahora = OffsetDateTime.now(ZONA_BOLIVIA);
        YearMonth mesActual = YearMonth.from(ahora);
        OffsetDateTime inicioHistorial = mesActual.minusMonths(6).atDay(1).atStartOfDay()
                .atOffset(ZONA_BOLIVIA);

        for (int empresaIndice = 0; empresaIndice < EMPRESAS.size(); empresaIndice++) {
            Empresa empresa = EMPRESAS.get(empresaIndice);
            Organizacion organizacion = organizaciones.findBySlug(empresa.slug())
                    .orElseGet(() -> organizaciones.saveAndFlush(new Organizacion(empresa.nombre(), empresa.slug())));
            List<Usuario> organizadores = empresa.organizadores().stream()
                    .map(persona -> asegurarUsuario(usuarios, claveDemoHash, empresa, persona, organizador, inicioHistorial)).toList();
            List<Usuario> equipo = empresa.staff().stream()
                    .map(persona -> asegurarUsuario(usuarios, claveDemoHash, empresa, persona, staff, inicioHistorial)).toList();
            List<Usuario> asistentes = PARTICIPANTES.stream()
                    .map(persona -> asegurarUsuario(usuarios, claveDemoHash, empresa, persona, participante, inicioHistorial)).toList();

            organizadores.forEach(usuario -> asegurarMembresia(membresias, usuario, organizacion, Set.of(admin, organizador)));
            equipo.forEach(usuario -> asegurarMembresia(membresias, usuario, organizacion, Set.of(staff)));
            asistentes.forEach(usuario -> asegurarMembresia(membresias, usuario, organizacion, Set.of(participante)));

            List<Categoria> categoriasEmpresa = empresa.categorias().stream()
                    .map(nombre -> categorias.findFirstByNombreIgnoreCaseAndOrganizacionId(nombre, organizacion.getId())
                            .orElseGet(() -> categorias.save(new Categoria(nombre,
                                    "Actividades de " + empresa.nombre(), organizacion))))
                    .toList();
            List<Espacio> sedes = empresa.sedes().stream()
                    .map(sede -> espacios.findFirstByNombreIgnoreCaseAndOrganizacionId(sede.nombre(), organizacion.getId())
                            .orElseGet(() -> {
                                Espacio nuevo = new Espacio(sede.nombre(), sede.ubicacion(), sede.capacidad());
                                nuevo.asignarOrganizacion(organizacion);
                                return espacios.save(nuevo);
                            }))
                    .toList();

            for (int mesOffset = -6; mesOffset <= 0; mesOffset++) {
                YearMonth mes = mesActual.plusMonths(mesOffset);
                for (int serieIndice = 0; serieIndice < empresa.series().size(); serieIndice++) {
                    Serie serie = empresa.series().get(serieIndice);
                    LocalDate fecha = mes.atDay(serieIndice == 0 ? 1 : 16);
                    OffsetDateTime inicio = fecha.atTime(serieIndice == 0 ? 9 : 16, 0)
                            .atOffset(ZONA_BOLIVIA);
                    OffsetDateTime fin = inicio.plusHours(3);
                    String nombreEvento = serie.nombre() + " · " + mes.getMonth().getDisplayName(
                            TextStyle.FULL, ESPANOL_BOLIVIA) + " " + mes.getYear();
                    EstadoEvento estado = estadoEvento(inicio, fin, mesOffset, serieIndice, ahora);
                    Categoria categoria = categoriasEmpresa.get(serie.categoriaIndice());
                    Espacio sede = sedes.get(Math.floorMod(mesOffset + serieIndice + empresaIndice, sedes.size()));
                    Usuario responsable = organizadores.get(Math.floorMod(mesOffset + serieIndice, organizadores.size()));
                    int aforo = Math.max(40, sede.getCapacidad() - 12);
                    OffsetDateTime creadoEvento = inicio.minusDays(30 + empresaIndice * 2L + serieIndice * 5L);
                    Evento evento = eventos.findFirstByNombreIgnoreCaseAndOrganizacionId(nombreEvento, organizacion.getId())
                            .orElseGet(() -> {
                                Evento nuevo = new Evento(nombreEvento, serie.descripcion(), inicio, fin,
                                        sede.getNombre(), estado, categoria, sede, aforo, creadoEvento);
                                nuevo.asignarOrganizacion(organizacion);
                                nuevo.asignarResponsables(Set.of(responsable));
                                return eventos.save(nuevo);
                            });

                    sembrarInscripciones(empresa, empresaIndice, mesOffset, serieIndice, evento, asistentes,
                            inscripciones, ahora);
                    if (serieIndice == 0) {
                    sembrarConvocatoria(empresa, empresaIndice, mesOffset, evento, inicio, equipo, asistentes,
                            organizacion, convocatorias, postulaciones, asignaciones, ahora);
                    }
                }
            }

            if (suscripciones != null) suscripciones.crearPlanBaseSiFalta(organizacion);
        }
    }

    private static void sembrarInscripciones(Empresa empresa, int empresaIndice, int mesOffset, int serieIndice,
                                               Evento evento, List<Usuario> asistentes,
                                               InscripcionRepository inscripciones, OffsetDateTime ahora) {
        int periodo = mesOffset + 6;
        Set<Long> participantesInscritos = new HashSet<>(inscripciones
                .findByEventoIdAndOrganizacionId(evento.getId(), evento.getOrganizacion().getId())
                .stream().map(inscripcion -> inscripcion.getParticipante().getId()).toList());
        for (int participanteIndice = 0; participanteIndice < asistentes.size(); participanteIndice++) {
            int patron = Math.floorMod(participanteIndice * 7 + periodo * 5 + serieIndice * 11 + empresaIndice * 3, 24);
            if (patron >= 15) continue;
            Usuario asistente = asistentes.get(participanteIndice);
            if (participantesInscritos.contains(asistente.getId())) continue;

            OffsetDateTime fechaInscripcion = evento.getFechaInicio().minusDays(7 + patron);
            if (fechaInscripcion.isAfter(ahora)) fechaInscripcion = ahora.minusDays(1 + patron % 9);
            EstadoInscripcion estado = evento.getEstado() == EstadoEvento.CANCELADO
                    ? EstadoInscripcion.CANCELADA
                    : patron == 0 ? EstadoInscripcion.EN_ESPERA
                    : patron == 13 ? EstadoInscripcion.CANCELADA
                    : EstadoInscripcion.CONFIRMADA;
            String qr = UUID.nameUUIDFromBytes((empresa.slug() + ":" + evento.getNombre() + ":"
                    + asistente.getCorreo()).getBytes(StandardCharsets.UTF_8)).toString();
            Inscripcion inscripcion = new Inscripcion(evento, asistente, qr, fechaInscripcion, estado);
            inscripcion.asignarOrganizacion(evento.getOrganizacion());
            inscripciones.save(inscripcion);
        }
    }

    private static void sembrarConvocatoria(Empresa empresa, int empresaIndice, int mesOffset, Evento evento,
                                             OffsetDateTime inicio, List<Usuario> equipo, List<Usuario> asistentes,
                                             Organizacion organizacion, ConvocatoriaRepository convocatorias,
                                             PostulacionStaffRepository postulaciones,
                                             AsignacionStaffRepository asignaciones, OffsetDateTime ahora) {
        int periodo = mesOffset + 6;
        TipoPersonal tipo = periodo % 2 == 0 ? TipoPersonal.STAFF : TipoPersonal.VOLUNTARIO;
        Convocatoria convocatoria = convocatorias.findFirstByEventoIdAndOrganizacionIdAndTipoPersonal(
                        evento.getId(), organizacion.getId(), tipo)
                .orElseGet(() -> {
                    Convocatoria nueva = new Convocatoria(evento, tipo,
                            tipo == TipoPersonal.STAFF
                                    ? "Apoyo de producción, registro y orientación de asistentes."
                                    : "Voluntariado para acompañar la actividad y recibir al público.",
                            tipo == TipoPersonal.STAFF ? 3 : 4,
                            inicio.minusDays(5).toLocalDate(), inicio.minusDays(18));
                    nueva.asignarOrganizacion(organizacion);
                    if (evento.getEstado() == EstadoEvento.FINALIZADO || evento.getEstado() == EstadoEvento.CANCELADO) {
                        if (evento.getEstado() == EstadoEvento.CANCELADO) nueva.cancelar();
                        else nueva.cerrar();
                    }
                    return convocatorias.save(nueva);
                });

        List<Usuario> candidatos = tipo == TipoPersonal.STAFF ? equipo : asistentes;
        for (int candidatoIndice = 0; candidatoIndice < 3; candidatoIndice++) {
            Usuario candidato = candidatos.get((periodo + empresaIndice + candidatoIndice * 2) % candidatos.size());
            boolean aceptar = evento.getEstado() != EstadoEvento.CANCELADO
                    && (candidatoIndice == 0 || (candidatoIndice == 1 && periodo % 3 == 0));
            boolean rechazar = evento.getEstado() == EstadoEvento.CANCELADO
                    || evento.getEstado() == EstadoEvento.FINALIZADO || candidatoIndice == 2;
            OffsetDateTime postulacionEn = inicio.minusDays(4 + candidatoIndice);
            PostulacionStaff postulacion = postulaciones
                    .findByConvocatoriaIdAndUsuarioIdAndOrganizacionId(
                            convocatoria.getId(), candidato.getId(), organizacion.getId())
                    .orElseGet(() -> {
                        PostulacionStaff nueva = new PostulacionStaff(convocatoria, candidato, organizacion,
                                motivacion(candidato, empresa, tipo),
                                aceptar ? EstadoPostulacionStaff.ACEPTADA
                                        : rechazar ? EstadoPostulacionStaff.RECHAZADA : EstadoPostulacionStaff.PENDIENTE,
                                postulacionEn);
                        return postulaciones.save(nueva);
                    });
            if (postulacion.getEstado() == EstadoPostulacionStaff.ACEPTADA
                    && !asignaciones.existsByPostulacionIdAndOrganizacionIdAndEstado(
                    postulacion.getId(), organizacion.getId(), EstadoAsignacionStaff.ASIGNADA)) {
                asignaciones.save(new AsignacionStaff(postulacion, organizacion,
                        tarea(tipo, candidatoIndice), inicio.minusHours(1), inicio.plusHours(2),
                        "Turno coordinado con " + empresa.nombre(), inicio.minusDays(1)));
            }
        }
    }

    private static EstadoEvento estadoEvento(OffsetDateTime inicio, OffsetDateTime fin,
                                               int mesOffset, int serieIndice, OffsetDateTime ahora) {
        if (mesOffset == -3 && serieIndice == 1) return EstadoEvento.CANCELADO;
        if (fin.isBefore(ahora)) return EstadoEvento.FINALIZADO;
        if (inicio.isBefore(ahora)) return EstadoEvento.EN_CURSO;
        return (Math.abs(mesOffset) + serieIndice) % 2 == 0
                ? EstadoEvento.PUBLICADO : EstadoEvento.PLANIFICADO;
    }

    private static Usuario asegurarUsuario(UsuarioRepository usuarios, String claveDemoHash,
                                           Empresa empresa, Persona persona, Rol rol,
                                           OffsetDateTime inicioHistorial) {
        String correo = correo(persona, empresa.dominioCorreo());
        return usuarios.findByCorreoConRoles(correo)
                .map(existente -> {
                    existente.actualizarEstado(EstadoUsuario.ACTIVO);
                    existente.getRoles().add(rol);
                    return usuarios.save(existente);
                })
                .orElseGet(() -> {
                    Usuario nuevo = new Usuario(persona.nombres(), persona.apellidos(), correo,
                            claveDemoHash, EstadoUsuario.ACTIVO,
                            inicioHistorial.minusDays(90 + Math.floorMod(correo.hashCode(), 30)));
                    nuevo.asignarRoles(Set.of(rol));
                    return usuarios.save(nuevo);
                });
    }

    private static void asegurarMembresia(MembresiaOrganizacionRepository membresias,
                                          Usuario usuario, Organizacion organizacion, Set<Rol> roles) {
        MembresiaOrganizacion membresia = membresias.findByUsuarioIdAndOrganizacionIdConRoles(
                        usuario.getId(), organizacion.getId())
                .orElseGet(() -> new MembresiaOrganizacion(usuario, organizacion));
        membresia.activar();
        membresia.getRoles().addAll(roles);
        membresias.save(membresia);
    }

    private static Rol rol(RolRepository roles, String nombre) {
        return roles.findByNombre(nombre).orElseThrow(() -> new IllegalStateException("No existe el rol " + nombre));
    }

    private static String correo(Persona persona, String dominio) {
        String usuario = (persona.nombres() + "." + persona.apellidos()).toLowerCase(Locale.ROOT);
        usuario = Normalizer.normalize(usuario, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9.]+", "").replaceAll("\\.+", ".");
        return usuario + "@" + dominio;
    }

    private static String motivacion(Usuario usuario, Empresa empresa, TipoPersonal tipo) {
        return tipo == TipoPersonal.STAFF
                ? "Tengo experiencia en coordinación y atención de asistentes; me interesa colaborar con " + empresa.nombre() + "."
                : "Quiero apoyar las actividades comunitarias de " + empresa.nombre() + " y participar en equipo.";
    }

    private static String tarea(TipoPersonal tipo, int indice) {
        if (tipo == TipoPersonal.VOLUNTARIO) return indice == 0 ? "Recepción de participantes" : "Orientación del público";
        return switch (indice) {
            case 0 -> "Coordinación de sala";
            case 1 -> "Registro y acreditación";
            default -> "Apoyo de producción";
        };
    }

    record Empresa(String nombre, String slug, String dominioCorreo, List<String> categorias,
                   List<Sede> sedes, List<Persona> organizadores, List<Persona> staff, List<Serie> series) {
        Empresa {
            categorias = List.copyOf(categorias);
            sedes = List.copyOf(sedes);
            organizadores = List.copyOf(organizadores);
            staff = List.copyOf(staff);
            series = List.copyOf(series);
        }
    }

    record Sede(String nombre, String ubicacion, int capacidad) { }
    record Persona(String nombres, String apellidos) { }
    record Serie(String nombre, int categoriaIndice, String descripcion) { }
}
