package com.plataformaeventos.autenticacion.dto;

public record SesionRespuesta(
        String accessToken,
        String refreshToken,
        String tipoToken
) {
}
