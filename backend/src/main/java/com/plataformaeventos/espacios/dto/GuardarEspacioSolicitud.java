package com.plataformaeventos.espacios.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GuardarEspacioSolicitud(
        @NotBlank(message = "El nombre del espacio es obligatorio")
        @Size(max = 120, message = "El nombre del espacio no debe superar 120 caracteres")
        String nombre,

        @NotBlank(message = "La ubicacion del espacio es obligatoria")
        @Size(max = 255, message = "La ubicacion no debe superar 255 caracteres")
        String ubicacion,

        @NotNull(message = "La capacidad es obligatoria")
        @Positive(message = "La capacidad debe ser mayor que cero")
        Integer capacidad,

        @DecimalMin(value = "0.00", message = "El costo de alquiler no puede ser negativo")
        BigDecimal costoAlquiler
) {
    public GuardarEspacioSolicitud(String nombre, String ubicacion, Integer capacidad) {
        this(nombre, ubicacion, capacidad, BigDecimal.ZERO);
    }
}
