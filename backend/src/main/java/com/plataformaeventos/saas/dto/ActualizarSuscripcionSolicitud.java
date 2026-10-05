package com.plataformaeventos.saas.dto;

import com.plataformaeventos.saas.modelo.EstadoSuscripcion;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record ActualizarSuscripcionSolicitud(
        @NotNull PlanSuscripcion plan,
        @NotNull EstadoSuscripcion estado,
        @NotNull OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin
) {
    @AssertTrue(message = "La fecha fin debe ser posterior a la fecha inicio")
    public boolean isPeriodoValido() {
        return fechaInicio != null && (fechaFin == null || fechaFin.isAfter(fechaInicio));
    }
}
