package com.plataformaeventos.convocatorias.postulaciones;

import com.plataformaeventos.convocatorias.Convocatoria;
import com.plataformaeventos.usuarios.Usuario;
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
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(name = "postulacion_staff", uniqueConstraints = @UniqueConstraint(
        name = "uk_postulacion_convocatoria_usuario", columnNames = {"id_convocatoria", "id_usuario"}
))
public class PostulacionStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postulacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_convocatoria", nullable = false)
    private Convocatoria convocatoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoPostulacion estado;

    @Column(name = "fecha_postulacion", nullable = false)
    private OffsetDateTime fechaPostulacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    protected PostulacionStaff() {
    }

    public PostulacionStaff(Usuario usuario, Convocatoria convocatoria) {
        this.usuario = usuario;
        this.convocatoria = convocatoria;
        this.estado = EstadoPostulacion.PENDIENTE;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime ahora = OffsetDateTime.now();
        fechaPostulacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void preUpdate() {
        fechaActualizacion = OffsetDateTime.now();
    }

    public void retirar() {
        if (estado != EstadoPostulacion.PENDIENTE) {
            throw new IllegalArgumentException("Solo se puede retirar una postulacion pendiente");
        }
        estado = EstadoPostulacion.RETIRADA;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Convocatoria getConvocatoria() {
        return convocatoria;
    }

    public EstadoPostulacion getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaPostulacion() {
        return fechaPostulacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
