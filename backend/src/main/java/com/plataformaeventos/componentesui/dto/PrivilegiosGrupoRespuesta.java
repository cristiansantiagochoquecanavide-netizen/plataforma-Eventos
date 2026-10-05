package com.plataformaeventos.componentesui.dto;

import java.util.List;

public record PrivilegiosGrupoRespuesta(
        Long idGrupo,
        List<PrivilegioComponenteRespuesta> privilegios
) {
}
