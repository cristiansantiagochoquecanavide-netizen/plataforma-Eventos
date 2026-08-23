package com.plataformaeventos.autenticacion;

import jakarta.validation.constraints.NotBlank;

public record TokenRefrescoSolicitud(
        @NotBlank(message = "El token de refresco es obligatorio")
        String tokenRefresco
) {
}
