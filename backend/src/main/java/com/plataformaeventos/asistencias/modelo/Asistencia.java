package com.plataformaeventos.asistencias.modelo;

import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;

@Entity
@EntityListeners(com.plataformaeventos.aforo.CambiosAforoListener.class)
@Table(name = "asistencia", uniqueConstraints =
        @UniqueConstraint(name = "uq_asistencia_inscripcion", columnNames = "id_inscripcion"))
public class Asistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asistencia")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_inscripcion", nullable = false)
    private Inscripcion inscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organizacion", nullable = false)
    private Organizacion organizacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_staff", nullable = false)
    private Usuario registradoPor;

    @Column(name = "fecha_hora", nullable = false, insertable = false, updatable = false)
    @Generated(event = EventType.INSERT)
    private OffsetDateTime fechaHora;

    protected Asistencia() {}

    public Asistencia(Inscripcion inscripcion, Organizacion organizacion, Usuario registradoPor) {
        this.inscripcion = inscripcion;
        this.organizacion = organizacion;
        this.registradoPor = registradoPor;
    }

    public Long getId() { return id; }
    public Inscripcion getInscripcion() { return inscripcion; }
    public Organizacion getOrganizacion() { return organizacion; }
    public Usuario getRegistradoPor() { return registradoPor; }
    public OffsetDateTime getFechaHora() { return fechaHora; }
}
