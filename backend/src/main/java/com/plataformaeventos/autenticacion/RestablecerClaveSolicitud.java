package com.plataformaeventos.autenticacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RestablecerClaveSolicitud(
        @NotBlank(message = "El token es obligatorio")
        String token,
        @NotBlank(message = "La nueva clave es obligatoria")
        @Size(min = 8, max = 72, message = "La nueva clave debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La nueva clave debe incluir letras y numeros")
        String nuevaClave
) {
}
