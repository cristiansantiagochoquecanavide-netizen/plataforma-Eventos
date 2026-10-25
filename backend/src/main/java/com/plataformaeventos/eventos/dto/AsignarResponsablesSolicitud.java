package com.plataformaeventos.eventos.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AsignarResponsablesSolicitud(
        Set<@NotNull(message = "El responsable es obligatorio") Long> responsables
) {
}
