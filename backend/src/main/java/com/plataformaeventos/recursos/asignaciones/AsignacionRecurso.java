package com.plataformaeventos.recursos.asignaciones;

import com.plataformaeventos.eventos.Evento;
import com.plataformaeventos.recursos.Recurso;
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
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "asignacion_recurso")
public class AsignacionRecurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_recurso", nullable = false)
    private Recurso recurso;

    @Column(name = "sala_espacio", nullable = false, length = 100)
    private String salaEspacio;

    @Column(name = "cantidad_asignada", nullable = false)
    private Integer cantidadAsignada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsignacion estado;

    @Column(length = 255)
    private String observaciones;

    @Column(name = "fecha_asignacion", nullable = false)
    private OffsetDateTime fechaAsignacion;

    @Column(name = "fecha_devolucion")
    private OffsetDateTime fechaDevolucion;

    public AsignacionRecurso() {
    }

    public AsignacionRecurso(
            Evento evento,
            Recurso recurso,
            String salaEspacio,
            Integer cantidadAsignada,
            EstadoAsignacion estado,
            String observaciones
    ) {
        this.evento = evento;
        this.recurso = recurso;
        this.salaEspacio = salaEspacio;
        this.cantidadAsignada = cantidadAsignada;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    @PrePersist
    void prePersist() {
        if (fechaAsignacion == null) {
            fechaAsignacion = OffsetDateTime.now();
        }
        if (estado == null) {
            estado = EstadoAsignacion.ACTIVA;
        }
    }

    public Long getId() {
        return id;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public void setRecurso(Recurso recurso) {
        this.recurso = recurso;
    }

    public String getSalaEspacio() {
        return salaEspacio;
    }

    public void setSalaEspacio(String salaEspacio) {
        this.salaEspacio = salaEspacio;
    }

    public Integer getCantidadAsignada() {
        return cantidadAsignada;
    }

    public void setCantidadAsignada(Integer cantidadAsignada) {
        this.cantidadAsignada = cantidadAsignada;
    }

    public EstadoAsignacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoAsignacion estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public OffsetDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public OffsetDateTime getFechaDevolucion() {
        return fechaDevolucion;
    }

    public void setFechaDevolucion(OffsetDateTime fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }
}
