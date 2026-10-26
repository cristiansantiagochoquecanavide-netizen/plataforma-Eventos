package com.plataformaeventos.inscripciones.modelo;

import com.plataformaeventos.eventos.modelo.Evento;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@jakarta.persistence.EntityListeners(com.plataformaeventos.aforo.CambiosAforoListener.class)
@Table(name = "inscripcion")
public class Inscripcion {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organizacion")
    private Organizacion organizacion;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inscripcion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_participante", nullable = false)
    private Usuario participante;

    @Column(name = "fecha_inscripcion", nullable = false)
    private OffsetDateTime fechaInscripcion;

    @Column(name = "codigo_qr", nullable = false, unique = true, length = 255)
    private String codigoQr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoInscripcion estado;

    @Column(name = "categoria_entrada", length = 80)
    private String categoriaEntrada;

    public String getCategoriaEntrada() { return categoriaEntrada; }
    public void clasificarEntrada(String categoria) {
        String valor = categoria == null ? null : categoria.strip();
        if (valor != null && (valor.length() > 80 || valor.chars().anyMatch(Character::isISOControl)))
            throw new IllegalArgumentException("Categoria de entrada invalida (maximo 80 caracteres)");
        categoriaEntrada = valor == null || valor.isBlank() ? null : valor;
    }

    protected Inscripcion() {
    }

    public Inscripcion(Evento evento, Usuario participante, String codigoQr) {
        this(evento, participante, codigoQr, OffsetDateTime.now(), EstadoInscripcion.CONFIRMADA);
    }

    public Inscripcion(Evento evento, Usuario participante, String codigoQr,
                       OffsetDateTime fechaInscripcion, EstadoInscripcion estado) {
        this.evento = evento;
        this.participante = participante;
        this.codigoQr = codigoQr;
        this.estado = estado == null ? EstadoInscripcion.CONFIRMADA : estado;
        this.fechaInscripcion = fechaInscripcion == null ? OffsetDateTime.now() : fechaInscripcion;
    }

    @PrePersist
    void prePersist() {
        if (fechaInscripcion == null) {
            fechaInscripcion = OffsetDateTime.now();
        }
        if (estado == null) {
            estado = EstadoInscripcion.CONFIRMADA;
        }
    }

    public Long getId() {
        return id;
    }
    public Organizacion getOrganizacion() { return organizacion; }
    public void asignarOrganizacion(Organizacion organizacion) { this.organizacion = organizacion; }

    public Evento getEvento() {
        return evento;
    }

    public Usuario getParticipante() {
        return participante;
    }

    public OffsetDateTime getFechaInscripcion() {
        return fechaInscripcion;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public EstadoInscripcion getEstado() {
        return estado;
    }

    public void cancelar() {
        if (this.estado == EstadoInscripcion.CANCELADA) {
            throw new IllegalStateException("La inscripción ya se encuentra cancelada");
        }
        this.estado = EstadoInscripcion.CANCELADA;
    }

    public void reactivar(String nuevoCodigoQr) {
        this.estado = EstadoInscripcion.CONFIRMADA;
        this.codigoQr = nuevoCodigoQr;
        this.fechaInscripcion = OffsetDateTime.now();
    }

    public boolean estaConfirmada() {
        return this.estado == EstadoInscripcion.CONFIRMADA;
    }
}
