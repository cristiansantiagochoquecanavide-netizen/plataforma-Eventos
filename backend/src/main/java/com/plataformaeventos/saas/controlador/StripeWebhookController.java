package com.plataformaeventos.saas.controlador;

import com.plataformaeventos.saas.servicio.StripeWebhookService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saas/stripe")
public class StripeWebhookController {
    private final StripeWebhookService webhooks;

    public StripeWebhookController(StripeWebhookService webhooks) {
        this.webhooks = webhooks;
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> recibir(
            @RequestBody byte[] cuerpoOriginal,
            @RequestHeader(name = "Stripe-Signature", required = false) String firma
    ) {
        webhooks.procesar(cuerpoOriginal, firma);
        return ResponseEntity.ok().build();
    }
}
