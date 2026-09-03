package com.plataformaeventos.recursos.asignaciones.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearAsignacionSolicitud(
        @NotNull(message = "El identificador del evento es obligatorio")
        Long idEvento,

        @NotNull(message = "El identificador del recurso es obligatorio")
        Long idRecurso,

        @NotBlank(message = "La sala o espacio es obligatoria")
        @Size(max = 100, message = "La sala o espacio no debe superar 100 caracteres")
        String salaEspacio,

        @NotNull(message = "La cantidad a asignar es obligatoria")
        @Min(value = 1, message = "La cantidad asignada debe ser mayor que cero")
        Integer cantidadAsignada,

        @Size(max = 255, message = "Las observaciones no deben superar 255 caracteres")
        String observaciones
) {
}
