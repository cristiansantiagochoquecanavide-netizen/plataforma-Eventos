package com.plataformaeventos.categorias;

import java.time.OffsetDateTime;

public record CategoriaRespuesta(
        Long id,
        String nombre,
        String descripcion,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
}
