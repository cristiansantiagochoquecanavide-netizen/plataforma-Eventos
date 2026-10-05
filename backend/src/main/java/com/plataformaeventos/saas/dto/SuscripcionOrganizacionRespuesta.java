package com.plataformaeventos.saas.dto;

import com.plataformaeventos.saas.modelo.EstadoSuscripcion;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import com.plataformaeventos.saas.modelo.SuscripcionOrganizacion;

import java.time.OffsetDateTime;

public record SuscripcionOrganizacionRespuesta(
        Long idOrganizacion,
        String nombreOrganizacion,
        String slugOrganizacion,
        PlanSuscripcion plan,
        EstadoSuscripcion estado,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        boolean vigente
) {
    /** Identificador estándar para que la auditoría registre el tenant objetivo de la operación. */
    public Long id() { return idOrganizacion; }

    public static SuscripcionOrganizacionRespuesta desde(SuscripcionOrganizacion suscripcion) {
        return new SuscripcionOrganizacionRespuesta(
                suscripcion.getIdOrganizacion(),
                suscripcion.getOrganizacion().getNombre(),
                suscripcion.getOrganizacion().getSlug(),
                suscripcion.getPlan(),
                suscripcion.getEstado(),
                suscripcion.getFechaInicio(),
                suscripcion.getFechaFin(),
                suscripcion.estaVigente(OffsetDateTime.now())
        );
    }
}
