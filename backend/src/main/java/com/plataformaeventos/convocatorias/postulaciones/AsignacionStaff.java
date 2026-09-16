package com.plataformaeventos.convocatorias.postulaciones;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

/** Registro del equipo del evento asociado a una postulación seleccionada. */
@Entity
@Table(name = "asignacion_staff", uniqueConstraints = @UniqueConstraint(
        name = "uk_asignacion_staff_postulacion", columnNames = "id_postulacion"
))
public class AsignacionStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignacion")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_postulacion", nullable = false, unique = true)
    private PostulacionStaff postulacion;

    @Column(name = "fecha_asignacion", nullable = false)
    private OffsetDateTime fechaAsignacion;

    protected AsignacionStaff() {
    }

    public AsignacionStaff(PostulacionStaff postulacion) {
        this.postulacion = postulacion;
    }

    @PrePersist
    void prePersist() {
        if (fechaAsignacion == null) {
            fechaAsignacion = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public PostulacionStaff getPostulacion() {
        return postulacion;
    }

    public OffsetDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }
}
