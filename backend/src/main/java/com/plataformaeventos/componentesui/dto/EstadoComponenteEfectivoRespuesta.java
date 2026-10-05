package com.plataformaeventos.componentesui.dto;

import com.plataformaeventos.componentesui.modelo.EstadoPrivilegioUi;

import java.util.Set;

public record EstadoComponenteEfectivoRespuesta(
        Set<EstadoPrivilegioUi> estadosPermitidos
) {
    public boolean permitido(EstadoPrivilegioUi estado) {
        return estadosPermitidos.contains(estado);
    }
}
