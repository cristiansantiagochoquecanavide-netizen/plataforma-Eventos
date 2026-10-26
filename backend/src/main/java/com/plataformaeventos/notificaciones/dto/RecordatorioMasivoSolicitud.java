package com.plataformaeventos.notificaciones.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record RecordatorioMasivoSolicitud(
        @NotNull @Future OffsetDateTime fechaProgramada,
        @NotBlank @Size(max = 150) String asunto,
        @NotBlank @Size(max = 4000) String mensaje
) { }
