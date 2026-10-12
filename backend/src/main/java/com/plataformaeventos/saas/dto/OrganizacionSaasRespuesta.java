package com.plataformaeventos.saas.dto;

import com.plataformaeventos.saas.modelo.EstadoSuscripcion;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import java.time.OffsetDateTime;

public record OrganizacionSaasRespuesta(
        Long id,
        String nombre,
        String slug,
        String estado,
        OffsetDateTime fechaCreacion,
        Long miembrosActivos,
        PlanSuscripcion plan,
        EstadoSuscripcion estadoSuscripcion,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        boolean suscripcionVigente
) {
    public static OrganizacionSaasRespuesta desde(OrganizacionSaasFila fila, OffsetDateTime ahora) {
        boolean vigente = fila.estadoSuscripcion() == EstadoSuscripcion.ACTIVA
                && (fila.fechaInicio() == null || !fila.fechaInicio().isAfter(ahora))
                && (fila.fechaFin() == null || fila.fechaFin().isAfter(ahora));
        return new OrganizacionSaasRespuesta(
                fila.id(), fila.nombre(), fila.slug(), fila.estado(), fila.fechaCreacion(),
                fila.miembrosActivos(), fila.plan(), fila.estadoSuscripcion(),
                fila.fechaInicio(), fila.fechaFin(), vigente);
    }
}
