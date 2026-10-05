package com.plataformaeventos.saas;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.saas.dto.ActualizarSuscripcionSolicitud;
import com.plataformaeventos.saas.modelo.EstadoSuscripcion;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import com.plataformaeventos.saas.modelo.SuscripcionOrganizacion;
import com.plataformaeventos.saas.repositorio.SuscripcionOrganizacionRepository;
import com.plataformaeventos.saas.servicio.SuscripcionOrganizacionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SuscripcionOrganizacionServiceTest {
    private final SuscripcionOrganizacionRepository suscripciones = mock(SuscripcionOrganizacionRepository.class);
    private final OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
    private final SuscripcionOrganizacionService servicio =
            new SuscripcionOrganizacionService(suscripciones, organizaciones);

    @Test
    void suspendeYReactivaPlanConVigenciaManual() {
        Organizacion organizacion = new Organizacion("Tenant", "tenant");
        ReflectionTestUtils.setField(organizacion, "id", 7L);
        OffsetDateTime inicio = OffsetDateTime.now().minusDays(1);
        OffsetDateTime fin = OffsetDateTime.now().plusDays(30);
        SuscripcionOrganizacion suscripcion = new SuscripcionOrganizacion(
                organizacion, PlanSuscripcion.PROFESIONAL, EstadoSuscripcion.SUSPENDIDA, inicio, fin);
        when(organizaciones.findById(7L)).thenReturn(Optional.of(organizacion));
        when(suscripciones.findByOrganizacionId(7L)).thenReturn(Optional.of(suscripcion));
        when(suscripciones.save(suscripcion)).thenReturn(suscripcion);

        var suspendida = servicio.actualizar(7L,
                new ActualizarSuscripcionSolicitud(PlanSuscripcion.PROFESIONAL, EstadoSuscripcion.SUSPENDIDA, inicio, fin));
        assertThat(suspendida.id()).isEqualTo(7L);
        assertThat(suspendida.vigente()).isFalse();

        var activa = servicio.actualizar(7L,
                new ActualizarSuscripcionSolicitud(PlanSuscripcion.BASICO, EstadoSuscripcion.ACTIVA, inicio, fin));
        assertThat(activa.vigente()).isTrue();
        assertThat(activa.plan()).isEqualTo(PlanSuscripcion.BASICO);
    }

    @Test
    void noCreaSuscripcionParaOrganizacionInexistente() {
        when(organizaciones.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(99L,
                new ActualizarSuscripcionSolicitud(PlanSuscripcion.BASICO, EstadoSuscripcion.ACTIVA,
                        OffsetDateTime.now(), null)))
                .isInstanceOf(ExcepcionNoEncontrado.class);
    }
}
