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
        String texto = termino == null ? "" : termino.trim().toLowerCase();
        String filtroCategoria = categoria == null ? "" : categoria.trim().toLowerCase();
        return eventoRepository.findByEstadoNotOrderByFechaInicioAsc(EstadoEvento.CANCELADO)
                .stream()
                // Se filtra en el servicio para que web y movil reciban exactamente el mismo resultado.
                .filter(evento -> texto.isEmpty() || evento.getNombre().toLowerCase().contains(texto))
                .filter(evento -> filtroCategoria.isEmpty() || nombreCategoria(evento).toLowerCase().equals(filtroCategoria))
                .map(this::convertirEvento)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventoRespuesta obtenerEvento(Long idEvento) {
        return convertirEvento(buscarEvento(idEvento));
    }

    @Transactional(readOnly = true)
    public List<ResponsableEventoRespuesta> listarResponsablesDisponibles() {
        return usuarioRepository.findResponsablesDisponibles(ROLES_RESPONSABLES).stream()
                .map(this::convertirResponsable)
                .toList();
    }

    @Transactional
    public EventoRespuesta crearEvento(GuardarEventoSolicitud solicitud) {
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Categoria categoria = buscarCategoria(solicitud.idCategoria());
        Espacio espacio = validarProgramacion(solicitud, null);
        Evento evento = new Evento(
                solicitud.nombre().trim(),
                normalizarTexto(solicitud.descripcion()),
                solicitud.fechaInicio(),
                solicitud.fechaFin(),
                normalizarTexto(solicitud.lugarPrincipal()),
                solicitud.estado() == null ? EstadoEvento.PLANIFICADO : solicitud.estado(),
                categoria
        );
        evento.configurarProgramacion(espacio, solicitud.aforoMaximo());
        if (solicitud.responsables() != null) {
            evento.asignarResponsables(obtenerResponsablesValidos(solicitud.responsables()));
        }
        return convertirEvento(eventoRepository.save(evento));
    }

    @Transactional
    public EventoRespuesta actualizarEvento(Long idEvento, GuardarEventoSolicitud solicitud) {
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Evento evento = buscarEvento(idEvento);
        Categoria categoria = buscarCategoria(solicitud.idCategoria());
        Espacio espacio = validarProgramacion(solicitud, idEvento);
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
        return convertirEvento(evento);
    }

    @Transactional
    public EventoRespuesta asignarResponsables(Long idEvento, AsignarResponsablesSolicitud solicitud) {
        Evento evento = buscarEvento(idEvento);
        evento.asignarResponsables(obtenerResponsablesValidos(solicitud.responsables()));
        return convertirEvento(evento);
    }

    private Evento buscarEvento(Long idEvento) {
        return eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Evento no encontrado"));
    }

    private Categoria buscarCategoria(Long idCategoria) {
        if (idCategoria == null) {
            throw new IllegalArgumentException("La categoria del evento es obligatoria");
        }
        return categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Categoria no encontrada"));
    }

    private Set<Usuario> obtenerResponsablesValidos(Set<Long> idsResponsables) {
        if (idsResponsables == null || idsResponsables.isEmpty()) {
            return Set.of();
        }
        Set<Usuario> responsables = idsResponsables.stream()
                .map(id -> usuarioRepository.findById(id)
                        .orElseThrow(() -> new ExcepcionNoEncontrado("Responsable no encontrado")))
                .collect(Collectors.toSet());
        if (responsables.size() != idsResponsables.size()) {
            throw new IllegalArgumentException("No se puede asignar el mismo responsable mas de una vez");
        }
        boolean hayResponsableInvalido = responsables.stream().anyMatch(usuario ->
                usuario.getEstado() != EstadoUsuario.ACTIVO || usuario.getRoles().stream()
                        .map(Rol::getNombre)
                        .noneMatch(ROLES_RESPONSABLES::contains)
        );
        if (hayResponsableInvalido) {
            throw new IllegalArgumentException("Los responsables deben estar activos y tener rol ORGANIZADOR o STAFF");
        }
        return responsables;
    }

    private void validarFechas(OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {
        if (fechaFin != null && !fechaFin.isAfter(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
    }

    private Espacio validarProgramacion(GuardarEventoSolicitud solicitud, Long idEvento) {
        if (solicitud.idEspacio() == null) {
            throw new IllegalArgumentException("El espacio es obligatorio");
        }
        if (solicitud.aforoMaximo() == null) {
            throw new IllegalArgumentException("El aforo maximo es obligatorio");
        }
        Espacio espacio = espacioRepository.findById(solicitud.idEspacio())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Espacio no encontrado"));
        if (solicitud.aforoMaximo() > espacio.getCapacidad()) {
            throw new IllegalArgumentException("El aforo del evento no puede superar la capacidad del espacio");
        }
        if (existeConflictoProgramacion(espacio.getId(), solicitud.fechaInicio(), solicitud.fechaFin(), idEvento)) {
            throw new IllegalArgumentException("El espacio ya esta ocupado en el horario seleccionado");
        }
        return espacio;
    }

    private boolean existeConflictoProgramacion(
            Long idEspacio,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin,
            Long idEvento
    ) {
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

    private EventoRespuesta convertirEvento(Evento evento) {
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
                        .map(this::convertirResponsable)
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

    private ResponsableEventoRespuesta convertirResponsable(Usuario usuario) {
        return new ResponsableEventoRespuesta(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getRoles().stream()
                        .map(Rol::getNombre)
                        .sorted()
                        .toList()
        );
    }
}
