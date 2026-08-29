package com.plataformaeventos.usuarios.dto;
 
import com.plataformaeventos.usuarios.EstadoUsuario;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUsuarioSolicitud(
        @NotNull(message = "El estado es obligatorio")
        EstadoUsuario estado
) {
}
