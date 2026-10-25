package com.plataformaeventos.saas.servicio;

import com.plataformaeventos.saas.configuracion.StripeSaasProperties;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StripeCheckoutGatewayImpl implements StripeCheckoutGateway {
    private static final String CURRENCY_BOB = "bob";

    private final StripeSaasProperties properties;
    private final CatalogoPlanesSaasService catalogo;

    public StripeCheckoutGatewayImpl(StripeSaasProperties properties, CatalogoPlanesSaasService catalogo) {
        this.properties = properties;
        this.catalogo = catalogo;
    }

    @Override
    public StripeCheckoutResultado crearSesion(UUID altaId, String correo, PlanSuscripcion plan) {
        if (!properties.checkoutReady()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El checkout de Stripe no está configurado para este entorno");
        }
        if (plan == PlanSuscripcion.BASICO) {
            throw new IllegalArgumentException("El plan gratuito no necesita Checkout de Stripe");
        }
        String successUrl = properties.successUrl() == null || properties.successUrl().isBlank()
                ? properties.frontendUrl() + "/pago/completado?session_id={CHECKOUT_SESSION_ID}"
                : properties.successUrl();
        String cancelUrl = properties.cancelUrl() == null || properties.cancelUrl().isBlank()
                ? properties.frontendUrl() + "/planes?checkout=cancelado"
                : properties.cancelUrl();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(altaId.toString())
                .setCustomerEmail(correo)
                .putMetadata("alta_saas_id", altaId.toString())
                .putMetadata("plan", plan.name())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(CURRENCY_BOB)
                                .setUnitAmount(catalogo.importeMinor(plan))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Plataforma Eventos - " + catalogo.obtener(plan).nombre())
                                        .build())
                                .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                        .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                        .build())
                                .build())
                        .setQuantity(1L)
                        .build())
                .build();

        try {
            Session session = new StripeClient(properties.secretKey())
                    .v1().checkout().sessions().create(params);
            if (session.getId() == null || session.getUrl() == null) {
                throw new IllegalStateException("Stripe no devolvió una sesión de checkout válida");
            }
            return new StripeCheckoutResultado(session.getId(), session.getUrl());
        } catch (StripeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Stripe no pudo crear la sesión de pago. Verifica precios, moneda BOB y modo test.", exception);
        }
    }
}
