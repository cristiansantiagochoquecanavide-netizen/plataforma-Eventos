package com.plataformaeventos.recursos.asignaciones.dto;

import com.plataformaeventos.recursos.TipoRecurso;
import com.plataformaeventos.recursos.asignaciones.EstadoAsignacion;

import java.time.OffsetDateTime;

public record AsignacionRecursoRespuesta(
        Long id,
        Long idEvento,
        String nombreEvento,
        Long idRecurso,
        String codigoRecurso,
        String nombreRecurso,
        TipoRecurso tipoRecurso,
        String salaEspacio,
        Integer cantidadAsignada,
        EstadoAsignacion estado,
        String observaciones,
        OffsetDateTime fechaAsignacion,
        OffsetDateTime fechaDevolucion
) {
}
