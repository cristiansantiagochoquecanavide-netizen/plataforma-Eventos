package com.plataformaeventos.participantes.dto;

import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoParticipanteSolicitud(
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoUsuario estado
) {}
