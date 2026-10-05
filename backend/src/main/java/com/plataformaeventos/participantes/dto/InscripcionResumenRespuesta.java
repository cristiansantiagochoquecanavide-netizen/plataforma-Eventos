package com.plataformaeventos.participantes.dto;

import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;

import java.time.OffsetDateTime;

public record InscripcionResumenRespuesta(
        Long idInscripcion,
        Long idEvento,
        String tituloEvento,
        OffsetDateTime fechaInicioEvento,
        OffsetDateTime fechaFinEvento,
        OffsetDateTime fechaInscripcion,
        String codigoQr,
        EstadoInscripcion estado
) {
    public static InscripcionResumenRespuesta desdeEntidad(Inscripcion inscripcion) {
        return new InscripcionResumenRespuesta(
                inscripcion.getId(),
                inscripcion.getEvento().getId(),
                inscripcion.getEvento().getNombre(),
                inscripcion.getEvento().getFechaInicio(),
                inscripcion.getEvento().getFechaFin(),
                inscripcion.getFechaInscripcion(),
                inscripcion.getCodigoQr(),
                inscripcion.getEstado()
        );
    }
}
