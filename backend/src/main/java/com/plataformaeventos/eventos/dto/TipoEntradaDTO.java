package com.plataformaeventos.eventos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TipoEntradaDTO(
        Long id,

        @NotBlank(message = "El nombre de la categoria de entrada es obligatorio")
        @Size(max = 100, message = "El nombre de la categoria de entrada no debe superar 100 caracteres")
        String nombre,

        @NotNull(message = "El precio de la entrada es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio de la entrada no puede ser negativo")
        BigDecimal precio,

        @NotNull(message = "El cupo maximo de la entrada es obligatorio")
        @Min(value = 1, message = "El cupo maximo de la entrada debe ser mayor que cero")
        Integer cupoMaximo,

        @Size(max = 255, message = "La descripcion de la entrada no debe superar 255 caracteres")
        String descripcion,

        Integer entradasVendidas,
        Integer cuposDisponibles
) {
    public TipoEntradaDTO(Long id, String nombre, BigDecimal precio, Integer cupoMaximo, String descripcion) {
        this(id, nombre, precio, cupoMaximo, descripcion, 0, cupoMaximo);
    }
}
