package com.plataformaeventos.notificaciones.modelo;

import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notificaciones_eventos", uniqueConstraints =
        @UniqueConstraint(name = "uq_notificacion_tenant_deduplicacion", columnNames = {"id_organizacion", "clave_deduplicacion"}))
public class NotificacionEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_inscripcion", nullable = false)
    private Inscripcion inscripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organizacion", nullable = false)
    private Organizacion organizacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_notificacion", nullable = false, length = 40)
    private TipoNotificacionEvento tipo;

    @Column(name = "canal_envio", nullable = false, length = 20)
    private String canalEnvio;

    @Column(nullable = false, length = 150)
    private String asunto;

    @Column(name = "cuerpo_mensaje", nullable = false, columnDefinition = "TEXT")
    private String cuerpoMensaje;

    @Column(name = "fecha_programada", nullable = false)
    private OffsetDateTime fechaProgramada;

    @Column(name = "fecha_envio")
    private OffsetDateTime fechaEnvio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false, length = 20)
    private EstadoEnvioNotificacion estadoEnvio;

    @Column(name = "id_mensaje_externo", length = 150)
    private String idMensajeExterno;

    @Column(name = "clave_deduplicacion", nullable = false, length = 180)
    private String claveDeduplicacion;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    @Column(nullable = false)
    private int intentos;

    protected NotificacionEvento() { }

    public NotificacionEvento(Evento evento, Inscripcion inscripcion, Usuario usuario, Organizacion organizacion,
                              TipoNotificacionEvento tipo, String asunto, String cuerpoMensaje,
                              OffsetDateTime fechaProgramada, String claveDeduplicacion) {
        this.evento = evento;
        this.inscripcion = inscripcion;
        this.usuario = usuario;
        this.organizacion = organizacion;
        this.tipo = tipo;
        this.canalEnvio = "BREVO";
        this.asunto = asunto;
        this.cuerpoMensaje = cuerpoMensaje;
        this.fechaProgramada = fechaProgramada;
        this.claveDeduplicacion = claveDeduplicacion;
        this.estadoEnvio = EstadoEnvioNotificacion.PENDIENTE;
        this.intentos = 0;
    }

    public void iniciarEnvio() {
        if (estadoEnvio != EstadoEnvioNotificacion.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden enviar notificaciones pendientes");
        }
        estadoEnvio = EstadoEnvioNotificacion.PROCESANDO;
        intentos++;
        ultimoError = null;
    }

    public void marcarEnviado(String canal, String idMensaje, OffsetDateTime fecha) {
        estadoEnvio = EstadoEnvioNotificacion.ENVIADO;
        canalEnvio = canal;
        idMensajeExterno = idMensaje;
        fechaEnvio = fecha;
        ultimoError = null;
    }

    public void marcarFallido(String error) {
        estadoEnvio = EstadoEnvioNotificacion.FALLIDO;
        ultimoError = error == null ? "Fallo desconocido" : error.substring(0, Math.min(500, error.length()));
    }

    public void reintentar(OffsetDateTime ahora) {
        if (estadoEnvio != EstadoEnvioNotificacion.FALLIDO) {
            throw new IllegalStateException("Solo se pueden reintentar notificaciones fallidas");
        }
        estadoEnvio = EstadoEnvioNotificacion.PENDIENTE;
        fechaProgramada = ahora;
        ultimoError = null;
    }

    public void cancelar() {
        if (estadoEnvio == EstadoEnvioNotificacion.PENDIENTE) {
            estadoEnvio = EstadoEnvioNotificacion.CANCELADO;
        }
    }

    public Long getId() { return id; }
    public Evento getEvento() { return evento; }
    public Inscripcion getInscripcion() { return inscripcion; }
    public Usuario getUsuario() { return usuario; }
    public Organizacion getOrganizacion() { return organizacion; }
    public TipoNotificacionEvento getTipo() { return tipo; }
    public String getCanalEnvio() { return canalEnvio; }
    public String getAsunto() { return asunto; }
    public String getCuerpoMensaje() { return cuerpoMensaje; }
    public OffsetDateTime getFechaProgramada() { return fechaProgramada; }
    public OffsetDateTime getFechaEnvio() { return fechaEnvio; }
    public EstadoEnvioNotificacion getEstadoEnvio() { return estadoEnvio; }
    public String getIdMensajeExterno() { return idMensajeExterno; }
    public String getClaveDeduplicacion() { return claveDeduplicacion; }
    public String getUltimoError() { return ultimoError; }
    public int getIntentos() { return intentos; }
}
