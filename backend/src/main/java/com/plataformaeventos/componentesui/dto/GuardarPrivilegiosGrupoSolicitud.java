package com.plataformaeventos.componentesui.dto;

import jakarta.validation.Valid;

import java.util.Set;

public record GuardarPrivilegiosGrupoSolicitud(
        @Valid Set<PrivilegioComponenteSolicitud> privilegios
) {
}
