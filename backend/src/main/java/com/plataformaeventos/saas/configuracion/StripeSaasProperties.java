package com.plataformaeventos.saas.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "saas.stripe")
public record StripeSaasProperties(
        boolean enabled,
        String secretKey,
        String webhookSecret,
        String successUrl,
        String cancelUrl,
        String frontendUrl
) {
    public boolean checkoutReady() {
        return enabled && hasText(secretKey) && hasText(frontendUrl);
    }

    public boolean webhookReady() {
        return enabled && hasText(webhookSecret);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
