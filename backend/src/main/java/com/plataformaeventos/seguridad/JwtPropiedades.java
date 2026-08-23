package com.plataformaeventos.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seguridad.jwt")
public record JwtPropiedades(
        String secreto,
        long accesoExpiracionMinutos,
        long refrescoExpiracionDias
) {
}
