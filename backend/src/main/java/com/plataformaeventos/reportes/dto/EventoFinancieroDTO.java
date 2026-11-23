package com.plataformaeventos.reportes.dto;

import java.math.BigDecimal;
import java.util.List;

public record EventoFinancieroDTO(
        Long idEvento,
        String nombre,
        String estado,
        String nombreEspacio,
        Integer aforo,
        Integer inscripcionesConfirmadas,
        Integer asistentesReales,
        BigDecimal ingresos,
        BigDecimal costoEspacio,
        BigDecimal costoRecursos,
        BigDecimal costosTotales,
        BigDecimal balanceNeto,
        Double roi,
        String situacion,
        List<DesgloseEntradaFinancieroDTO> desgloseEntradas,
        List<RecursoAsignadoFinancieroDTO> desgloseRecursos
) {
    public record DesgloseEntradaFinancieroDTO(
            String categoria,
            BigDecimal precioUnitario,
            long cantidadVendida,
            Integer cupoMaximo,
            BigDecimal subtotal
    ) {
    }

    public record RecursoAsignadoFinancieroDTO(
            String codigo,
            String nombre,
            String tipo,
            int cantidadAsignada,
            BigDecimal costoUnitario,
            BigDecimal subtotal
    ) {
    }
}
