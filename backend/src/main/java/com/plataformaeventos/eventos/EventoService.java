package com.plataformaeventos.eventos;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.roles.Rol;
import com.plataformaeventos.usuarios.EstadoUsuario;
import com.plataformaeventos.usuarios.Usuario;
import com.plataformaeventos.usuarios.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventoService {

    private static final Set<String> ROLES_RESPONSABLES = Set.of("ORGANIZADOR", "STAFF");

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    public EventoService(EventoRepository eventoRepository, UsuarioRepository usuarioRepository) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public java.util.List<EventoRespuesta> listarEventos() {
        return eventoRepository.findAllByOrderByFechaInicioAsc().stream()
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
        Evento evento = new Evento(
                solicitud.nombre().trim(),
                normalizarDescripcion(solicitud.descripcion()),
                solicitud.fechaInicio(),
                solicitud.fechaFin()
        );
        evento.asignarResponsables(obtenerResponsablesValidos(solicitud.responsables()));
        return convertirEvento(eventoRepository.save(evento));
    }

    @Transactional
    public EventoRespuesta actualizarEvento(Long idEvento, GuardarEventoSolicitud solicitud) {
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Evento evento = buscarEvento(idEvento);
        evento.actualizarDatos(
                solicitud.nombre().trim(),
                normalizarDescripcion(solicitud.descripcion()),
                solicitud.fechaInicio(),
                solicitud.fechaFin(),
                solicitud.estado() == null ? evento.getEstado() : solicitud.estado()
        );
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
        if (fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        return descripcion.trim();
    }

    private EventoRespuesta convertirEvento(Evento evento) {
        return new EventoRespuesta(
                evento.getId(),
                evento.getNombre(),
                evento.getDescripcion(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
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
