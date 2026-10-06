package com.plataformaeventos.saas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plataformaeventos.saas.configuracion.StripeSaasProperties;
import com.plataformaeventos.saas.repositorio.StripeWebhookEventoRepository;
import com.plataformaeventos.saas.servicio.AltaSaasService;
import com.plataformaeventos.saas.servicio.StripeWebhookService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StripeWebhookServiceTest {
    private static final String SECRET = "whsec_test_secret";
    private final StripeWebhookEventoRepository eventos = mock(StripeWebhookEventoRepository.class);
    private final AltaSaasService altas = mock(AltaSaasService.class);
    private final StripeWebhookService servicio = new StripeWebhookService(
            new StripeSaasProperties(true, "sk_test", SECRET, null, null, "http://localhost:4200"),
            new ObjectMapper(), eventos, altas);

    @Test
    void rechazaFirmaInvalidaAntesDePersistirElEvento() {
        byte[] payload = evento("evt_bad", "customer.updated").getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> servicio.procesar(payload, "t=1,v1=invalid"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));

        verify(eventos, never()).saveAndFlush(any());
    }

    @Test
    void persisteEventoValidoUnaSolaVez() throws Exception {
        String payload = evento("evt_valid", "customer.updated");
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        String signature = firmar(payload);
        when(eventos.existsById("evt_valid")).thenReturn(false, true);

        servicio.procesar(body, signature);
        servicio.procesar(body, signature);

        verify(eventos).saveAndFlush(any());
    }

    @Test
    void soloDespachaCheckoutDeSuscripcionPagadoConReferenciaCoincidente() throws Exception {
        UUID altaId = UUID.randomUUID();
        String payload = "{\"id\":\"evt_paid\",\"object\":\"event\",\"api_version\":\"2025-03-31.basil\","
                + "\"created\":1700000000,\"data\":{\"object\":{\"id\":\"cs_paid\","
                + "\"mode\":\"subscription\",\"payment_status\":\"paid\","
                + "\"client_reference_id\":\"" + altaId + "\",\"metadata\":{\"alta_saas_id\":\"" + altaId + "\"},"
                + "\"customer\":\"cus_123\",\"subscription\":\"sub_123\","
                + "\"currency\":\"bob\",\"amount_total\":39900}},"
                + "\"livemode\":false,\"pending_webhooks\":1,\"type\":\"checkout.session.completed\"}";
        when(eventos.existsById("evt_paid")).thenReturn(false);

        servicio.procesar(payload.getBytes(StandardCharsets.UTF_8), firmar(payload));

        verify(altas).confirmarPagoStripe(altaId, "cs_paid", "cus_123", "sub_123", "bob", 39900L,
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1700000000L), ZoneOffset.UTC));
    }

    private String evento(String id, String type) {
        return "{\"id\":\"" + id + "\",\"object\":\"event\",\"api_version\":\"2025-03-31.basil\","
                + "\"created\":1700000000,\"data\":{\"object\":{\"id\":\"obj_123\"}},"
                + "\"livemode\":false,\"pending_webhooks\":1,\"type\":\"" + type + "\"}";
    }

    private String firmar(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(digest);
    }
}
