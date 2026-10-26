package com.plataformaeventos.asistencias.dto;

import com.plataformaeventos.asistencias.modelo.Asistencia;
import java.time.OffsetDateTime;

public record AsistenciaRespuesta(
        Long idAsistencia,
        Long idInscripcion,
        String participante,
        String correoParticipante,
        Long idEvento,
        String nombreEvento,
        OffsetDateTime fechaHora,
        Long idRegistradoPor,
        String nombreRegistradoPor
) {
    public static AsistenciaRespuesta desde(Asistencia asistencia) {
        var inscripcion = asistencia.getInscripcion();
        var participante = inscripcion.getParticipante();
        var evento = inscripcion.getEvento();
        var staff = asistencia.getRegistradoPor();
        return new AsistenciaRespuesta(asistencia.getId(), inscripcion.getId(),
                participante.getNombres() + " " + participante.getApellidos(), participante.getCorreo(),
                evento.getId(), evento.getNombre(), asistencia.getFechaHora(), staff.getId(),
                staff.getNombres() + " " + staff.getApellidos());
    }
}
