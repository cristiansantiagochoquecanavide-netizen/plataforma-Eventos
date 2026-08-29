package com.plataformaeventos.convocatorias;

import com.plataformaeventos.eventos.Evento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "convocatoria_personal")
public class Convocatoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_convocatoria")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_personal", nullable = false, length = 15)
    private TipoPersonal tipoPersonal;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(name = "cantidad_requerida", nullable = false)
    private Integer cantidadRequerida;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoConvocatoria estado;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected Convocatoria() {
    }

    public Convocatoria(
            Evento evento,
            TipoPersonal tipoPersonal,
            String descripcion,
            Integer cantidadRequerida,
            LocalDate fechaLimite
    ) {
        this.evento = evento;
        this.tipoPersonal = tipoPersonal;
        this.descripcion = descripcion;
        this.cantidadRequerida = cantidadRequerida;
        this.fechaLimite = fechaLimite;
        this.estado = EstadoConvocatoria.ABIERTA;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        fechaActualizacion = ahora;
        if (estado == null) {
            estado = EstadoConvocatoria.ABIERTA;
        }
    }

    @PreUpdate
    void preUpdate() {
        fechaActualizacion = OffsetDateTime.now();
    }

    public void cerrar() {
        validarAbierta("cerrar");
        estado = EstadoConvocatoria.CERRADA;
    }

    public void cancelar() {
        validarAbierta("cancelar");
        estado = EstadoConvocatoria.CANCELADA;
    }

    private void validarAbierta(String accion) {
        if (estado != EstadoConvocatoria.ABIERTA) {
            throw new IllegalArgumentException(
                    "Solo se puede " + accion + " una convocatoria abierta"
            );
        }
    }

    public Long getId() {
        return id;
    }

    public Evento getEvento() {
        return evento;
    }

    public TipoPersonal getTipoPersonal() {
        return tipoPersonal;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Integer getCantidadRequerida() {
        return cantidadRequerida;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public EstadoConvocatoria getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
