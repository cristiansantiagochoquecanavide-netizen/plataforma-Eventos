package com.plataformaeventos.usuarios.dto;

import java.util.List;

public record UsuarioRespuesta(
        Long id,
        String nombres,
        String apellidos,
        String correo,
        String estado,
        List<String> roles
) {
}
