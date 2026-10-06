package com.plataformaeventos.saas;

import com.plataformaeventos.saas.modelo.EstadoSolicitudAltaSaas;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import com.plataformaeventos.saas.modelo.SolicitudAltaSaas;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitudAltaSaasTest {
    @Test
    void mantieneVerificacionDuranteSieteDiasParaReintentarCheckoutCancelado() {
        OffsetDateTime ahora = OffsetDateTime.now();
        String tokenHash = "a".repeat(64);
        SolicitudAltaSaas alta = new SolicitudAltaSaas(UUID.randomUUID(), "Organización", null,
                "Ana", "Pérez", "ana@example.bo", PlanSuscripcion.PROFESIONAL, tokenHash,
                ahora.plusMinutes(30));

        alta.verificarEmail(ahora);
        alta.checkoutCreado("cs_primero", "https://checkout.stripe.test/1", ahora);
        alta.checkoutCancelado(ahora.plusMinutes(2));

        assertThat(alta.getEstado()).isEqualTo(EstadoSolicitudAltaSaas.CANCELADA);
        assertThat(alta.getTokenVerificacionHash()).isEqualTo(tokenHash);
        assertThat(alta.getTokenVerificacionExpira()).isEqualTo(ahora.plusDays(7));
        assertThat(alta.tokenVerificacionVigente(ahora.plusDays(6))).isTrue();

        alta.checkoutCreado("cs_reintento", "https://checkout.stripe.test/2", ahora.plusDays(6));
        assertThat(alta.getEstado()).isEqualTo(EstadoSolicitudAltaSaas.CHECKOUT_PENDIENTE);
        assertThat(alta.getStripeCheckoutSessionId()).isEqualTo("cs_reintento");
    }
}
