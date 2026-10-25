package com.plataformaeventos.saas.modelo;

import com.plataformaeventos.organizaciones.modelo.Organizacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(name = "suscripcion_organizacion", uniqueConstraints = @UniqueConstraint(
        name = "uk_suscripcion_organizacion", columnNames = "id_organizacion"))
public class SuscripcionOrganizacion {
    @Id
    @Column(name = "id_organizacion")
    private Long idOrganizacion;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organizacion", nullable = false)
    private Organizacion organizacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PlanSuscripcion plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSuscripcion estado;

    @Column(name = "fecha_inicio", nullable = false)
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private OffsetDateTime fechaFin;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 255)
    private String stripeSubscriptionId;

    @Column(name = "ciclo_facturacion", nullable = false, length = 16)
    private String cicloFacturacion = "MENSUAL";

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = "BOB";

    @Column(name = "importe_mensual_minor", nullable = false)
    private Long importeMensualMinor = 0L;

    @Column(name = "stripe_ultimo_evento_en")
    private OffsetDateTime stripeUltimoEventoEn;

    protected SuscripcionOrganizacion() { }

    public SuscripcionOrganizacion(Organizacion organizacion, PlanSuscripcion plan,
                                   EstadoSuscripcion estado, OffsetDateTime fechaInicio,
                                   OffsetDateTime fechaFin) {
        this.organizacion = organizacion;
        this.plan = plan;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.fechaActualizacion = OffsetDateTime.now();
    }

    public void vincularStripe(String customerId, String subscriptionId, long importeMinor, OffsetDateTime eventoEn) {
        this.stripeCustomerId = customerId;
        this.stripeSubscriptionId = subscriptionId;
        this.importeMensualMinor = importeMinor;
        this.cicloFacturacion = "MENSUAL";
        this.moneda = "BOB";
        this.stripeUltimoEventoEn = eventoEn;
        this.fechaActualizacion = OffsetDateTime.now();
    }

    public void actualizarDesdeStripe(EstadoSuscripcion estado, OffsetDateTime fechaInicio,
                                      OffsetDateTime fechaFin, OffsetDateTime eventoEn) {
        if (eventoEn != null && stripeUltimoEventoEn != null && eventoEn.isBefore(stripeUltimoEventoEn)) return;
        this.estado = estado;
        if (fechaInicio != null) this.fechaInicio = fechaInicio;
        if (fechaFin != null) this.fechaFin = fechaFin;
        if (eventoEn != null) this.stripeUltimoEventoEn = eventoEn;
        this.fechaActualizacion = OffsetDateTime.now();
    }

    public Long getIdOrganizacion() {
        return idOrganizacion != null ? idOrganizacion : organizacion == null ? null : organizacion.getId();
    }
    public Organizacion getOrganizacion() { return organizacion; }
    public PlanSuscripcion getPlan() { return plan; }
    public EstadoSuscripcion getEstado() { return estado; }
    public OffsetDateTime getFechaInicio() { return fechaInicio; }
    public OffsetDateTime getFechaFin() { return fechaFin; }
    public OffsetDateTime getFechaActualizacion() { return fechaActualizacion; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public String getCicloFacturacion() { return cicloFacturacion; }
    public String getMoneda() { return moneda; }
    public Long getImporteMensualMinor() { return importeMensualMinor; }
    public OffsetDateTime getStripeUltimoEventoEn() { return stripeUltimoEventoEn; }

    public boolean estaVigente(OffsetDateTime ahora) {
        return estado == EstadoSuscripcion.ACTIVA
                && (fechaInicio == null || !fechaInicio.isAfter(ahora))
                && (fechaFin == null || fechaFin.isAfter(ahora));
    }
}
