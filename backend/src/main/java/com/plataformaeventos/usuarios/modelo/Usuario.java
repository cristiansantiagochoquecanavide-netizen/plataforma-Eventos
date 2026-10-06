package com.plataformaeventos.usuarios.modelo;

import com.plataformaeventos.roles.modelo.Rol;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 254)
    private String correo;

    @Column(name = "clave_hash", nullable = false, length = 100)
    private String claveHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoUsuario estado;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_rol")
    )
    private Set<Rol> roles = new HashSet<>();

    protected Usuario() {
    }

    public Usuario(String nombres, String apellidos, String correo, String claveHash, EstadoUsuario estado) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.claveHash = claveHash;
        this.estado = estado;
    }

    public Usuario(String nombres, String apellidos, String correo, String claveHash, EstadoUsuario estado,
                   OffsetDateTime fechaCreacion) {
        this(nombres, apellidos, correo, claveHash, estado);
        this.fechaCreacion = fechaCreacion;
    }

    @PrePersist
    void asignarFechasCreacion() {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (fechaCreacion == null) fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void actualizarFechaActualizacion() {
        fechaActualizacion = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public String getClaveHash() {
        return claveHash;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public Set<Rol> getRoles() {
        return roles;
    }

    public void asignarRoles(Set<Rol> roles) {
        this.roles = new HashSet<>(roles);
    }

    public void actualizarClaveHash(String claveHash) {
        this.claveHash = claveHash;
    }

    public void actualizarDatos(String nombres, String apellidos, String correo) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
    }

    public void actualizarEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
