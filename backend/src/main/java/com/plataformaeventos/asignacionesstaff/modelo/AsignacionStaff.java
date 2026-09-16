package com.plataformaeventos.asignacionesstaff.modelo;

import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.postulacionesstaff.modelo.PostulacionStaff;
import com.plataformaeventos.usuarios.modelo.Usuario;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "asignacion_staff")
public class AsignacionStaff {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_asignacion") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_postulacion") private PostulacionStaff postulacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_organizacion") private Organizacion organizacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_usuario") private Usuario usuario;
    @Column(nullable = false) private String tarea;
    @Column(name = "turno_inicio") private OffsetDateTime turnoInicio;
    @Column(name = "turno_fin") private OffsetDateTime turnoFin;
    private String observaciones;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoAsignacionStaff estado = EstadoAsignacionStaff.ASIGNADA;
    @Column(name = "fecha_creacion", nullable = false) private OffsetDateTime fechaCreacion = OffsetDateTime.now();
    protected AsignacionStaff() {}
    public AsignacionStaff(PostulacionStaff p, Organizacion o, String t, OffsetDateTime i, OffsetDateTime f, String obs) { postulacion=p; organizacion=o; usuario=p.getUsuario(); tarea=t; turnoInicio=i; turnoFin=f; observaciones=obs; }
    public Long getId() { return id; }
    public PostulacionStaff getPostulacion() { return postulacion; }
    public String getTarea() { return tarea; }
    public OffsetDateTime getTurnoInicio() { return turnoInicio; }
    public OffsetDateTime getTurnoFin() { return turnoFin; }
    public EstadoAsignacionStaff getEstado() { return estado; }
    public void cancelar() { if (estado != EstadoAsignacionStaff.ASIGNADA) throw new IllegalStateException("La asignacion ya fue cancelada"); estado = EstadoAsignacionStaff.CANCELADA; }
}
