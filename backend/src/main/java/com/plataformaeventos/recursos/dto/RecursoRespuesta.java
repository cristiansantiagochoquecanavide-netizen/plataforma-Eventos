package com.plataformaeventos.recursos.dto;

import com.plataformaeventos.recursos.modelo.EstadoRecurso;
import com.plataformaeventos.recursos.modelo.TipoRecurso;

import java.math.BigDecimal;
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
        BigDecimal costoUnitario,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
    public RecursoRespuesta(
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
        this(id, codigo, nombre, tipo, cantidadTotal, cantidadDisponible, estado, ubicacion, descripcion, BigDecimal.ZERO, fechaCreacion, fechaActualizacion);
    }
}
