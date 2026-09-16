package com.plataformaeventos.convocatorias.postulaciones.dto;

import com.plataformaeventos.convocatorias.postulaciones.EstadoPostulacion;

import java.time.OffsetDateTime;

public record PostulacionStaffRespuesta(
        Long id,
        Long idUsuario,
        Long idConvocatoria,
        EstadoPostulacion estado,
        OffsetDateTime fechaPostulacion,
        OffsetDateTime fechaActualizacion
) {
}
