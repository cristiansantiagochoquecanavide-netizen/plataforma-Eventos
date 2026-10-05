package com.plataformaeventos.grupos.dto;

import com.plataformaeventos.roles.modelo.Rol;

public record RolGrupoRespuesta(Long idRol, String nombre) {
    public static RolGrupoRespuesta desde(Rol rol) {
        return new RolGrupoRespuesta(rol.getId(), rol.getNombre());
    }
}
