package com.plataformaeventos.autenticacion.dto;

import java.util.List;

public record UsuarioActualRespuesta(
        Long id,
        String nombres,
        String apellidos,
        String correo,
        String estado,
        List<String> roles,
        List<String> permisos
) {
}
