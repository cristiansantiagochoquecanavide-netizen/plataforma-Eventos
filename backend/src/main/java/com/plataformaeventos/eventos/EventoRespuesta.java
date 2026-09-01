package com.plataformaeventos.eventos;

import com.plataformaeventos.categorias.CategoriaRespuesta;

import java.time.OffsetDateTime;
import java.util.List;

public record EventoRespuesta(
        Long id,
        String nombre,
        String descripcion,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        String lugarPrincipal,
        CategoriaRespuesta categoria,
        String estado,
        List<ResponsableEventoRespuesta> responsables
) {
}
