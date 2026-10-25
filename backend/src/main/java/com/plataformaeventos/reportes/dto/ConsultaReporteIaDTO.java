package com.plataformaeventos.reportes.dto;

import java.util.Map;

public record ConsultaReporteIaDTO(
        String consulta,
        Map<String, Object> contextoFiltros
) {}
