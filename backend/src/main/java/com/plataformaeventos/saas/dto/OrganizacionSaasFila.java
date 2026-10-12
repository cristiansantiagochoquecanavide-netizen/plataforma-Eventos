package com.plataformaeventos.saas.dto;

import com.plataformaeventos.saas.modelo.EstadoSuscripcion;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import java.time.OffsetDateTime;

public record OrganizacionSaasFila(
        Long id,
        String nombre,
        String slug,
        String estado,
        OffsetDateTime fechaCreacion,
        Long miembrosActivos,
        PlanSuscripcion plan,
        EstadoSuscripcion estadoSuscripcion,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin
) {
}
