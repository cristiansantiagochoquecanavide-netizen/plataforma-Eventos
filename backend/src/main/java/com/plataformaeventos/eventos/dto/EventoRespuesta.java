package com.plataformaeventos.eventos.dto;

import com.plataformaeventos.categorias.dto.CategoriaRespuesta;

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
        String categoriaNombre,
        EspacioEventoRespuesta espacio,
        Integer aforoMaximo,
        String estado,
        List<ResponsableEventoRespuesta> responsables,
        List<TipoEntradaDTO> tiposEntrada
) {
}
