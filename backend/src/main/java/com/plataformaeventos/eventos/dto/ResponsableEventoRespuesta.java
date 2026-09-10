package com.plataformaeventos.eventos.dto;

import java.util.List;

public record ResponsableEventoRespuesta(
        Long id,
        String nombres,
        String apellidos,
        String correo,
        List<String> roles
) {
}
