package com.plataformaeventos.inscripciones.servicio;

import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.inscripciones.dto.DisponibilidadEventoRespuesta;
import com.plataformaeventos.inscripciones.dto.InscripcionRespuesta;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    public InscripcionService(
            InscripcionRepository inscripcionRepository,
            EventoRepository eventoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.inscripcionRepository = inscripcionRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public InscripcionRespuesta inscribir(Long idEvento, String correoUsuario) {
        Usuario participante = buscarUsuario(correoUsuario);
        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Evento no encontrado con identificador " + idEvento));

        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new IllegalStateException("Solo es posible inscribirse a eventos en estado PUBLICADO");
        }

        if (evento.getFechaFin() != null && evento.getFechaFin().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("No es posible inscribirse a un evento que ya ha finalizado");
        }

        var inscripcionExistente = inscripcionRepository.findByEventoIdAndParticipanteId(idEvento, participante.getId());
        if (inscripcionExistente.isPresent()) {
            Inscripcion existente = inscripcionExistente.get();
            if (existente.estaConfirmada()) {
                throw new IllegalStateException("Ya cuentas con una inscripción confirmada para este evento");
            }
            validarCuposDisponibles(evento);
            String nuevoCodigo = generarCodigoQr(idEvento, participante.getId());
            existente.reactivar(nuevoCodigo);
            return InscripcionRespuesta.desdeEntidad(inscripcionRepository.save(existente));
        }

        validarCuposDisponibles(evento);
        String codigoQr = generarCodigoQr(idEvento, participante.getId());
        Inscripcion nuevaInscripcion = new Inscripcion(evento, participante, codigoQr);
        return InscripcionRespuesta.desdeEntidad(inscripcionRepository.save(nuevaInscripcion));
    }

    @Transactional(readOnly = true)
    public List<InscripcionRespuesta> listarMisInscripciones(String correoUsuario) {
        Usuario participante = buscarUsuario(correoUsuario);
        return inscripcionRepository.findByParticipanteIdConEvento(participante.getId())
                .stream()
                .map(InscripcionRespuesta::desdeEntidad)
                .toList();
    }

    @Transactional
    public InscripcionRespuesta cancelarInscripcion(Long idInscripcion, String correoUsuario) {
        Usuario participante = buscarUsuario(correoUsuario);
        Inscripcion inscripcion = inscripcionRepository.findByIdAndParticipanteId(idInscripcion, participante.getId())
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
        return InscripcionRespuesta.desdeEntidad(inscripcionRepository.save(inscripcion));
    }

    @Transactional(readOnly = true)
    public DisponibilidadEventoRespuesta consultarDisponibilidad(Long idEvento, String correoUsuario) {
        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Evento no encontrado con identificador " + idEvento));

        long cuposOcupados = inscripcionRepository.countByEventoIdAndEstado(idEvento, EstadoInscripcion.CONFIRMADA);
        Integer aforo = evento.getAforoMaximo();
        Integer cuposDisponibles = aforo != null ? Math.max(0, aforo - (int) cuposOcupados) : null;
        boolean tieneCupo = aforo == null || cuposDisponibles > 0;

        boolean estaInscrito = false;
        Long idInscripcionActiva = null;

        if (correoUsuario != null && !correoUsuario.isBlank()) {
            var usuarioOpt = usuarioRepository.findByCorreo(correoUsuario.trim().toLowerCase(Locale.ROOT));
            if (usuarioOpt.isPresent()) {
                var insOpt = inscripcionRepository.findByEventoIdAndParticipanteId(idEvento, usuarioOpt.get().getId());
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

    private void validarCuposDisponibles(Evento evento) {
        if (evento.getAforoMaximo() != null) {
            long cuposOcupados = inscripcionRepository.countByEventoIdAndEstado(evento.getId(), EstadoInscripcion.CONFIRMADA);
            if (cuposOcupados >= evento.getAforoMaximo()) {
                throw new IllegalStateException("No hay cupos disponibles para este evento. Aforo completo.");
            }
        }
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado con correo " + correo));
    }

    private String generarCodigoQr(Long idEvento, Long idParticipante) {
        return "INS-" + idEvento + "-" + idParticipante + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
