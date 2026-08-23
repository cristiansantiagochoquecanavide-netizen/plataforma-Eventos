package com.plataformaeventos.usuarios;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUsuarioSolicitud(
        @NotNull(message = "El estado es obligatorio")
        EstadoUsuario estado
) {
}
