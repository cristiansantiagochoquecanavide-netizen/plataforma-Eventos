package com.plataformaeventos.inscripciones.dto;

public record DisponibilidadEventoRespuesta(
        Long idEvento,
        Integer aforoMaximo,
        long cuposOcupados,
        Integer cuposDisponibles,
        boolean tieneCupo,
        boolean estaInscrito,
        Long idInscripcionActiva
) {
}
