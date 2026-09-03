package com.plataformaeventos.recursos.dto;

import com.plataformaeventos.recursos.EstadoRecurso;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRecursoSolicitud(
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoRecurso estado
) {
}
