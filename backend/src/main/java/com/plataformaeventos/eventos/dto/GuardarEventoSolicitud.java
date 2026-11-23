package com.plataformaeventos.eventos.dto;

import com.plataformaeventos.eventos.modelo.EstadoEvento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public record GuardarEventoSolicitud(
        @NotBlank(message = "El nombre del evento es obligatorio")
        @Size(max = 150, message = "El nombre del evento no debe superar 150 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripcion no debe superar 500 caracteres")
        String descripcion,

        @NotNull(message = "La fecha de inicio es obligatoria")
        OffsetDateTime fechaInicio,

        OffsetDateTime fechaFin,

        @Size(max = 150, message = "El lugar principal no debe superar 150 caracteres")
        String lugarPrincipal,

        @NotNull(message = "La categoria del evento es obligatoria")
        Long idCategoria,

        @NotNull(message = "El espacio es obligatorio")
        Long idEspacio,

        @NotNull(message = "El aforo maximo es obligatorio")
        @Positive(message = "El aforo maximo debe ser mayor que cero")
        Integer aforoMaximo,

        EstadoEvento estado,

        Set<@NotNull(message = "El responsable es obligatorio") Long> responsables,

        List<@Valid TipoEntradaDTO> tiposEntrada
) {
    public GuardarEventoSolicitud(
            String nombre,
            String descripcion,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin,
            String lugarPrincipal,
            Long idCategoria,
            Long idEspacio,
            Integer aforoMaximo,
            EstadoEvento estado,
            Set<Long> responsables
    ) {
        this(nombre, descripcion, fechaInicio, fechaFin, lugarPrincipal, idCategoria, idEspacio, aforoMaximo, estado, responsables, null);
    }
}
