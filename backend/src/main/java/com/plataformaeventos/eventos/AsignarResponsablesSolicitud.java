package com.plataformaeventos.eventos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AsignarResponsablesSolicitud(
        @NotEmpty(message = "Debe asignarse al menos un responsable")
        Set<@NotNull(message = "El responsable es obligatorio") Long> responsables
) {
}
