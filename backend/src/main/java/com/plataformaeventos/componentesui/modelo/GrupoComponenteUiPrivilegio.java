package com.plataformaeventos.componentesui.modelo;

import com.plataformaeventos.grupos.modelo.GrupoUsuario;
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
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "grupo_componente_ui_privilegio",
        uniqueConstraints = @UniqueConstraint(name = "uk_grupo_componente_ui_privilegio_estado",
                columnNames = {"id_organizacion", "id_grupo", "clave_componente", "estado"}))
public class GrupoComponenteUiPrivilegio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_privilegio_ui")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organizacion", nullable = false)
    private Organizacion organizacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "id_grupo", referencedColumnName = "id_grupo", nullable = false),
            @JoinColumn(name = "id_organizacion", referencedColumnName = "id_organizacion", nullable = false, insertable = false, updatable = false)
    })
    private GrupoUsuario grupo;

    @Column(name = "clave_componente", nullable = false, length = 120)
    private String claveComponente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPrivilegioUi estado;

    protected GrupoComponenteUiPrivilegio() {
    }

    public GrupoComponenteUiPrivilegio(Organizacion organizacion, GrupoUsuario grupo, String claveComponente, EstadoPrivilegioUi estado) {
        this.organizacion = organizacion;
        this.grupo = grupo;
        this.claveComponente = claveComponente;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public Organizacion getOrganizacion() { return organizacion; }
    public GrupoUsuario getGrupo() { return grupo; }
    public String getClaveComponente() { return claveComponente; }
    public EstadoPrivilegioUi getEstado() { return estado; }
}
