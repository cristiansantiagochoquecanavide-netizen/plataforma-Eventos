package com.plataformaeventos.organizaciones.modelo;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "organizacion")
public class Organizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_organizacion") private Long id;
    @Column(nullable = false, length = 150) private String nombre;
    @Column(nullable = false, unique = true, length = 100) private String slug;
    @Column(nullable = false, length = 20) private String estado = "ACTIVA";
    @Column(name = "fecha_creacion", nullable = false) private OffsetDateTime fechaCreacion;
    protected Organizacion() {}
    public Organizacion(String nombre, String slug) { this.nombre = nombre; this.slug = slug; }
    @PrePersist void fechas() { if (fechaCreacion == null) fechaCreacion = OffsetDateTime.now(); }
    public Long getId() { return id; } public String getNombre() { return nombre; } public String getSlug() { return slug; } public String getEstado() { return estado; }
}
