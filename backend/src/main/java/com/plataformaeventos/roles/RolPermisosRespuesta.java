package com.plataformaeventos.roles;

import com.plataformaeventos.permisos.PermisoRespuesta;

import java.util.List;

public record RolPermisosRespuesta(
        Long idRol,
        String nombreRol,
        List<PermisoRespuesta> permisos
) {
}
