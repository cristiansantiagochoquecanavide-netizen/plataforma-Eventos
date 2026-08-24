package com.plataformaeventos.perfil;

import com.plataformaeventos.seguridad.ClaveSegura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarClaveSolicitud(
        @NotBlank(message = "La clave actual es obligatoria")
        String claveActual,
        @NotBlank(message = "La nueva clave es obligatoria")
        @Size(max = 72, message = "La nueva clave no debe superar 72 caracteres")
        @ClaveSegura
        String nuevaClave
) {
}
