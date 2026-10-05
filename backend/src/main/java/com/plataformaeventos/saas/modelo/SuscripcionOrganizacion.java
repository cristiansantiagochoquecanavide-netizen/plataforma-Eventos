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

    public void actualizar(PlanSuscripcion plan, EstadoSuscripcion estado,
                           OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {
        this.plan = plan;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
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

    public boolean estaVigente(OffsetDateTime ahora) {
        return estado == EstadoSuscripcion.ACTIVA
                && (fechaInicio == null || !fechaInicio.isAfter(ahora))
                && (fechaFin == null || fechaFin.isAfter(ahora));
    }
}
