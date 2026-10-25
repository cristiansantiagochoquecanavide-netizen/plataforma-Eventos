package com.plataformaeventos.perfil.dto;

import java.util.List;

public record PerfilRespuesta(
        Long id,
        String nombres,
        String apellidos,
        String correo,
        List<String> roles,
        List<String> permisos
) {
}
