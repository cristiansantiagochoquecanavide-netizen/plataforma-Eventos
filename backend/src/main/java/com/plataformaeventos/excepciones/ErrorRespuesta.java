package com.plataformaeventos.excepciones;

import java.time.OffsetDateTime;

public record ErrorRespuesta(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
