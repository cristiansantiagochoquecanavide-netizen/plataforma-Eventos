package com.plataformaeventos.certificados.modelo;

import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "certificado", uniqueConstraints = {
        @UniqueConstraint(name = "uq_certificado_inscripcion", columnNames = "id_inscripcion"),
        @UniqueConstraint(name = "uq_certificado_codigo", columnNames = "codigo_verificacion")
})
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_certificado")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_inscripcion", nullable = false)
    private Inscripcion inscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organizacion", nullable = false)
    private Organizacion organizacion;

    @Column(name = "codigo_verificacion", nullable = false, length = 64)
    private String codigoVerificacion;

    @Column(name = "hash_seguridad", nullable = false, length = 128)
    private String hashSeguridad;

    @Column(name = "fecha_emision", nullable = false)
    private OffsetDateTime fechaEmision;

    protected Certificado() {}

    public Certificado(Inscripcion inscripcion, Organizacion organizacion, String codigoVerificacion, String hashSeguridad) {
        this.inscripcion = inscripcion;
        this.organizacion = organizacion;
        this.codigoVerificacion = codigoVerificacion;
        this.hashSeguridad = hashSeguridad;
        this.fechaEmision = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Inscripcion getInscripcion() {
        return inscripcion;
    }

    public Organizacion getOrganizacion() {
        return organizacion;
    }

    public String getCodigoVerificacion() {
        return codigoVerificacion;
    }

    public String getHashSeguridad() {
        return hashSeguridad;
    }

    public OffsetDateTime getFechaEmision() {
        return fechaEmision;
    }
}
