package com.plataformaeventos.perfil;

import java.util.List;

public record PerfilRespuesta(
        Long id,
        String nombres,
        String apellidos,
        String correo,
        List<String> roles
) {
}
