package com.plataformaeventos.reportes.dto;

import java.util.List;

public record ExportarReporteDinamicoSolicitud(
        String titulo,
        String subtitulo,
        String periodo,
        String resumenEjecutivo,
        List<KpiItem> kpis,
        List<String> columnas,
        List<List<String>> filas,
        List<String> conclusiones,
        List<String> recomendaciones
) {
    public record KpiItem(
            String etiqueta,
            String valor,
            String detalle
    ) {}
}
