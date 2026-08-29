package com.plataformaeventos.roles.dto;
 
import com.plataformaeventos.roles.EstadoRol;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarRolSolicitud(
        @NotBlank(message = "El nombre es obligatorio")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$", message = "El nombre debe usar mayusculas, numeros o guion bajo")
        String nombre,
        @Size(max = 255, message = "La descripcion no debe superar 255 caracteres")
        String descripcion,
        @NotNull(message = "El estado es obligatorio")
        EstadoRol estado
) {
}
