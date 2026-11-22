package com.plataformaeventos.reportes.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenFinancieroDTO(
        BigDecimal ingresosTotales,
        BigDecimal costosTotales,
        BigDecimal balanceNetoGlobal,
        Double roiGlobal,
        long eventosConGanancia,
        long eventosConPerdida,
        long eventosEnEquilibrio,
        List<EventoFinancieroDTO> detalleEventos
) {
}
