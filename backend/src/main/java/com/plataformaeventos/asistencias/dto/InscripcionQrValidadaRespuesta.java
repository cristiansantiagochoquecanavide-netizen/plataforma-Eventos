package com.plataformaeventos.asistencias.dto;

public record InscripcionQrValidadaRespuesta(
        Long idInscripcion,
        String participante,
        Long idEvento,
        String nombreEvento
) {}
