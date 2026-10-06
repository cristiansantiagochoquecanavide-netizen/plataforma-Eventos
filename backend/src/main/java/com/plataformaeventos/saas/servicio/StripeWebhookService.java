package com.plataformaeventos.saas.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plataformaeventos.saas.configuracion.StripeSaasProperties;
import com.plataformaeventos.saas.modelo.StripeWebhookEvento;
import com.plataformaeventos.saas.repositorio.StripeWebhookEventoRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StripeWebhookService {
    private final StripeSaasProperties properties;
    private final ObjectMapper objectMapper;
    private final StripeWebhookEventoRepository eventos;
    private final AltaSaasService altas;

    public StripeWebhookService(StripeSaasProperties properties, ObjectMapper objectMapper,
                                StripeWebhookEventoRepository eventos, AltaSaasService altas) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.eventos = eventos;
        this.altas = altas;
    }

    @Transactional
    public void procesar(byte[] cuerpoOriginal, String firma) {
        if (!properties.webhookReady()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook de Stripe no configurado");
        }
        if (firma == null || firma.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la firma de Stripe");
        }

        String payload = new String(cuerpoOriginal, StandardCharsets.UTF_8);
        Event evento;
        JsonNode raiz;
        try {
            evento = Webhook.constructEvent(payload, firma, properties.webhookSecret());
            raiz = objectMapper.readTree(cuerpoOriginal);
        } catch (SignatureVerificationException | IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firma o cuerpo del webhook de Stripe inválido", e);
        }

        if (eventos.existsById(evento.getId())) return;
        eventos.saveAndFlush(new StripeWebhookEvento(evento.getId(), evento.getType()));
        JsonNode objeto = raiz.path("data").path("object");
        OffsetDateTime eventoStripeEn = fechaEvento(raiz);

        switch (evento.getType()) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" ->
                    procesarCheckout(objeto, eventoStripeEn);
            case "checkout.session.expired", "checkout.session.async_payment_failed" ->
                    altas.checkoutExpirado(texto(objeto, "id"));
            case "invoice.paid" -> {
                String subscriptionId = idReferencia(objeto.path("subscription"));
                if (subscriptionId != null) {
                    altas.procesarRenovacionPagada(subscriptionId, finPeriodoFactura(objeto), eventoStripeEn);
                }
            }
            case "invoice.payment_failed" -> {
                String subscriptionId = idReferencia(objeto.path("subscription"));
                if (subscriptionId != null) altas.procesarPagoFallido(subscriptionId, eventoStripeEn);
            }
            case "customer.subscription.updated" -> procesarCambioSuscripcion(objeto, eventoStripeEn);
            case "customer.subscription.deleted" -> {
                String subscriptionId = texto(objeto, "id");
                if (subscriptionId != null) altas.procesarSuscripcionEliminada(subscriptionId, eventoStripeEn);
            }
            default -> { /* Evento válido de Stripe que no afecta al acceso SaaS. */ }
        }
    }

    private void procesarCheckout(JsonNode session, OffsetDateTime eventoStripeEn) {
        if (!"subscription".equals(texto(session, "mode")) || !"paid".equals(texto(session, "payment_status"))) {
            return;
        }
        String altaId = texto(session.path("metadata"), "alta_saas_id");
        String referenceId = texto(session, "client_reference_id");
        if (altaId == null || !altaId.equals(referenceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metadata del checkout de Stripe inválida");
        }
        try {
            altas.confirmarPagoStripe(
                    java.util.UUID.fromString(altaId),
                    texto(session, "id"),
                    idReferencia(session.path("customer")),
                    idReferencia(session.path("subscription")),
                    texto(session, "currency"),
                    session.path("amount_total").isNumber() ? session.path("amount_total").longValue() : null,
                    eventoStripeEn
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referencia de alta inválida", e);
        }
    }

    private void procesarCambioSuscripcion(JsonNode subscription, OffsetDateTime eventoStripeEn) {
        String stripeSubscriptionId = texto(subscription, "id");
        String estado = texto(subscription, "status");
        if (stripeSubscriptionId == null || estado == null) return;
        if ("active".equals(estado)) {
            altas.procesarRenovacionPagada(stripeSubscriptionId, finPeriodoSuscripcion(subscription), eventoStripeEn);
        } else if ("past_due".equals(estado) || "unpaid".equals(estado)) {
            altas.procesarPagoFallido(stripeSubscriptionId, eventoStripeEn);
        } else if ("canceled".equals(estado) || "incomplete_expired".equals(estado)) {
            altas.procesarSuscripcionEliminada(stripeSubscriptionId, eventoStripeEn);
        }
    }

    private OffsetDateTime fechaEvento(JsonNode event) {
        long created = event.path("created").asLong(0L);
        return created > 0 ? OffsetDateTime.ofInstant(Instant.ofEpochSecond(created), ZoneOffset.UTC) : null;
    }

    private OffsetDateTime finPeriodoFactura(JsonNode invoice) {
        JsonNode lines = invoice.path("lines").path("data");
        if (lines.isArray() && !lines.isEmpty()) {
            long end = lines.get(0).path("period").path("end").asLong(0L);
            if (end > 0) return OffsetDateTime.ofInstant(Instant.ofEpochSecond(end), ZoneOffset.UTC);
        }
        return null;
    }

    private OffsetDateTime finPeriodoSuscripcion(JsonNode subscription) {
        long end = subscription.path("current_period_end").asLong(0L);
        return end > 0 ? OffsetDateTime.ofInstant(Instant.ofEpochSecond(end), ZoneOffset.UTC) : null;
    }

    private String idReferencia(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) return null;
        return value.isTextual() ? value.asText() : texto(value, "id");
    }

    private String texto(JsonNode node, String campo) {
        JsonNode value = node.path(campo);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }
}
