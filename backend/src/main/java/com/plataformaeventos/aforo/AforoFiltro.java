package com.plataformaeventos.aforo;

import java.time.OffsetDateTime;

public record AforoFiltro(Long eventoId, Long salaId, String categoria, OffsetDateTime desde, OffsetDateTime hasta) {
    public AforoFiltro {
        if (desde != null && hasta != null && !desde.isBefore(hasta))
            throw new IllegalArgumentException("El inicio debe ser anterior al fin de la franja horaria");
        if (categoria != null && categoria.length() > 80) throw new IllegalArgumentException("Categoria invalida");
    }
}
