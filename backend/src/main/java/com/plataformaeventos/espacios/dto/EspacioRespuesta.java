package com.plataformaeventos.espacios.dto;

import java.math.BigDecimal;

public record EspacioRespuesta(Long id, String nombre, String ubicacion, Integer capacidad, BigDecimal costoAlquiler) {
    public EspacioRespuesta(Long id, String nombre, String ubicacion, Integer capacidad) {
        this(id, nombre, ubicacion, capacidad, BigDecimal.ZERO);
    }
}
