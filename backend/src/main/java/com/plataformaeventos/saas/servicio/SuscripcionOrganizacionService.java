package com.plataformaeventos.saas.servicio;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.audit.servicio.OperacionAuditable;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.saas.dto.ActualizarSuscripcionSolicitud;
import com.plataformaeventos.saas.dto.SuscripcionOrganizacionRespuesta;
import com.plataformaeventos.saas.modelo.EstadoSuscripcion;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import com.plataformaeventos.saas.modelo.SuscripcionOrganizacion;
import com.plataformaeventos.saas.repositorio.SuscripcionOrganizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SuscripcionOrganizacionService {
    private final SuscripcionOrganizacionRepository suscripciones;
    private final OrganizacionRepository organizaciones;

    public SuscripcionOrganizacionService(SuscripcionOrganizacionRepository suscripciones,
                                          OrganizacionRepository organizaciones) {
        this.suscripciones = suscripciones;
        this.organizaciones = organizaciones;
    }

    @Transactional(readOnly = true)
    public List<SuscripcionOrganizacionRespuesta> listar() {
        return suscripciones.findAllByOrderByOrganizacionNombreAsc().stream()
                .map(SuscripcionOrganizacionRespuesta::desde).toList();
    }

    @Transactional
    public SuscripcionOrganizacionRespuesta actualizar(Long idOrganizacion, ActualizarSuscripcionSolicitud solicitud) {
        Organizacion organizacion = organizaciones.findById(idOrganizacion)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Organización no encontrada"));
        return actualizar(organizacion, solicitud);
    }

    @Transactional
    @OperacionAuditable(modulo = "SAAS", entidad = "SUSCRIPCION", accion = "ACTUALIZAR")
    public SuscripcionOrganizacionRespuesta actualizar(Organizacion organizacion,
                                                        ActualizarSuscripcionSolicitud solicitud) {
        Long idOrganizacion = organizacion.getId();
        SuscripcionOrganizacion suscripcion = suscripciones.findByOrganizacionId(idOrganizacion)
                .orElseGet(() -> new SuscripcionOrganizacion(organizacion, solicitud.plan(), solicitud.estado(),
                        solicitud.fechaInicio(), solicitud.fechaFin()));
        suscripcion.actualizar(solicitud.plan(), solicitud.estado(), solicitud.fechaInicio(), solicitud.fechaFin());
        return SuscripcionOrganizacionRespuesta.desde(suscripciones.save(suscripcion));
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
