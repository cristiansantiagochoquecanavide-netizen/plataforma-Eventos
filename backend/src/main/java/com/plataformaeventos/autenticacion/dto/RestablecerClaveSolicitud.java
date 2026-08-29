package com.plataformaeventos.autenticacion.dto;

import com.plataformaeventos.seguridad.ClaveSegura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerClaveSolicitud(
        @NotBlank(message = "El token es obligatorio")
        String token,
        @NotBlank(message = "La nueva clave es obligatoria")
        @Size(max = 72, message = "La nueva clave no debe superar 72 caracteres")
        @ClaveSegura(message = "La contrase\u00f1a no cumple los requisitos.")
        String nuevaClave
) {
}
