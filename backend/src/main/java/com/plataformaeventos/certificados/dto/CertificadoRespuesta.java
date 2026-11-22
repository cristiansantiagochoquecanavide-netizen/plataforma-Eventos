package com.plataformaeventos.certificados.dto;

import java.time.OffsetDateTime;

public record CertificadoRespuesta(
        Long idCertificado,
        Long idInscripcion,
        Long idEvento,
        String tituloEvento,
        Long idParticipante,
        String nombreParticipante,
        String correoParticipante,
        String codigoVerificacion,
        String hashSeguridad,
        OffsetDateTime fechaEmision,
        Long idOrganizacion,
        String nombreOrganizacion
) {}
