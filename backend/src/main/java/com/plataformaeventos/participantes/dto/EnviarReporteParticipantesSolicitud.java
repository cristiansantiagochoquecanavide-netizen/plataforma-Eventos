package com.plataformaeventos.participantes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnviarReporteParticipantesSolicitud(
        @NotBlank @Email String correoDestino,
        @NotNull FormatoReporte formato,
        @NotNull @Valid ReporteParticipantesSolicitud reporte
) { }
