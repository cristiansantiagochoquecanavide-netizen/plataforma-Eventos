package com.plataformaeventos.autenticacion.dto;

import java.util.List;

public record PerfilOrganizacionActivaRespuesta(
        Long idOrganizacion,
        List<String> roles,
        List<String> permisos,
        boolean administrador
) {
}
