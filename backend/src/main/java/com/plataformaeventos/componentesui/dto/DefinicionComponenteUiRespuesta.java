package com.plataformaeventos.componentesui.dto;

import com.plataformaeventos.componentesui.modelo.EstadoPrivilegioUi;
import com.plataformaeventos.componentesui.modelo.TipoComponenteUi;

import java.util.Set;

public record DefinicionComponenteUiRespuesta(
        String clave,
        TipoComponenteUi tipo,
        String descripcion,
        Set<EstadoPrivilegioUi> estadosSoportados,
        boolean controlRecuperacion
) {
}
