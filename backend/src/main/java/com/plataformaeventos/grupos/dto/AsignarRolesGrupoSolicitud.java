package com.plataformaeventos.grupos.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AsignarRolesGrupoSolicitud(@NotNull Set<Long> idsRoles) {
}
