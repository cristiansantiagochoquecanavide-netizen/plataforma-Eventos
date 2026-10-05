package com.plataformaeventos.grupos.dto;

import com.plataformaeventos.grupos.modelo.GrupoUsuario;

import java.util.Comparator;
import java.util.List;

public record GrupoUsuarioRespuesta(
        Long id,
        Long idOrganizacion,
        String nombre,
        String descripcion,
        boolean activo,
        List<MiembroGrupoRespuesta> miembros,
        List<RolGrupoRespuesta> roles
) {
    public static GrupoUsuarioRespuesta desde(GrupoUsuario grupo) {
        return new GrupoUsuarioRespuesta(
                grupo.getId(),
                grupo.getOrganizacion().getId(),
                grupo.getNombre(),
                grupo.getDescripcion(),
                grupo.isActivo(),
                grupo.getMiembros().stream()
                        .map(MiembroGrupoRespuesta::desde)
                        .sorted(Comparator.comparing(MiembroGrupoRespuesta::apellidos).thenComparing(MiembroGrupoRespuesta::nombres))
                        .toList(),
                grupo.getRoles().stream()
                        .map(RolGrupoRespuesta::desde)
                        .sorted(Comparator.comparing(RolGrupoRespuesta::nombre))
                        .toList()
        );
    }
}
