package com.plataformaeventos.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seguridad.recuperacion")
public record RecuperacionClavePropiedades(
        long expiracionMinutos,
        long intervaloSolicitudSegundos,
        String urlFrontendWeb,
        String urlAplicacionMovil,
        String correoRemitente
) {
}
