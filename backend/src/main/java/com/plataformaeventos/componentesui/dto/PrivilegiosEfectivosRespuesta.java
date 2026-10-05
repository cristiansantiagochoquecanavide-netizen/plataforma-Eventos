package com.plataformaeventos.componentesui.dto;

import java.util.Map;

public record PrivilegiosEfectivosRespuesta(
        Map<String, EstadoComponenteEfectivoRespuesta> componentes
) {
}
