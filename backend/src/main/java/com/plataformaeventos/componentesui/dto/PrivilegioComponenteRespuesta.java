package com.plataformaeventos.componentesui.dto;

import com.plataformaeventos.componentesui.modelo.EstadoPrivilegioUi;

import java.util.Set;

public record PrivilegioComponenteRespuesta(
        String claveComponente,
        Set<EstadoPrivilegioUi> estados
) {
}
