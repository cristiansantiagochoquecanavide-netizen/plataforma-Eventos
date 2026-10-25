package com.plataformaeventos.reportes.dto;

import java.util.List;
import java.util.Map;

public record ReporteMetricasRespuesta(
        long totalEventos,
        long eventosPublicados,
        long eventosFinalizados,
        long totalInscripciones,
        long inscripcionesConfirmadas,
        long capacidadTotalEspacios,
        double tasaOcupacionPromedio,
        List<EventoMetricaItem> detalleEventos,
        List<CategoriaMetricaItem> distribucionCategorias,
        Map<String, Long> inscripcionesPorMes
) {
    public record EventoMetricaItem(
            Long idEvento,
            String nombre,
            String categoria,
            String estado,
            String fechaInicio,
            Integer aforoMaximo,
            long inscritos,
            double porcentajeLlenado
    ) {}

    public record CategoriaMetricaItem(
            String categoria,
            long cantidadEventos
    ) {}
}
