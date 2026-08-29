package com.plataformaeventos.roles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearRolSolicitud(
        @NotBlank(message = "El nombre es obligatorio")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$", message = "El nombre debe usar mayusculas, numeros o guion bajo")
        String nombre,
        @Size(max = 255, message = "La descripcion no debe superar 255 caracteres")
        String descripcion
) {
}
