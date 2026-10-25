package com.plataformaeventos.saas;

import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.saas.modelo.EstadoSuscripcion;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import com.plataformaeventos.saas.modelo.SuscripcionOrganizacion;
import com.plataformaeventos.saas.repositorio.SuscripcionOrganizacionRepository;
import com.plataformaeventos.saas.servicio.SuscripcionOrganizacionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuscripcionOrganizacionServiceTest {
    private final SuscripcionOrganizacionRepository suscripciones = mock(SuscripcionOrganizacionRepository.class);
    private final SuscripcionOrganizacionService servicio =
            new SuscripcionOrganizacionService(suscripciones);

    @Test
    void creaPlanBaseGratisCuandoNoExisteSuscripcion() {
        Organizacion organizacion = new Organizacion("Tenant", "tenant");
        ReflectionTestUtils.setField(organizacion, "id", 7L);
        when(suscripciones.existsById(7L)).thenReturn(false);

        servicio.crearPlanBaseSiFalta(organizacion);

        var captor = org.mockito.ArgumentCaptor.forClass(SuscripcionOrganizacion.class);
        verify(suscripciones).save(captor.capture());
        assertThat(captor.getValue().getPlan()).isEqualTo(PlanSuscripcion.BASICO);
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
    }

    @Test
    void noSobrescribeSuscripcionExistenteAdministradaPorStripe() {
        Organizacion organizacion = new Organizacion("Tenant", "tenant");
        ReflectionTestUtils.setField(organizacion, "id", 7L);
        when(suscripciones.existsById(7L)).thenReturn(true);

        servicio.crearPlanBaseSiFalta(organizacion);

        verify(suscripciones, never()).save(any(SuscripcionOrganizacion.class));
    }
}
