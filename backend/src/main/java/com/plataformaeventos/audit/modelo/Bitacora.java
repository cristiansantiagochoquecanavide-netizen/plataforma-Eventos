package com.plataformaeventos.audit.modelo;

import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bitacora")
public class Bitacora {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bitacora") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_organizacion", nullable = false)
    private Organizacion organizacion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_actor") private Usuario actor;
    @Column(nullable = false) private String accion;
    @Column(nullable = false) private String modulo;
    @Column(nullable = false) private String entidad;
    @Column(name = "id_entidad") private Long idEntidad;
    @Column(nullable = false) private OffsetDateTime fecha = OffsetDateTime.now();
    @Column(length = 64) private String ip;
    @Column(nullable = false) private boolean exito;
    @Column(length = 1000) private String resultado;

    protected Bitacora() { }

    public Bitacora(Usuario actor, Organizacion organizacion, String accion, String modulo,
                    String entidad, Long idEntidad, String ip, boolean exito, String resultado) {
        this.actor = actor; this.organizacion = organizacion; this.accion = accion;
        this.modulo = modulo; this.entidad = entidad; this.idEntidad = idEntidad;
        this.ip = ip; this.exito = exito; this.resultado = resultado;
    }

    public Bitacora(Usuario actor, Organizacion organizacion, String accion, String modulo,
                    String entidad, Long idEntidad, boolean exito, String resultado) {
        this(actor, organizacion, accion, modulo, entidad, idEntidad, null, exito, resultado);
    }

    @PreUpdate @PreRemove
    private void impedirMutacion() { throw new IllegalStateException("La bitacora es inmutable"); }

    public Long getId() { return id; }
    public Organizacion getOrganizacion() { return organizacion; }
    public Usuario getActor() { return actor; }
    public String getAccion() { return accion; }
    public String getModulo() { return modulo; }
    public String getEntidad() { return entidad; }
    public Long getIdEntidad() { return idEntidad; }
    public OffsetDateTime getFecha() { return fecha; }
    public String getIp() { return ip; }
    public boolean isExito() { return exito; }
    public String getResultado() { return resultado; }
}
