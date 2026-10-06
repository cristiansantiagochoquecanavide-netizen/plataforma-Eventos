package com.plataformaeventos.saas.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "solicitud_alta_saas")
public class SolicitudAltaSaas {
    @Id
    private UUID id;

    @Column(name = "nombre_empresa", nullable = false, length = 150)
    private String nombreEmpresa;

    @Column(name = "slug_sugerido", length = 100)
    private String slugSugerido;

    @Column(name = "nombres_contacto", nullable = false, length = 100)
    private String nombresContacto;

    @Column(name = "apellidos_contacto", nullable = false, length = 100)
    private String apellidosContacto;

    @Column(name = "correo_contacto", nullable = false, length = 254)
    private String correoContacto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PlanSuscripcion plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EstadoSolicitudAltaSaas estado;

    @Column(name = "token_verificacion_hash", nullable = false, unique = true, length = 64)
    private String tokenVerificacionHash;

    @Column(name = "token_verificacion_expira", nullable = false)
    private OffsetDateTime tokenVerificacionExpira;

    @Column(name = "fecha_email_verificado")
    private OffsetDateTime fechaEmailVerificado;

    @Column(name = "stripe_checkout_session_id", unique = true)
    private String stripeCheckoutSessionId;

    @Column(name = "stripe_checkout_url", columnDefinition = "TEXT")
    private String stripeCheckoutUrl;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "fecha_pago_confirmado")
    private OffsetDateTime fechaPagoConfirmado;

    @Column(name = "id_organizacion")
    private Long idOrganizacion;

    @Column(name = "id_usuario_propietario")
    private Long idUsuarioPropietario;

    @Column(name = "token_activacion_hash", unique = true, length = 64)
    private String tokenActivacionHash;

    @Column(name = "token_activacion_expira")
    private OffsetDateTime tokenActivacionExpira;

    @Column(name = "fecha_activacion")
    private OffsetDateTime fechaActivacion;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected SolicitudAltaSaas() { }

    public SolicitudAltaSaas(UUID id, String nombreEmpresa, String slugSugerido, String nombresContacto,
                             String apellidosContacto, String correoContacto, PlanSuscripcion plan,
                             String tokenVerificacionHash, OffsetDateTime tokenVerificacionExpira) {
        this.id = id;
        this.nombreEmpresa = nombreEmpresa;
        this.slugSugerido = slugSugerido;
        this.nombresContacto = nombresContacto;
        this.apellidosContacto = apellidosContacto;
        this.correoContacto = correoContacto;
        this.plan = plan;
        this.estado = EstadoSolicitudAltaSaas.EMAIL_PENDIENTE;
        this.tokenVerificacionHash = tokenVerificacionHash;
        this.tokenVerificacionExpira = tokenVerificacionExpira;
        this.fechaCreacion = OffsetDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    public void verificarEmail(OffsetDateTime ahora) {
        this.fechaEmailVerificado = ahora;
        this.fechaActualizacion = ahora;
    }

    public void checkoutCreado(String sessionId, String checkoutUrl, OffsetDateTime ahora) {
        this.stripeCheckoutSessionId = sessionId;
        this.stripeCheckoutUrl = checkoutUrl;
        this.estado = EstadoSolicitudAltaSaas.CHECKOUT_PENDIENTE;
        this.tokenVerificacionExpira = ahora.plusDays(7);
        this.fechaActualizacion = ahora;
    }

    public void checkoutCancelado(OffsetDateTime ahora) {
        if (estado == EstadoSolicitudAltaSaas.CHECKOUT_PENDIENTE) {
            this.estado = EstadoSolicitudAltaSaas.CANCELADA;
            this.fechaActualizacion = ahora;
        }
    }

    public void pagoConfirmado(String customerId, String subscriptionId, OffsetDateTime ahora) {
        this.stripeCustomerId = customerId;
        this.stripeSubscriptionId = subscriptionId;
        this.fechaPagoConfirmado = ahora;
        this.tokenVerificacionHash = null;
        this.tokenVerificacionExpira = null;
        this.fechaActualizacion = ahora;
    }

    public void tenantProvisionado(Long organizacionId, Long propietarioId, String tokenActivacionHash,
                                   OffsetDateTime tokenActivacionExpira, boolean requiereActivacion,
                                   OffsetDateTime ahora) {
        this.idOrganizacion = organizacionId;
        this.idUsuarioPropietario = propietarioId;
        this.tokenVerificacionHash = null;
        this.tokenVerificacionExpira = null;
        this.tokenActivacionHash = tokenActivacionHash;
        this.tokenActivacionExpira = tokenActivacionExpira;
        this.estado = requiereActivacion ? EstadoSolicitudAltaSaas.ACTIVACION_PENDIENTE : EstadoSolicitudAltaSaas.ACTIVA;
        this.fechaActualizacion = ahora;
    }

    public void activar(OffsetDateTime ahora) {
        this.tokenActivacionHash = null;
        this.tokenActivacionExpira = null;
        this.fechaActivacion = ahora;
        this.estado = EstadoSolicitudAltaSaas.ACTIVA;
        this.fechaActualizacion = ahora;
    }

    public void marcarPagoFallido(OffsetDateTime ahora) {
        if (estado == EstadoSolicitudAltaSaas.CHECKOUT_PENDIENTE) {
            this.estado = EstadoSolicitudAltaSaas.PAGO_FALLIDO;
            this.fechaActualizacion = ahora;
        }
    }

    public boolean tokenVerificacionVigente(OffsetDateTime ahora) {
        return (estado == EstadoSolicitudAltaSaas.EMAIL_PENDIENTE
                || estado == EstadoSolicitudAltaSaas.CHECKOUT_PENDIENTE
                || estado == EstadoSolicitudAltaSaas.CANCELADA
                || estado == EstadoSolicitudAltaSaas.PAGO_FALLIDO)
                && tokenVerificacionExpira != null && tokenVerificacionExpira.isAfter(ahora);
    }

    public boolean tokenActivacionVigente(OffsetDateTime ahora) {
        return estado == EstadoSolicitudAltaSaas.ACTIVACION_PENDIENTE
                && tokenActivacionExpira != null && tokenActivacionExpira.isAfter(ahora);
    }

    @PreUpdate
    void actualizarFechaActualizacion() {
        fechaActualizacion = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getNombreEmpresa() { return nombreEmpresa; }
    public String getSlugSugerido() { return slugSugerido; }
    public String getNombresContacto() { return nombresContacto; }
    public String getApellidosContacto() { return apellidosContacto; }
    public String getCorreoContacto() { return correoContacto; }
    public PlanSuscripcion getPlan() { return plan; }
    public EstadoSolicitudAltaSaas getEstado() { return estado; }
    public String getTokenVerificacionHash() { return tokenVerificacionHash; }
    public OffsetDateTime getTokenVerificacionExpira() { return tokenVerificacionExpira; }
    public String getStripeCheckoutSessionId() { return stripeCheckoutSessionId; }
    public String getStripeCheckoutUrl() { return stripeCheckoutUrl; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public OffsetDateTime getFechaPagoConfirmado() { return fechaPagoConfirmado; }
    public OffsetDateTime getFechaEmailVerificado() { return fechaEmailVerificado; }
    public Long getIdOrganizacion() { return idOrganizacion; }
    public Long getIdUsuarioPropietario() { return idUsuarioPropietario; }
    public String getTokenActivacionHash() { return tokenActivacionHash; }
    public OffsetDateTime getTokenActivacionExpira() { return tokenActivacionExpira; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
}
