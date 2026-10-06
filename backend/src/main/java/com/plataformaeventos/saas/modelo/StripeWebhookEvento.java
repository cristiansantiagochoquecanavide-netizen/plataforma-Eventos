package com.plataformaeventos.saas.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "stripe_webhook_evento")
public class StripeWebhookEvento {
    @Id
    @Column(name = "stripe_event_id", length = 255)
    private String stripeEventId;

    @Column(nullable = false, length = 120)
    private String tipo;

    @Column(name = "fecha_recibido", nullable = false)
    private OffsetDateTime fechaRecibido;

    protected StripeWebhookEvento() { }

    public StripeWebhookEvento(String stripeEventId, String tipo) {
        this.stripeEventId = stripeEventId;
        this.tipo = tipo;
        this.fechaRecibido = OffsetDateTime.now();
    }

    public String getStripeEventId() { return stripeEventId; }
    public String getTipo() { return tipo; }
    public OffsetDateTime getFechaRecibido() { return fechaRecibido; }
}
