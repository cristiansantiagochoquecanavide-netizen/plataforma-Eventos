package com.plataformaeventos.inscripciones.dto;

import com.plataformaeventos.asistencias.modelo.Asistencia;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;

import java.time.OffsetDateTime;

/** Datos del historial propio del participante para el CU27. */
public record HistorialParticipacionRespuesta(
        Long idInscripcion,
        Long idEvento,
        String nombreEvento,
        String descripcionEvento,
        String categoriaEvento,
        OffsetDateTime fechaInicioEvento,
        OffsetDateTime fechaFinEvento,
        String lugarEvento,
        OffsetDateTime fechaInscripcion,
        EstadoInscripcion estadoInscripcion,
        boolean asistio,
        OffsetDateTime fechaAsistencia
) {
    public static HistorialParticipacionRespuesta desde(Inscripcion inscripcion, Asistencia asistencia) {
        var evento = inscripcion.getEvento();
        String categoria = evento.getCategoria() != null ? evento.getCategoria().getNombre() : "GENERAL";
        String lugar = evento.getLugarPrincipal();
        if ((lugar == null || lugar.isBlank()) && evento.getEspacio() != null) {
            lugar = evento.getEspacio().getNombre() + " (" + evento.getEspacio().getUbicacion() + ")";
        }
        return new HistorialParticipacionRespuesta(
                inscripcion.getId(),
                evento.getId(),
                evento.getNombre(),
                evento.getDescripcion(),
                categoria,
                evento.getFechaInicio(),
                evento.getFechaFin(),
                lugar != null ? lugar : "Sin lugar asignado",
                inscripcion.getFechaInscripcion(),
                inscripcion.getEstado(),
                asistencia != null,
                asistencia == null ? null : asistencia.getFechaHora()
        );
    }
}
