package com.plataformaeventos.eventos.modelo;

import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.espacios.modelo.Espacio;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "evento")
public class Evento {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organizacion")
    private Organizacion organizacion;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private OffsetDateTime fechaFin;

    @Column(name = "lugar_principal", length = 150)
    private String lugarPrincipal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_espacio")
    private Espacio espacio;

    @Column(name = "aforo_maximo")
    private Integer aforoMaximo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEvento estado;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "evento_responsable",
            joinColumns = @JoinColumn(name = "id_evento"),
            inverseJoinColumns = @JoinColumn(name = "id_usuario")
    )
    private Set<Usuario> responsables = new HashSet<>();

    protected Evento() {
    }

    public Evento(
            String nombre,
            String descripcion,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin,
            String lugarPrincipal,
            EstadoEvento estado,
            Categoria categoria
    ) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.lugarPrincipal = lugarPrincipal;
        this.estado = estado;
        this.categoria = categoria;
    }

    public Evento(
            String nombre,
            String descripcion,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin,
            String lugarPrincipal,
            EstadoEvento estado,
            Categoria categoria,
            Espacio espacio,
            Integer aforoMaximo
    ) {
        this(nombre, descripcion, fechaInicio, fechaFin, lugarPrincipal, estado, categoria);
        configurarProgramacion(espacio, aforoMaximo);
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        fechaActualizacion = ahora;
        if (estado == null) {
            estado = EstadoEvento.PLANIFICADO;
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

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public OffsetDateTime getFechaInicio() {
        return fechaInicio;
    }

    public OffsetDateTime getFechaFin() {
        return fechaFin;
    }

    public String getLugarPrincipal() {
        return lugarPrincipal;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Espacio getEspacio() {
        return espacio;
    }

    public Integer getAforoMaximo() {
        return aforoMaximo;
    }

    public EstadoEvento getEstado() {
        return estado;
    }

    public Set<Usuario> getResponsables() {
        return responsables;
    }

    public void actualizarDatos(
            String nombre,
            String descripcion,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin,
            String lugarPrincipal,
            EstadoEvento estado,
            Categoria categoria
    ) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.lugarPrincipal = lugarPrincipal;
        this.estado = estado;
        this.categoria = categoria;
    }

    public void configurarProgramacion(Espacio espacio, Integer aforoMaximo) {
        this.espacio = espacio;
        this.aforoMaximo = aforoMaximo;
    }

    public void asignarResponsables(Set<Usuario> responsables) {
        this.responsables = new HashSet<>(responsables);
    }
}
