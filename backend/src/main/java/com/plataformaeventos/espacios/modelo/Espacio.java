package com.plataformaeventos.espacios.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.plataformaeventos.organizaciones.modelo.Organizacion;

import java.time.OffsetDateTime;

@Entity
@jakarta.persistence.EntityListeners(com.plataformaeventos.aforo.CambiosAforoListener.class)
@Table(name = "espacio")
public class Espacio {

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "id_organizacion")
    private Organizacion organizacion;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_espacio")
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String ubicacion;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(name = "costo_alquiler", nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal costoAlquiler = java.math.BigDecimal.ZERO;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected Espacio() {
    }

    public Espacio(String nombre, String ubicacion, Integer capacidad) {
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.capacidad = capacidad;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (fechaCreacion == null) fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void preUpdate() {
        fechaActualizacion = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Organizacion getOrganizacion() { return organizacion; }
    public void asignarOrganizacion(Organizacion organizacion) { this.organizacion = organizacion; }
    public String getNombre() { return nombre; }
    public String getUbicacion() { return ubicacion; }
    public Integer getCapacidad() { return capacidad; }
    public java.math.BigDecimal getCostoAlquiler() { return costoAlquiler != null ? costoAlquiler : java.math.BigDecimal.ZERO; }
    public void setCostoAlquiler(java.math.BigDecimal costoAlquiler) { this.costoAlquiler = costoAlquiler != null ? costoAlquiler : java.math.BigDecimal.ZERO; }

    public void actualizarDatos(String nombre, String ubicacion, Integer capacidad) {
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.capacidad = capacidad;
    }
}
