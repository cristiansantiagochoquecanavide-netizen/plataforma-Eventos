package com.plataformaeventos.autenticacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InicioSesionSolicitud(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        String correo,
        @NotBlank(message = "La clave es obligatoria")
        String clave
) {
}
