package com.plataformaeventos.inscripciones.dto;

import java.time.OffsetDateTime;

public record CredencialDigitalRespuesta(
        Long idInscripcion,
        String nombreParticipante,
        String nombreEvento,
        String categoria,
        OffsetDateTime fechaEvento,
        String lugar,
        String token,
        String qrPngBase64,
        OffsetDateTime expiraEn
) { }
