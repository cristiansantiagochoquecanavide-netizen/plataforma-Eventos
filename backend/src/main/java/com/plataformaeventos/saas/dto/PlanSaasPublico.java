package com.plataformaeventos.saas.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlanSaasPublico(
        String codigo,
        String nombre,
        BigDecimal precioMensualBob,
        boolean gratuito,
        int usuariosInternos,
        int eventosActivos,
        int inscripcionesMes,
        List<String> reportes
) { }
