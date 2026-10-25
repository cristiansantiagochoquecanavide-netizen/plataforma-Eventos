package com.plataformaeventos.convocatorias.dto;

import com.plataformaeventos.convocatorias.modelo.EstadoConvocatoria;
import com.plataformaeventos.convocatorias.modelo.TipoPersonal;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ConvocatoriaRespuesta(
        Long id,
        Long idEvento,
        String nombreEvento,
        TipoPersonal tipoPersonal,
        String descripcion,
        Integer cantidadRequerida,
        LocalDate fechaLimite,
        EstadoConvocatoria estado,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
}
