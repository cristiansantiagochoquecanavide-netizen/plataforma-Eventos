package com.plataformaeventos.saas;

import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.saas.modelo.EstadoSuscripcion;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import com.plataformaeventos.saas.modelo.SuscripcionOrganizacion;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SuscripcionOrganizacionWebhookTest {
    @Test
    void ignoraEventosStripeMasAntiguosQueElUltimoEstadoAplicado() {
        Organizacion organizacion = new Organizacion("Tenant", "tenant");
        OffsetDateTime inicio = OffsetDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime eventoNuevo = inicio.plusDays(4);
        OffsetDateTime eventoAntiguo = inicio.plusDays(3);
        SuscripcionOrganizacion suscripcion = new SuscripcionOrganizacion(
                organizacion, PlanSuscripcion.PROFESIONAL, EstadoSuscripcion.ACTIVA, inicio, inicio.plusMonths(1));
        suscripcion.vincularStripe("cus_123", "sub_123", 39900L, eventoNuevo);

        suscripcion.actualizarDesdeStripe(EstadoSuscripcion.SUSPENDIDA, inicio, inicio.plusMonths(1), eventoAntiguo);

        assertThat(suscripcion.getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(suscripcion.getStripeUltimoEventoEn()).isEqualTo(eventoNuevo);
    }
}
