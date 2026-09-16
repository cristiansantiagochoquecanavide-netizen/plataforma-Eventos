package com.plataformaeventos.convocatorias.postulaciones.dto;

import com.plataformaeventos.convocatorias.modelo.TipoPersonal;
import com.plataformaeventos.convocatorias.postulaciones.EstadoPostulacion;

import java.time.OffsetDateTime;

public record PostulanteConvocatoriaRespuesta(
        Long id,
        Long idUsuario,
        String nombres,
        String apellidos,
        String correo,
        Long idConvocatoria,
        String nombreEvento,
        TipoPersonal tipoPersonal,
        EstadoPostulacion estado,
        OffsetDateTime fechaPostulacion,
        OffsetDateTime fechaActualizacion
) {
}
