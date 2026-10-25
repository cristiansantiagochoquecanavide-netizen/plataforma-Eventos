package com.plataformaeventos.recursos.dto;

import com.plataformaeventos.recursos.modelo.EstadoAsignacion;
import com.plataformaeventos.recursos.modelo.TipoRecurso;

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
