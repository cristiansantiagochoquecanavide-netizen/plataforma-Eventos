package com.plataformaeventos.roles;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AsignarPermisosSolicitud(
        @NotNull(message = "La lista de permisos es obligatoria")
        Set<@NotBlank(message = "El nombre del permiso es obligatorio") String> permisos
) {
}
