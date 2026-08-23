package com.plataformaeventos.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seguridad.recuperacion")
public record RecuperacionClavePropiedades(
        long expiracionMinutos,
        String urlFrontendWeb,
        String correoRemitente
) {
}
