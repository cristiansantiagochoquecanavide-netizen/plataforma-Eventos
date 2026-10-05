package com.plataformaeventos.eventos.servicio;

import com.plataformaeventos.categorias.dto.CategoriaRespuesta;
import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.espacios.modelo.Espacio;
import com.plataformaeventos.espacios.repositorio.EspacioRepository;
import com.plataformaeventos.eventos.dto.AsignarResponsablesSolicitud;
import com.plataformaeventos.eventos.dto.EspacioEventoRespuesta;
import com.plataformaeventos.eventos.dto.EventoRespuesta;
import com.plataformaeventos.eventos.dto.GuardarEventoSolicitud;
import com.plataformaeventos.eventos.dto.ResponsableEventoRespuesta;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventoService {

    private static final Set<String> ROLES_RESPONSABLES = Set.of("ORGANIZADOR", "STAFF");

    private final EventoRepository eventoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspacioRepository espacioRepository;

    public EventoService(
            EventoRepository eventoRepository,
            CategoriaRepository categoriaRepository,
            UsuarioRepository usuarioRepository,
            EspacioRepository espacioRepository
    ) {
        this.eventoRepository = eventoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.espacioRepository = espacioRepository;
    }

    @Transactional(readOnly = true)
    public List<EventoRespuesta> listarEventos(String termino, String categoria) {
        return listarEventos(termino, categoria, null);
    }
    @Transactional(readOnly = true)
    public List<EventoRespuesta> listarEventos(String termino, String categoria, Organizacion organizacion) {
        String texto = termino == null ? "" : termino.trim().toLowerCase();
        String filtroCategoria = categoria == null ? "" : categoria.trim().toLowerCase();
        return (organizacion == null ? eventoRepository.findByEstadoNotOrderByFechaInicioAsc(EstadoEvento.CANCELADO) : eventoRepository.findByOrganizacionIdAndEstadoNotOrderByFechaInicioAsc(organizacion.getId(), EstadoEvento.CANCELADO))
                .stream()
                // Se filtra en el servicio para que web y movil reciban exactamente el mismo resultado.
                .filter(evento -> texto.isEmpty() || evento.getNombre().toLowerCase().contains(texto))
                .filter(evento -> filtroCategoria.isEmpty() || nombreCategoria(evento).toLowerCase().equals(filtroCategoria))
                .map(evento -> convertirEvento(evento, organizacion))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventoRespuesta obtenerEvento(Long idEvento) {
        return obtenerEvento(idEvento, null);
    }
    @Transactional(readOnly = true)
    public EventoRespuesta obtenerEvento(Long idEvento, Organizacion organizacion) {
        return convertirEvento(buscarEvento(idEvento, organizacion), organizacion);
    }

    @Transactional(readOnly = true)
    public List<ResponsableEventoRespuesta> listarResponsablesDisponibles() {
        return listarResponsablesDisponibles(null);
    }
    @Transactional(readOnly = true)
    public List<ResponsableEventoRespuesta> listarResponsablesDisponibles(Organizacion organizacion) {
        return (organizacion == null ? usuarioRepository.findResponsablesDisponibles(ROLES_RESPONSABLES) : usuarioRepository.findResponsablesDisponiblesPorOrganizacion(ROLES_RESPONSABLES, organizacion.getId())).stream()
                .map(usuario -> convertirResponsable(usuario, organizacion))
                .toList();
    }

    @Transactional
    public EventoRespuesta crearEvento(GuardarEventoSolicitud solicitud) {
        return crearEvento(solicitud, null);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "EVENTOS", entidad = "EVENTO", accion = "CREAR")
    public EventoRespuesta crearEvento(GuardarEventoSolicitud solicitud, Organizacion organizacion) {
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Categoria categoria = buscarCategoria(solicitud.idCategoria(), organizacion);
        Espacio espacio = validarProgramacion(solicitud, null, organizacion);
        Evento evento = new Evento(
                solicitud.nombre().trim(),
                normalizarTexto(solicitud.descripcion()),
                solicitud.fechaInicio(),
                solicitud.fechaFin(),
                normalizarTexto(solicitud.lugarPrincipal()),
                solicitud.estado() == null ? EstadoEvento.PLANIFICADO : solicitud.estado(),
                categoria
        );
        evento.asignarOrganizacion(organizacion);
        evento.configurarProgramacion(espacio, solicitud.aforoMaximo());
        if (solicitud.responsables() != null) {
             evento.asignarResponsables(obtenerResponsablesValidos(solicitud.responsables(), organizacion));
        }
        return convertirEvento(eventoRepository.save(evento), organizacion);
    }

    @Transactional
    public EventoRespuesta actualizarEvento(Long idEvento, GuardarEventoSolicitud solicitud) {
        return actualizarEvento(idEvento, solicitud, null);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "EVENTOS", entidad = "EVENTO", accion = "ACTUALIZAR")
    public EventoRespuesta actualizarEvento(Long idEvento, GuardarEventoSolicitud solicitud, Organizacion organizacion) {
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Evento evento = buscarEvento(idEvento, organizacion);
        Categoria categoria = buscarCategoria(solicitud.idCategoria(), organizacion);
        Espacio espacio = validarProgramacion(solicitud, idEvento, organizacion);
        evento.actualizarDatos(
                solicitud.nombre().trim(),
                normalizarTexto(solicitud.descripcion()),
                solicitud.fechaInicio(),
                solicitud.fechaFin(),
                normalizarTexto(solicitud.lugarPrincipal()),
                solicitud.estado() == null ? evento.getEstado() : solicitud.estado(),
                categoria
        );
        evento.configurarProgramacion(espacio, solicitud.aforoMaximo());
        return convertirEvento(evento, organizacion);
    }

    @Transactional
    public EventoRespuesta asignarResponsables(Long idEvento, AsignarResponsablesSolicitud solicitud) {
        return asignarResponsables(idEvento, solicitud, null);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "EVENTOS", entidad = "EVENTO", accion = "RESPONSABLES")
    public EventoRespuesta asignarResponsables(Long idEvento, AsignarResponsablesSolicitud solicitud, Organizacion organizacion) {
        Evento evento = buscarEvento(idEvento, organizacion);
        evento.asignarResponsables(obtenerResponsablesValidos(solicitud.responsables(), organizacion));
        return convertirEvento(evento, organizacion);
    }

    private Evento buscarEvento(Long idEvento) { return buscarEvento(idEvento, null); }
    private Evento buscarEvento(Long idEvento, Organizacion organizacion) {
        return (organizacion == null ? eventoRepository.findById(idEvento) : eventoRepository.findByIdAndOrganizacionId(idEvento, organizacion.getId()))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Evento no encontrado"));
    }

    private Categoria buscarCategoria(Long idCategoria) {
        if (idCategoria == null) {
            throw new IllegalArgumentException("La categoria del evento es obligatoria");
        }
        return categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Categoria no encontrada"));
    }
    private Categoria buscarCategoria(Long idCategoria, Organizacion organizacion) {
        if (organizacion == null) return buscarCategoria(idCategoria);
        return categoriaRepository.findByIdAndOrganizacionId(idCategoria, organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Categoria no encontrada"));
    }

    private Set<Usuario> obtenerResponsablesValidos(Set<Long> idsResponsables, Organizacion organizacion) {
        if (idsResponsables == null || idsResponsables.isEmpty()) {
            return Set.of();
        }
        Set<Usuario> responsables = idsResponsables.stream()
                .map(id -> buscarResponsableValido(id, organizacion))
                .collect(Collectors.toSet());
        if (responsables.size() != idsResponsables.size()) {
            throw new IllegalArgumentException("No se puede asignar el mismo responsable mas de una vez");
        }
        if (organizacion == null) {
            boolean hayResponsableInvalido = responsables.stream().anyMatch(usuario ->
                    usuario.getEstado() != EstadoUsuario.ACTIVO || usuario.getRoles().stream()
                            .map(Rol::getNombre)
                            .noneMatch(ROLES_RESPONSABLES::contains)
            );
            if (hayResponsableInvalido) {
                throw new IllegalArgumentException("Los responsables deben estar activos y tener rol ORGANIZADOR o STAFF");
            }
        }
        return responsables;
    }

    private Usuario buscarResponsableValido(Long id, Organizacion organizacion) {
        if (organizacion != null) {
            return usuarioRepository.findResponsableActivoPorOrganizacionYRol(id, organizacion.getId(), ROLES_RESPONSABLES)
                    .orElseThrow(() -> new ExcepcionNoEncontrado("Responsable no encontrado"));
        }
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Responsable no encontrado"));
    }

    private void validarFechas(OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {
        if (fechaFin != null && !fechaFin.isAfter(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
    }

    private Espacio validarProgramacion(GuardarEventoSolicitud solicitud, Long idEvento, Organizacion organizacion) {
        if (solicitud.idEspacio() == null) {
            throw new IllegalArgumentException("El espacio es obligatorio");
        }
        if (solicitud.aforoMaximo() == null) {
            throw new IllegalArgumentException("El aforo maximo es obligatorio");
        }
        Espacio espacio = (organizacion == null ? espacioRepository.findById(solicitud.idEspacio()) : espacioRepository.findByIdAndOrganizacionId(solicitud.idEspacio(), organizacion.getId()))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Espacio no encontrado"));
        if (solicitud.aforoMaximo() > espacio.getCapacidad()) {
            throw new IllegalArgumentException("El aforo del evento no puede superar la capacidad del espacio");
        }
        if (existeConflictoProgramacion(espacio.getId(), solicitud.fechaInicio(), solicitud.fechaFin(), idEvento, organizacion)) {
            throw new IllegalArgumentException("El espacio ya esta ocupado en el horario seleccionado");
        }
        return espacio;
    }

    private boolean existeConflictoProgramacion(
            Long idEspacio,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin,
            Long idEvento,
            Organizacion organizacion
    ) {
        if (organizacion != null) {
            Long org = organizacion.getId();
            if (idEvento == null) return fechaFin == null
                    ? eventoRepository.existeConflictoProgramacionCrearSinFinTenant(idEspacio, fechaInicio, org)
                    : eventoRepository.existeConflictoProgramacionCrearConFinTenant(idEspacio, fechaInicio, fechaFin, org);
            return fechaFin == null
                    ? eventoRepository.existeConflictoProgramacionEditarSinFinTenant(idEspacio, fechaInicio, idEvento, org)
                    : eventoRepository.existeConflictoProgramacionEditarConFinTenant(idEspacio, fechaInicio, fechaFin, idEvento, org);
        }
        if (idEvento == null) {
            return fechaFin == null
                    ? eventoRepository.existeConflictoProgramacionCrearSinFin(idEspacio, fechaInicio)
                    : eventoRepository.existeConflictoProgramacionCrearConFin(idEspacio, fechaInicio, fechaFin);
        }
        return fechaFin == null
                ? eventoRepository.existeConflictoProgramacionEditarSinFin(idEspacio, fechaInicio, idEvento)
                : eventoRepository.existeConflictoProgramacionEditarConFin(
                        idEspacio, fechaInicio, fechaFin, idEvento
                );
    }

    private String normalizarTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private EventoRespuesta convertirEvento(Evento evento) { return convertirEvento(evento, null); }
    private EventoRespuesta convertirEvento(Evento evento, Organizacion organizacion) {
        CategoriaRespuesta categoria = convertirCategoria(evento.getCategoria());
        return new EventoRespuesta(
                evento.getId(),
                evento.getNombre(),
                evento.getDescripcion(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                evento.getLugarPrincipal(),
                categoria,
                categoria == null ? null : categoria.nombre(),
                convertirEspacio(evento.getEspacio()),
                evento.getAforoMaximo(),
                evento.getEstado().name(),
                evento.getResponsables().stream()
                        .sorted(Comparator.comparing(Usuario::getApellidos).thenComparing(Usuario::getNombres))
                        .map(usuario -> convertirResponsable(usuario, organizacion))
                        .toList()
        );
    }

    private String nombreCategoria(Evento evento) {
        Categoria categoria = evento.getCategoria();
        return categoria == null || categoria.getNombre() == null ? "" : categoria.getNombre();
    }

    private CategoriaRespuesta convertirCategoria(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        return new CategoriaRespuesta(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getFechaCreacion(),
                categoria.getFechaActualizacion()
        );
    }

    private EspacioEventoRespuesta convertirEspacio(Espacio espacio) {
        if (espacio == null) {
            return null;
        }
        return new EspacioEventoRespuesta(
                espacio.getId(),
                espacio.getNombre(),
                espacio.getUbicacion(),
                espacio.getCapacidad()
        );
    }

    private ResponsableEventoRespuesta convertirResponsable(Usuario usuario) { return convertirResponsable(usuario, null); }
    private ResponsableEventoRespuesta convertirResponsable(Usuario usuario, Organizacion organizacion) {
        return new ResponsableEventoRespuesta(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                nombresRoles(usuario, organizacion)
        );
    }

    private List<String> nombresRoles(Usuario usuario, Organizacion organizacion) {
        if (organizacion != null) {
            return usuarioRepository.findNombresRolesActivosPorUsuarioYOrganizacion(usuario.getId(), organizacion.getId());
        }
        return usuario.getRoles().stream()
                .map(Rol::getNombre)
                .sorted()
                .toList();
    }
}
