package com.plataformaeventos.autenticacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudRecuperacionClave(
        @NotBlank(message = "Ingresa un correo electr\u00f3nico v\u00e1lido.")
        @Email(message = "Ingresa un correo electr\u00f3nico v\u00e1lido.")
        @Size(max = 254, message = "El correo no debe superar 254 caracteres")
        String correo
) {
}
