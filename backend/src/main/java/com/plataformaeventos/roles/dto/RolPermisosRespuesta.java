package com.plataformaeventos.roles.dto;

import com.plataformaeventos.permisos.dto.PermisoRespuesta;

import java.util.List;

public record RolPermisosRespuesta(
        Long idRol,
        String nombreRol,
        List<PermisoRespuesta> permisos
) {
}
