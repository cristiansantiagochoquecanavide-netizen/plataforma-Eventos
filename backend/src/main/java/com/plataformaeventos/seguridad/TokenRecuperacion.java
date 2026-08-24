package com.plataformaeventos.seguridad;

import com.plataformaeventos.usuarios.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "token_recuperacion")
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private OffsetDateTime fechaExpiracion;

    @Column(nullable = false)
    private boolean utilizado = false;

    @Column(name = "fecha_uso")
    private OffsetDateTime fechaUso;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_inactivacion")
    private OffsetDateTime fechaInactivacion;

    protected TokenRecuperacion() {
    }

    public TokenRecuperacion(Usuario usuario, String tokenHash, OffsetDateTime fechaExpiracion) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.fechaExpiracion = fechaExpiracion;
    }

    @PrePersist
    void asignarFechaCreacion() {
        fechaCreacion = OffsetDateTime.now();
    }

    public boolean fueUtilizado() {
        return utilizado;
    }

    public boolean estaActivo() {
        return activo;
    }

    public boolean estaExpirado(OffsetDateTime ahora) {
        return !fechaExpiracion.isAfter(ahora);
    }

    public void invalidar(OffsetDateTime ahora) {
        activo = false;
        fechaInactivacion = ahora;
    }

    public void marcarComoUtilizado(OffsetDateTime ahora) {
        utilizado = true;
        fechaUso = ahora;
        invalidar(ahora);
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
