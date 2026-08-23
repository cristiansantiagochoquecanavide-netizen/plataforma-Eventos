package com.plataformaeventos.autenticacion;

public record SesionRespuesta(
        String accessToken,
        String refreshToken,
        String tipoToken
) {
}
