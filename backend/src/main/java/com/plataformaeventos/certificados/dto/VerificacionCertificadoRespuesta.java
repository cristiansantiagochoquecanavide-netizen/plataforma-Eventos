package com.plataformaeventos.certificados.dto;

import java.time.OffsetDateTime;

public record VerificacionCertificadoRespuesta(
        boolean valido,
        String codigoVerificacion,
        String nombreParticipante,
        String tituloEvento,
        String nombreOrganizacion,
        OffsetDateTime fechaEmision,
        OffsetDateTime fechaEvento,
        String hashSeguridad,
        String mensaje
) {}
