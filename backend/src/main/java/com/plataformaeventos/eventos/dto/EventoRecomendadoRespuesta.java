package com.plataformaeventos.eventos.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record EventoRecomendadoRespuesta(
        Long idEvento,
        String titulo,
        String descripcion,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        String ubicacion,
        String nombreCategoria,
        Integer aforoMaximo,
        Integer inscritos,
        Double afinidad,
        String motivoRecomendacion,
        List<String> etiquetas
) {
}
