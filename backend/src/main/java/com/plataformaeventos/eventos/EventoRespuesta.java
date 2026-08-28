package com.plataformaeventos.eventos;

import java.time.OffsetDateTime;
import java.util.List;

public record EventoRespuesta(
        Long id,
        String nombre,
        String descripcion,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        String estado,
        List<ResponsableEventoRespuesta> responsables
) {
}
