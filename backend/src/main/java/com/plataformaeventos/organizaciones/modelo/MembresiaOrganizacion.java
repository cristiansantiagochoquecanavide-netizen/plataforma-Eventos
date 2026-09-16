package com.plataformaeventos.organizaciones.modelo;

import com.plataformaeventos.usuarios.modelo.Usuario;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import com.plataformaeventos.roles.modelo.Rol;

@Entity @Table(name = "usuario_organizacion")
@IdClass(MembresiaOrganizacion.Clave.class)
public class MembresiaOrganizacion {
    @Id @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_usuario") private Usuario usuario;
    @Id @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_organizacion") private Organizacion organizacion;
    @Column(nullable = false) private boolean activo = true;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_organizacion_rol",
            joinColumns = {
                    @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario"),
                    @JoinColumn(name = "id_organizacion", referencedColumnName = "id_organizacion")
            }, inverseJoinColumns = @JoinColumn(name = "id_rol"))
    private Set<Rol> roles = new HashSet<>();
    protected MembresiaOrganizacion() {}
    public MembresiaOrganizacion(Usuario usuario, Organizacion organizacion) {
        this.usuario = usuario;
        this.organizacion = organizacion;
    }
    public boolean estaActiva() { return activo; }
    public Usuario getUsuario() { return usuario; }
    public Organizacion getOrganizacion() { return organizacion; }
    public boolean isActivo() { return activo; }
    public void activar() { this.activo = true; }
    public void inactivar() { this.activo = false; }
    public Set<Rol> getRoles() { return roles; }
    public void asignarRoles(Set<Rol> roles) { this.roles = new HashSet<>(roles); }
    public static class Clave implements Serializable { public Long usuario; public Long organizacion; public Clave() {} }
}
