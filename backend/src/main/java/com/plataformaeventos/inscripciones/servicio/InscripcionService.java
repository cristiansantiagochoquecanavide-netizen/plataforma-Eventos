package com.plataformaeventos.inscripciones.servicio;

import com.plataformaeventos.asistencias.modelo.Asistencia;
import com.plataformaeventos.asistencias.repositorio.AsistenciaRepository;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.inscripciones.dto.DisponibilidadEventoRespuesta;
import com.plataformaeventos.inscripciones.dto.InscripcionRespuesta;
import com.plataformaeventos.inscripciones.dto.HistorialParticipacionRespuesta;
import com.plataformaeventos.inscripciones.dto.ParticipanteInscritoRespuesta;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.notificaciones.servicio.NotificacionEventoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Locale;
import java.util.UUID;

@Service
public class InscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final NotificacionEventoService notificaciones;

    public InscripcionService(
            InscripcionRepository inscripcionRepository,
            EventoRepository eventoRepository,
            UsuarioRepository usuarioRepository,
            AsistenciaRepository asistenciaRepository
    ) {
        this(inscripcionRepository, eventoRepository, usuarioRepository, asistenciaRepository, null);
    }

    @Autowired
    public InscripcionService(
            InscripcionRepository inscripcionRepository,
            EventoRepository eventoRepository,
            UsuarioRepository usuarioRepository,
            AsistenciaRepository asistenciaRepository,
            NotificacionEventoService notificaciones
    ) {
        this.inscripcionRepository = inscripcionRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.notificaciones = notificaciones;
    }

    @Transactional
    public InscripcionRespuesta inscribir(Long idEvento, String correoUsuario) {
        return inscribir(idEvento, correoUsuario, null);
    }

    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "INSCRIPCIONES", entidad = "INSCRIPCION", accion = "CREAR")
    public InscripcionRespuesta inscribir(Long idEvento, String correoUsuario, Organizacion organizacion) {
        Usuario participante = buscarUsuario(correoUsuario);
        Evento evento = (organizacion == null ? eventoRepository.findById(idEvento) : inscripcionRepository.bloquearEvento(idEvento, organizacion.getId()))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Evento no encontrado con identificador " + idEvento));
        if (organizacion != null && (evento.getOrganizacion() == null || !organizacion.getId().equals(evento.getOrganizacion().getId()))) throw new ExcepcionNoEncontrado("Evento no encontrado con identificador " + idEvento);

        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new IllegalStateException("Solo es posible inscribirse a eventos en estado PUBLICADO");
        }

        if (evento.getFechaFin() != null && evento.getFechaFin().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("No es posible inscribirse a un evento que ya ha finalizado");
        }

        var inscripcionExistente = organizacion == null ? inscripcionRepository.findByEventoIdAndParticipanteId(idEvento, participante.getId()) : inscripcionRepository.findByEventoIdAndParticipanteIdAndOrganizacionId(idEvento, participante.getId(), organizacion.getId());
        if (inscripcionExistente.isPresent()) {
            Inscripcion existente = inscripcionExistente.get();
            if (existente.estaConfirmada()) {
                throw new IllegalStateException("Ya cuentas con una inscripción confirmada para este evento");
            }
            validarCuposDisponibles(evento, organizacion);
            String nuevoCodigo = generarCodigoQr(idEvento, participante.getId());
            existente.reactivar(nuevoCodigo);
            if (organizacion != null) existente.asignarOrganizacion(organizacion);
            Inscripcion guardada = inscripcionRepository.save(existente);
            if (notificaciones != null && organizacion != null) notificaciones.programarConfirmacion(guardada, organizacion);
            return InscripcionRespuesta.desdeEntidad(guardada);
        }

        validarCuposDisponibles(evento, organizacion);
        String codigoQr = generarCodigoQr(idEvento, participante.getId());
        Inscripcion nuevaInscripcion = new Inscripcion(evento, participante, codigoQr);
        if (organizacion != null) nuevaInscripcion.asignarOrganizacion(organizacion);
        Inscripcion guardada = inscripcionRepository.save(nuevaInscripcion);
        if (notificaciones != null && organizacion != null) notificaciones.programarConfirmacion(guardada, organizacion);
        return InscripcionRespuesta.desdeEntidad(guardada);
    }

    @Transactional(readOnly = true)
    public List<InscripcionRespuesta> listarMisInscripciones(String correoUsuario) {
        return listarMisInscripciones(correoUsuario, null);
    }
    @Transactional(readOnly = true)
    public List<InscripcionRespuesta> listarMisInscripciones(String correoUsuario, Organizacion organizacion) {
        Usuario participante = buscarUsuario(correoUsuario);
        return (organizacion == null ? inscripcionRepository.findByParticipanteIdConEvento(participante.getId()) : inscripcionRepository.findByParticipanteIdAndOrganizacionIdConEvento(participante.getId(), organizacion.getId()))
                .stream()
                .map(InscripcionRespuesta::desdeEntidad)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistorialParticipacionRespuesta> listarMiHistorial(String correoUsuario, Organizacion organizacion) {
        Usuario participante = buscarUsuario(correoUsuario);
        List<Inscripcion> inscripciones = organizacion == null
                ? inscripcionRepository.findByParticipanteIdConEvento(participante.getId())
                : inscripcionRepository.findByParticipanteIdAndOrganizacionIdConEvento(participante.getId(), organizacion.getId());

        if (inscripciones.isEmpty()) return List.of();

        Map<Long, Asistencia> asistencias = asistenciaRepository.findByInscripcionIdIn(
                        inscripciones.stream().map(Inscripcion::getId).toList())
                .stream()
                .collect(Collectors.toMap(a -> a.getInscripcion().getId(), Function.identity()));

        return inscripciones.stream()
                .map(inscripcion -> HistorialParticipacionRespuesta.desde(inscripcion, asistencias.get(inscripcion.getId())))
                .toList();
    }

    @Transactional
    public InscripcionRespuesta cancelarInscripcion(Long idInscripcion, String correoUsuario) {
        return cancelarInscripcion(idInscripcion, correoUsuario, null);
    }

    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "INSCRIPCIONES", entidad = "INSCRIPCION", accion = "CANCELAR")
    public InscripcionRespuesta cancelarInscripcion(Long idInscripcion, String correoUsuario, Organizacion organizacion) {
        Usuario participante = buscarUsuario(correoUsuario);
        Inscripcion inscripcion = (organizacion == null ? inscripcionRepository.findByIdAndParticipanteId(idInscripcion, participante.getId()) : inscripcionRepository.findByIdAndParticipanteIdAndOrganizacionId(idInscripcion, participante.getId(), organizacion.getId()))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Inscripción no encontrada o no pertenece al usuario autenticado"));

        if (!inscripcion.estaConfirmada()) {
            throw new IllegalStateException("La inscripción ya se encuentra cancelada");
        }

        Evento evento = inscripcion.getEvento();
        if (evento.getEstado() == EstadoEvento.FINALIZADO ||
                (evento.getFechaFin() != null && evento.getFechaFin().isBefore(OffsetDateTime.now()))) {
            throw new IllegalStateException("No es posible cancelar la inscripción a un evento que ya ha finalizado");
        }

        inscripcion.cancelar();
        if (notificaciones != null && organizacion != null) notificaciones.cancelarPendientesInscripcion(inscripcion, organizacion);
        return InscripcionRespuesta.desdeEntidad(inscripcionRepository.save(inscripcion));
    }

    @Transactional(readOnly = true)
    public DisponibilidadEventoRespuesta consultarDisponibilidad(Long idEvento, String correoUsuario) {
        return consultarDisponibilidad(idEvento, correoUsuario, null);
    }

    @Transactional(readOnly = true)
    public DisponibilidadEventoRespuesta consultarDisponibilidad(Long idEvento, String correoUsuario, Organizacion organizacion) {
        Evento evento = (organizacion == null ? eventoRepository.findById(idEvento) : eventoRepository.findByIdAndOrganizacionId(idEvento, organizacion.getId()))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Evento no encontrado con identificador " + idEvento));
        if (organizacion != null && (evento.getOrganizacion() == null || !organizacion.getId().equals(evento.getOrganizacion().getId()))) throw new ExcepcionNoEncontrado("Evento no encontrado con identificador " + idEvento);

        long cuposOcupados = organizacion == null ? inscripcionRepository.countByEventoIdAndEstado(idEvento, EstadoInscripcion.CONFIRMADA) : inscripcionRepository.countByEventoIdAndOrganizacionIdAndEstado(idEvento, organizacion.getId(), EstadoInscripcion.CONFIRMADA);
        Integer aforo = evento.getAforoMaximo();
        Integer cuposDisponibles = aforo != null ? Math.max(0, aforo - (int) cuposOcupados) : null;
        boolean tieneCupo = aforo == null || cuposDisponibles > 0;

        boolean estaInscrito = false;
        Long idInscripcionActiva = null;

        if (correoUsuario != null && !correoUsuario.isBlank()) {
            var usuarioOpt = usuarioRepository.findByCorreo(correoUsuario.trim().toLowerCase(Locale.ROOT));
            if (usuarioOpt.isPresent()) {
                var insOpt = organizacion == null ? inscripcionRepository.findByEventoIdAndParticipanteId(idEvento, usuarioOpt.get().getId()) : inscripcionRepository.findByEventoIdAndParticipanteIdAndOrganizacionId(idEvento, usuarioOpt.get().getId(), organizacion.getId());
                if (insOpt.isPresent() && insOpt.get().estaConfirmada()) {
                    estaInscrito = true;
                    idInscripcionActiva = insOpt.get().getId();
                }
            }
        }

        return new DisponibilidadEventoRespuesta(
                idEvento,
                aforo,
                cuposOcupados,
                cuposDisponibles,
                tieneCupo,
                estaInscrito,
                idInscripcionActiva
        );
    }

    private void validarCuposDisponibles(Evento evento, Organizacion organizacion) {
        if (evento.getAforoMaximo() != null) {
            long cuposOcupados = organizacion == null
                    ? inscripcionRepository.countByEventoIdAndEstado(evento.getId(), EstadoInscripcion.CONFIRMADA)
                    : inscripcionRepository.countByEventoIdAndOrganizacionIdAndEstado(evento.getId(), organizacion.getId(), EstadoInscripcion.CONFIRMADA);
            if (cuposOcupados >= evento.getAforoMaximo()) {
                throw new IllegalStateException("No hay cupos disponibles para este evento. Aforo completo.");
            }
        }
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado con correo " + correo));
    }

    @Transactional(readOnly = true)
    public List<ParticipanteInscritoRespuesta> listarParticipantes(Long idEvento, Organizacion organizacion) {
        List<Inscripcion> inscripciones;
        if (idEvento != null && idEvento > 0) {
            inscripciones = organizacion == null
                    ? inscripcionRepository.findByEventoIdConDetalles(idEvento)
                    : inscripcionRepository.findByEventoIdAndOrganizacionIdConDetalles(idEvento, organizacion.getId());
        } else {
            inscripciones = organizacion == null
                    ? inscripcionRepository.findAllConDetalles()
                    : inscripcionRepository.findByOrganizacionIdConDetalles(organizacion.getId());
        }
        return inscripciones.stream()
                .map(ParticipanteInscritoRespuesta::desdeEntidad)
                .toList();
    }

    private String generarCodigoQr(Long idEvento, Long idParticipante) {
        return "INS-" + idEvento + "-" + idParticipante + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
