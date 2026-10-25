package com.plataformaeventos.componentesui.dto;

import com.plataformaeventos.componentesui.modelo.EstadoPrivilegioUi;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record PrivilegioComponenteSolicitud(
        @NotBlank String claveComponente,
        @NotEmpty Set<EstadoPrivilegioUi> estados
) {
}
