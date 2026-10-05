package com.plataformaeventos.participantes.dto;

import java.util.List;

public record VistaPreviaReporteParticipantesRespuesta(
        List<String> columnas,
        List<List<String>> filas,
        int totalFilas,
        boolean limiteAplicado
) { }
