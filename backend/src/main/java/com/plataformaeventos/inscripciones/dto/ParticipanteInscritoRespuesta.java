package com.plataformaeventos.inscripciones.dto;

import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;

import java.time.OffsetDateTime;

public record ParticipanteInscritoRespuesta(
        Long idInscripcion,
        Long idUsuario,
        String nombres,
        String apellidos,
        String correo,
        Long idEvento,
        String tituloEvento,
        OffsetDateTime fechaInscripcion,
        EstadoInscripcion estado,
        String codigoQr
) {
    public static ParticipanteInscritoRespuesta desdeEntidad(Inscripcion inscripcion) {
        return new ParticipanteInscritoRespuesta(
                inscripcion.getId(),
                inscripcion.getParticipante().getId(),
                inscripcion.getParticipante().getNombres(),
                inscripcion.getParticipante().getApellidos(),
                inscripcion.getParticipante().getCorreo(),
                inscripcion.getEvento().getId(),
                inscripcion.getEvento().getNombre(),
                inscripcion.getFechaInscripcion(),
                inscripcion.getEstado(),
                inscripcion.getCodigoQr()
        );
    }
}
