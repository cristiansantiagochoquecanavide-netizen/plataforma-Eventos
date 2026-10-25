package com.plataformaeventos.saas.servicio;

import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.saas.modelo.EstadoSuscripcion;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import com.plataformaeventos.saas.modelo.SuscripcionOrganizacion;
import com.plataformaeventos.saas.repositorio.SuscripcionOrganizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class SuscripcionOrganizacionService {
    private final SuscripcionOrganizacionRepository suscripciones;

    public SuscripcionOrganizacionService(SuscripcionOrganizacionRepository suscripciones) {
        this.suscripciones = suscripciones;
    }

    @Transactional
    public void crearPlanBaseSiFalta(Organizacion organizacion) {
        if (suscripciones.existsById(organizacion.getId())) return;
        suscripciones.save(new SuscripcionOrganizacion(
                organizacion, PlanSuscripcion.BASICO, EstadoSuscripcion.ACTIVA, OffsetDateTime.now(), null));
    }

    @Transactional(readOnly = true)
    public boolean tieneAccesoActivo(Long idOrganizacion, OffsetDateTime ahora) {
        return suscripciones.findByOrganizacionId(idOrganizacion)
                .map(suscripcion -> suscripcion.estaVigente(ahora))
                .orElse(false);
    }
}
