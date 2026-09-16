package com.plataformaeventos.postulacionesstaff.modelo;
import com.plataformaeventos.convocatorias.modelo.Convocatoria;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity @Table(name="postulacion_staff", uniqueConstraints=@UniqueConstraint(name="uq_postulacion_usuario_convocatoria", columnNames={"id_convocatoria","id_usuario","id_organizacion"}))
public class PostulacionStaff {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_postulacion") private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="id_convocatoria") private Convocatoria convocatoria;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="id_usuario") private Usuario usuario;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="id_organizacion") private Organizacion organizacion;
 @Column(length=1000) private String motivacion;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private EstadoPostulacionStaff estado=EstadoPostulacionStaff.PENDIENTE;
 @Column(name="fecha_postulacion",nullable=false) private OffsetDateTime fechaPostulacion=OffsetDateTime.now();
 @Column(name="fecha_actualizacion",nullable=false) private OffsetDateTime fechaActualizacion=OffsetDateTime.now();
 protected PostulacionStaff() {}
 public PostulacionStaff(Convocatoria c, Usuario u, Organizacion o, String m) { convocatoria=c; usuario=u; organizacion=o; motivacion=m; }
 public Long getId(){return id;} public Convocatoria getConvocatoria(){return convocatoria;} public Usuario getUsuario(){return usuario;} public EstadoPostulacionStaff getEstado(){return estado;} public String getMotivacion(){return motivacion;} public OffsetDateTime getFechaPostulacion(){return fechaPostulacion;}
  public void aceptar(){transicionar(EstadoPostulacionStaff.ACEPTADA);} public void rechazar(){transicionar(EstadoPostulacionStaff.RECHAZADA);}
  private void transicionar(EstadoPostulacionStaff nuevo){if(estado!=EstadoPostulacionStaff.PENDIENTE)throw new IllegalStateException("La postulacion ya fue resuelta");estado=nuevo;fechaActualizacion=OffsetDateTime.now();}
}
