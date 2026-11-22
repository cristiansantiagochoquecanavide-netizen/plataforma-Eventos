package com.plataformaeventos.recursos.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.plataformaeventos.organizaciones.modelo.Organizacion;

import java.time.OffsetDateTime;

@Entity
@Table(name = "recurso")
public class Recurso {

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "id_organizacion")
    private Organizacion organizacion;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recurso")
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoRecurso tipo;

    @Column(name = "cantidad_total", nullable = false)
    private Integer cantidadTotal;

    @Column(name = "cantidad_disponible", nullable = false)
    private Integer cantidadDisponible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecurso estado;

    @Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal costoUnitario = java.math.BigDecimal.ZERO;

    @Column(length = 150)
    private String ubicacion;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    public Recurso() {
    }

    public Recurso(
            String codigo,
            String nombre,
            TipoRecurso tipo,
            Integer cantidadTotal,
            Integer cantidadDisponible,
            EstadoRecurso estado,
            String ubicacion,
            String descripcion
    ) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.cantidadTotal = cantidadTotal;
        this.cantidadDisponible = cantidadDisponible;
        this.estado = estado;
        this.ubicacion = ubicacion;
        this.descripcion = descripcion;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        fechaActualizacion = ahora;
        if (estado == null) {
            estado = EstadoRecurso.DISPONIBLE;
        }
        if (cantidadDisponible == null) {
            cantidadDisponible = cantidadTotal;
        }
    }

    @PreUpdate
    void preUpdate() {
        fechaActualizacion = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }
    public Organizacion getOrganizacion() { return organizacion; }
    public void asignarOrganizacion(Organizacion organizacion) { this.organizacion = organizacion; }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoRecurso getTipo() {
        return tipo;
    }

    public void setTipo(TipoRecurso tipo) {
        this.tipo = tipo;
    }

    public Integer getCantidadTotal() {
        return cantidadTotal;
    }

    public void setCantidadTotal(Integer cantidadTotal) {
        this.cantidadTotal = cantidadTotal;
    }

    public Integer getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void setCantidadDisponible(Integer cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
    }

    public EstadoRecurso getEstado() {
        return estado;
    }

    public void setEstado(EstadoRecurso estado) {
        this.estado = estado;
    }

    public java.math.BigDecimal getCostoUnitario() {
        return costoUnitario != null ? costoUnitario : java.math.BigDecimal.ZERO;
    }

    public void setCostoUnitario(java.math.BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario != null ? costoUnitario : java.math.BigDecimal.ZERO;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
