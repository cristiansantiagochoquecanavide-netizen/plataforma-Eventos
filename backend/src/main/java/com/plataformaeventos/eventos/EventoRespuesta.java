package com.plataformaeventos.eventos;

import java.time.OffsetDateTime;
import java.util.List;

public record EventoRespuesta(
        Long id,
        String nombre,
        String descripcion,
        String categoria,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        String lugarPrincipal,
        EspacioEventoRespuesta espacio,
        Integer aforoMaximo,
        String estado,
        List<ResponsableEventoRespuesta> responsables
) {
}
