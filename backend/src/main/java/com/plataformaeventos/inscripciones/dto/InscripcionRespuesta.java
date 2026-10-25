package com.plataformaeventos.inscripciones.dto;

import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;

import java.time.OffsetDateTime;

public record InscripcionRespuesta(
        Long idInscripcion,
        Long idEvento,
        String nombreEvento,
        String descripcionEvento,
        String categoriaEvento,
        OffsetDateTime fechaInicioEvento,
        OffsetDateTime fechaFinEvento,
        String lugarEvento,
        EstadoInscripcion estado,
        String codigoQr,
        OffsetDateTime fechaInscripcion
) {
    public static InscripcionRespuesta desdeEntidad(Inscripcion inscripcion) {
        var evento = inscripcion.getEvento();
        String categoria = evento.getCategoria() != null ? evento.getCategoria().getNombre() : "GENERAL";
        String lugar = evento.getLugarPrincipal();
        if ((lugar == null || lugar.isBlank()) && evento.getEspacio() != null) {
            lugar = evento.getEspacio().getNombre() + " (" + evento.getEspacio().getUbicacion() + ")";
        }
        return new InscripcionRespuesta(
                inscripcion.getId(),
                evento.getId(),
                evento.getNombre(),
                evento.getDescripcion(),
                categoria,
                evento.getFechaInicio(),
                evento.getFechaFin(),
                lugar != null ? lugar : "Sin lugar asignado",
                inscripcion.getEstado(),
                inscripcion.getCodigoQr(),
                inscripcion.getFechaInscripcion()
        );
    }
}
