package com.plataformaeventos.recursos.dto;

import com.plataformaeventos.recursos.modelo.EstadoRecurso;
import com.plataformaeventos.recursos.modelo.TipoRecurso;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarRecursoSolicitud(
        @NotBlank(message = "El codigo del recurso es obligatorio")
        @Size(max = 30, message = "El codigo no debe superar 30 caracteres")
        String codigo,

        @NotBlank(message = "El nombre del recurso es obligatorio")
        @Size(max = 120, message = "El nombre no debe superar 120 caracteres")
        String nombre,

        @NotNull(message = "El tipo de recurso es obligatorio")
        TipoRecurso tipo,

        @NotNull(message = "La cantidad total es obligatoria")
        @Min(value = 0, message = "La cantidad total no puede ser negativa")
        Integer cantidadTotal,

        @NotNull(message = "La cantidad disponible es obligatoria")
        @Min(value = 0, message = "La cantidad disponible no puede ser negativa")
        Integer cantidadDisponible,

        @NotNull(message = "El estado del recurso es obligatorio")
        EstadoRecurso estado,

        @Size(max = 150, message = "La ubicacion no debe superar 150 caracteres")
        String ubicacion,

        @Size(max = 255, message = "La descripcion no debe superar 255 caracteres")
        String descripcion
) {
}
