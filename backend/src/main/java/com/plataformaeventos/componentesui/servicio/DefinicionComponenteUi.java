package com.plataformaeventos.componentesui.servicio;

import com.plataformaeventos.componentesui.modelo.EstadoPrivilegioUi;
import com.plataformaeventos.componentesui.modelo.TipoComponenteUi;

import java.util.Set;

public record DefinicionComponenteUi(
        String clave,
        TipoComponenteUi tipo,
        String descripcion,
        Set<EstadoPrivilegioUi> estadosSoportados,
        boolean controlRecuperacion
) {
}
