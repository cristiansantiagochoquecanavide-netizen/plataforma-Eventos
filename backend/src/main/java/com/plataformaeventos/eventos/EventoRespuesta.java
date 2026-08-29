package com.plataformaeventos.eventos;

import java.time.OffsetDateTime;

public record EventoRespuesta(
        Long id,
        String nombre,
        String descripcion,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        String lugarPrincipal,
        EstadoEvento estado
) {
}
