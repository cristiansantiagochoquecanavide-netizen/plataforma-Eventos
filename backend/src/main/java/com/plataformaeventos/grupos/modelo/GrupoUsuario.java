package com.plataformaeventos.grupos.modelo;

import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.usuarios.modelo.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "grupo_usuario", uniqueConstraints = @UniqueConstraint(
        name = "uk_grupo_usuario_grupo_organizacion",
        columnNames = {"id_grupo", "id_organizacion"}
))
public class GrupoUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grupo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organizacion", nullable = false)
    private Organizacion organizacion;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "grupo_usuario_miembro",
            joinColumns = {
                    @JoinColumn(name = "id_grupo", referencedColumnName = "id_grupo"),
                    @JoinColumn(name = "id_organizacion", referencedColumnName = "id_organizacion")
            },
            inverseJoinColumns = @JoinColumn(name = "id_usuario"))
    private Set<Usuario> miembros = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "grupo_usuario_rol",
            joinColumns = @JoinColumn(name = "id_grupo"),
            inverseJoinColumns = @JoinColumn(name = "id_rol"))
    private Set<Rol> roles = new HashSet<>();

    protected GrupoUsuario() {
    }

    public GrupoUsuario(Organizacion organizacion, String nombre, String descripcion) {
        this.organizacion = organizacion;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public Organizacion getOrganizacion() { return organizacion; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public boolean isActivo() { return activo; }
    public Set<Usuario> getMiembros() { return miembros; }
    public Set<Rol> getRoles() { return roles; }

    public void actualizarDatos(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public void desactivar() { this.activo = false; }

    public void activar() { this.activo = true; }

    public void asignarMiembros(Set<Usuario> miembros) {
        this.miembros = new HashSet<>(miembros);
    }

    public void asignarRoles(Set<Rol> roles) {
        this.roles = new HashSet<>(roles);
    }
}
