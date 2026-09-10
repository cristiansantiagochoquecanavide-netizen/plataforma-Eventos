package com.plataformaeventos.recursos.dto;

import com.plataformaeventos.recursos.modelo.EstadoRecurso;
import com.plataformaeventos.recursos.modelo.TipoRecurso;

import java.time.OffsetDateTime;

public record RecursoRespuesta(
        Long id,
        String codigo,
        String nombre,
        TipoRecurso tipo,
        Integer cantidadTotal,
        Integer cantidadDisponible,
        EstadoRecurso estado,
        String ubicacion,
        String descripcion,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
}
