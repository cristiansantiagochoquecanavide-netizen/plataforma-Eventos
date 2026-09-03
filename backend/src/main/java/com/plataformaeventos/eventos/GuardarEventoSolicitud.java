package com.plataformaeventos.eventos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Set;

public record GuardarEventoSolicitud(
        @NotBlank(message = "El nombre del evento es obligatorio")
        @Size(max = 150, message = "El nombre del evento no debe superar 150 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripcion no debe superar 500 caracteres")
        String descripcion,

        @NotBlank(message = "La categoria es obligatoria")
        @Size(max = 80, message = "La categoria no debe superar 80 caracteres")
        String categoria,

        @NotNull(message = "La fecha de inicio es obligatoria")
        OffsetDateTime fechaInicio,

        OffsetDateTime fechaFin,

        @Size(max = 150, message = "El lugar principal no debe superar 150 caracteres")
        String lugarPrincipal,

        EstadoEvento estado,

        @NotEmpty(message = "Debe asignarse al menos un responsable")
        Set<@NotNull(message = "El responsable es obligatorio") Long> responsables
) {
}
