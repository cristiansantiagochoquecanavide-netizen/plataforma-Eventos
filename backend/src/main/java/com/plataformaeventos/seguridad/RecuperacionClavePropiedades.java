package com.plataformaeventos.seguridad;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "seguridad.recuperacion")
public record RecuperacionClavePropiedades(
        @Min(value = 1, message = "La expiracion de recuperacion debe ser mayor a cero")
        long expiracionMinutos,
        @Min(value = 1, message = "El intervalo de recuperacion debe ser mayor a cero")
        long intervaloSolicitudSegundos,
        @NotBlank(message = "La URL del frontend web es obligatoria")
        @Pattern(regexp = "https?://.+", message = "La URL del frontend web debe ser absoluta")
        String urlFrontendWeb,
        @NotBlank(message = "La URL de la aplicacion movil es obligatoria")
        String urlAplicacionMovil,
        @NotBlank(message = "El correo remitente es obligatorio")
        @Email(message = "El correo remitente no tiene un formato valido")
        String correoRemitente
) {
}
