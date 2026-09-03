package com.plataformaeventos.eventos;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.espacios.Espacio;
import com.plataformaeventos.espacios.EspacioRepository;
import com.plataformaeventos.roles.Rol;
import com.plataformaeventos.usuarios.EstadoUsuario;
import com.plataformaeventos.usuarios.Usuario;
import com.plataformaeventos.usuarios.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;
    private final EspacioRepository espacioRepository;

    public EventoService(
            EventoRepository eventoRepository,
            UsuarioRepository usuarioRepository,
            EspacioRepository espacioRepository
    ) {
        this.eventoRepository = eventoRepository;
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
                .filter(evento -> filtroCategoria.isEmpty() || evento.getCategoria().toLowerCase().equals(filtroCategoria))
                .map(this::convertirEvento)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventoRespuesta obtenerEvento(Long idEvento) {
        return convertirEvento(buscarEvento(idEvento));
    }

    @Transactional
    public EventoRespuesta crearEvento(GuardarEventoSolicitud solicitud) {
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Espacio espacio = validarProgramacion(solicitud, null);
        Evento evento = new Evento(
                solicitud.nombre().trim(),
                normalizarTexto(solicitud.descripcion()),
                solicitud.categoria().trim(),
                solicitud.fechaInicio(),
                solicitud.fechaFin(),
                normalizarTexto(solicitud.lugarPrincipal()),
                solicitud.estado() == null ? EstadoEvento.PLANIFICADO : solicitud.estado()
        );
        evento.configurarProgramacion(espacio, solicitud.aforoMaximo());
        evento.asignarResponsables(obtenerResponsablesValidos(solicitud.responsables()));
        return convertirEvento(eventoRepository.save(evento));
    }

    @Transactional
    public EventoRespuesta actualizarEvento(Long idEvento, GuardarEventoSolicitud solicitud) {
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Evento evento = buscarEvento(idEvento);
        Espacio espacio = validarProgramacion(solicitud, idEvento);
        evento.actualizarDatos(
                solicitud.nombre().trim(),
                normalizarTexto(solicitud.descripcion()),
                solicitud.categoria().trim(),
                solicitud.fechaInicio(),
                solicitud.fechaFin(),
                normalizarTexto(solicitud.lugarPrincipal()),
                solicitud.estado() == null ? evento.getEstado() : solicitud.estado()
        );
        evento.configurarProgramacion(espacio, solicitud.aforoMaximo());
        evento.asignarResponsables(obtenerResponsablesValidos(solicitud.responsables()));
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

    private Set<Usuario> obtenerResponsablesValidos(Set<Long> idsResponsables) {
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
        if (fechaFin == null || !fechaFin.isAfter(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
    }

    private Espacio validarProgramacion(GuardarEventoSolicitud solicitud, Long idEvento) {
        Espacio espacio = espacioRepository.findById(solicitud.idEspacio())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Espacio no encontrado"));
        if (solicitud.aforoMaximo() > espacio.getCapacidad()) {
            throw new IllegalArgumentException("El aforo del evento no puede superar la capacidad del espacio");
        }
        if (eventoRepository.existeConflictoProgramacion(
                espacio.getId(), solicitud.fechaInicio(), solicitud.fechaFin(), idEvento)) {
            throw new IllegalArgumentException("El espacio ya esta ocupado en el horario seleccionado");
        }
        return espacio;
    }

    private String normalizarTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private EventoRespuesta convertirEvento(Evento evento) {
        return new EventoRespuesta(
                evento.getId(),
                evento.getNombre(),
                evento.getDescripcion(),
                evento.getCategoria(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                evento.getLugarPrincipal(),
                evento.getEspacio() == null ? null : new EspacioEventoRespuesta(
                        evento.getEspacio().getId(),
                        evento.getEspacio().getNombre(),
                        evento.getEspacio().getUbicacion(),
                        evento.getEspacio().getCapacidad()
                ),
                evento.getAforoMaximo(),
                evento.getEstado().name(),
                evento.getResponsables().stream()
                        .sorted(Comparator.comparing(Usuario::getApellidos).thenComparing(Usuario::getNombres))
                        .map(this::convertirResponsable)
                        .toList()
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
