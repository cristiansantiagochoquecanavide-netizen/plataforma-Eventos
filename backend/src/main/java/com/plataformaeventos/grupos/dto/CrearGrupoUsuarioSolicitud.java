package com.plataformaeventos.grupos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearGrupoUsuarioSolicitud(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 255) String descripcion
) {
}
