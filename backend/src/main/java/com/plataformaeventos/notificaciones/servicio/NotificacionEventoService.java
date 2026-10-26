package com.plataformaeventos.notificaciones.servicio;

import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.inscripciones.dto.CredencialDigitalRespuesta;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.inscripciones.servicio.CredencialQrService;
import com.plataformaeventos.notificaciones.dto.NotificacionEventoRespuesta;
import com.plataformaeventos.notificaciones.dto.RecordatorioMasivoRespuesta;
import com.plataformaeventos.notificaciones.dto.RecordatorioMasivoSolicitud;
import com.plataformaeventos.notificaciones.modelo.EstadoEnvioNotificacion;
import com.plataformaeventos.notificaciones.modelo.NotificacionEvento;
import com.plataformaeventos.notificaciones.modelo.TipoNotificacionEvento;
import com.plataformaeventos.notificaciones.repositorio.NotificacionEventoRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class NotificacionEventoService {
    private static final DateTimeFormatter FECHA_CORREO =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm", Locale.forLanguageTag("es"));
    private static final List<TipoNotificacionEvento> TIPOS_RECORDATORIO = List.of(
            TipoNotificacionEvento.RECORDATORIO_24H,
            TipoNotificacionEvento.RECORDATORIO_2H,
            TipoNotificacionEvento.RECORDATORIO_MANUAL);

    private final NotificacionEventoRepository notificaciones;
    private final InscripcionRepository inscripciones;
    private final EventoRepository eventos;
    private final CredencialQrService credenciales;
    private final CorreoTransaccionalService correo;

    public NotificacionEventoService(NotificacionEventoRepository notificaciones,
                                     InscripcionRepository inscripciones,
                                     EventoRepository eventos,
                                     CredencialQrService credenciales,
                                     CorreoTransaccionalService correo) {
        this.notificaciones = notificaciones;
        this.inscripciones = inscripciones;
        this.eventos = eventos;
        this.credenciales = credenciales;
        this.correo = correo;
    }

    @Transactional
    public void programarConfirmacion(Inscripcion inscripcion, Organizacion organizacion) {
        if (inscripcion == null || !inscripcion.estaConfirmada()) return;
        Organizacion tenant = organizacion != null ? organizacion : inscripcion.getOrganizacion();
        if (tenant == null || inscripcion.getId() == null) return;
        OffsetDateTime ahora = OffsetDateTime.now();
        String dedupe = "inscripcion:" + inscripcion.getId() + ":confirmacion:"
                + inscripcion.getFechaInscripcion().toInstant().toEpochMilli();
        crearSiNoExiste(inscripcion, tenant, TipoNotificacionEvento.CONFIRMACION_INSCRIPCION,
                "Inscripción confirmada · " + inscripcion.getEvento().getNombre(),
                "Tu inscripción está confirmada. Adjuntamos tu credencial QR personal para el acceso al evento.",
                ahora, dedupe);
    }

    @Transactional
    public int programarRecordatoriosProximos() {
        OffsetDateTime ahora = OffsetDateTime.now();
        List<Evento> eventosProximos = eventos.findUpcomingForReminders(ahora, ahora.plusHours(24).plusMinutes(2));
        int programados = 0;
        for (Evento evento : eventosProximos) {
            Organizacion organizacion = evento.getOrganizacion();
            if (organizacion == null || evento.getEstado() == EstadoEvento.CANCELADO) continue;
            List<Inscripcion> confirmadas = inscripciones.findConfirmedByEventoAndOrganizacion(
                    evento.getId(), organizacion.getId());
            for (Inscripcion inscripcion : confirmadas) {
                String ubicacion = ubicacion(evento);
                String contenido = "Te recordamos que tu evento comienza pronto. "
                        + "Fecha: " + fecha(evento.getFechaInicio()) + ". Lugar: " + ubicacion
                        + ". Presenta tu credencial QR en el acceso y llega con anticipación.";
                programados += crearSiNoExiste(inscripcion, organizacion,
                        TipoNotificacionEvento.RECORDATORIO_24H,
                        "Mañana: " + evento.getNombre(), contenido,
                        evento.getFechaInicio().minusHours(24),
                        "inscripcion:" + inscripcion.getId() + ":recordatorio:24h") ? 1 : 0;
                programados += crearSiNoExiste(inscripcion, organizacion,
                        TipoNotificacionEvento.RECORDATORIO_2H,
                        "En 2 horas: " + evento.getNombre(), contenido,
                        evento.getFechaInicio().minusHours(2),
                        "inscripcion:" + inscripcion.getId() + ":recordatorio:2h") ? 1 : 0;
            }
        }
        return programados;
    }

    @Transactional
    public RecordatorioMasivoRespuesta programarMasivo(Long idEvento, Organizacion organizacion,
                                                       RecordatorioMasivoSolicitud solicitud) {
        Evento evento = buscarEvento(idEvento, organizacion);
        OffsetDateTime ahora = OffsetDateTime.now();
        if (solicitud.fechaProgramada() == null || !solicitud.fechaProgramada().isAfter(ahora)) {
            throw new IllegalArgumentException("La fecha del recordatorio debe ser futura");
        }
        if (evento.getEstado() == EstadoEvento.CANCELADO || !solicitud.fechaProgramada().isBefore(evento.getFechaInicio())) {
            throw new IllegalArgumentException("El recordatorio debe enviarse antes del inicio de un evento activo");
        }
        List<Inscripcion> confirmadas = inscripciones.findConfirmedByEventoAndOrganizacion(
                evento.getId(), organizacion.getId());
        if (confirmadas.isEmpty()) {
            throw new IllegalStateException("El evento no tiene participantes con inscripción confirmada");
        }

        String lote = UUID.randomUUID().toString();
        for (Inscripcion inscripcion : confirmadas) {
            crearSiNoExiste(inscripcion, organizacion, TipoNotificacionEvento.RECORDATORIO_MANUAL,
                    solicitud.asunto().trim(), solicitud.mensaje().trim(), solicitud.fechaProgramada(),
                    "evento:" + idEvento + ":manual:" + lote + ":inscripcion:" + inscripcion.getId());
        }
        return new RecordatorioMasivoRespuesta(idEvento, confirmadas.size(), solicitud.fechaProgramada());
    }

    @Transactional
    public void notificarCambioEvento(Evento evento, Organizacion organizacion,
                                     boolean reprogramado, boolean cambioEspacio, boolean cancelado) {
        if (organizacion == null || evento.getId() == null) return;
        if (cancelado) {
            notificaciones.cancelarPendientesPorEvento(evento.getId(), organizacion.getId(),
                    EstadoEnvioNotificacion.PENDIENTE, EstadoEnvioNotificacion.CANCELADO,
                    List.of(TipoNotificacionEvento.CONFIRMACION_INSCRIPCION, TipoNotificacionEvento.RECORDATORIO_24H,
                            TipoNotificacionEvento.RECORDATORIO_2H, TipoNotificacionEvento.RECORDATORIO_MANUAL),
                    "El evento fue cancelado antes del envío");
        } else if (reprogramado || cambioEspacio) {
            notificaciones.cancelarPendientesPorEvento(evento.getId(), organizacion.getId(),
                    EstadoEnvioNotificacion.PENDIENTE, EstadoEnvioNotificacion.CANCELADO,
                    TIPOS_RECORDATORIO, "El evento cambió y este recordatorio quedó obsoleto");
        }

        List<Inscripcion> asistentes = inscripciones.findConfirmedByEventoAndOrganizacion(
                evento.getId(), organizacion.getId());
        String lote = UUID.randomUUID().toString();
        OffsetDateTime ahora = OffsetDateTime.now();
        for (Inscripcion inscripcion : asistentes) {
            if (cancelado) {
                crearSiNoExiste(inscripcion, organizacion, TipoNotificacionEvento.EVENTO_CANCELADO,
                        "Evento cancelado · " + evento.getNombre(),
                        "El evento fue cancelado. Ya no necesitas presentar tu credencial de acceso.",
                        ahora, "evento:" + evento.getId() + ":cancelado:" + lote + ":inscripcion:" + inscripcion.getId());
            } else {
                if (reprogramado) {
                    crearSiNoExiste(inscripcion, organizacion, TipoNotificacionEvento.EVENTO_REPROGRAMADO,
                            "Nuevo horario · " + evento.getNombre(),
                            "El horario del evento fue actualizado. Nueva fecha: " + fecha(evento.getFechaInicio()) + ".",
                            ahora, "evento:" + evento.getId() + ":reprogramado:" + lote + ":inscripcion:" + inscripcion.getId());
                }
                if (cambioEspacio) {
                    crearSiNoExiste(inscripcion, organizacion, TipoNotificacionEvento.CAMBIO_ESPACIO,
                            "Nueva ubicación · " + evento.getNombre(),
                            "La ubicación del evento fue actualizada. Nuevo lugar: " + ubicacion(evento) + ".",
                            ahora, "evento:" + evento.getId() + ":espacio:" + lote + ":inscripcion:" + inscripcion.getId());
                }
            }
        }
    }

    @Transactional
    public void cancelarPendientesInscripcion(Inscripcion inscripcion, Organizacion organizacion) {
        if (inscripcion == null || inscripcion.getId() == null || organizacion == null) return;
        notificaciones.cancelarPendientesPorInscripcion(inscripcion.getId(), organizacion.getId(),
                EstadoEnvioNotificacion.PENDIENTE, EstadoEnvioNotificacion.CANCELADO,
                "La inscripción fue cancelada antes del envío");
    }

    @Transactional(readOnly = true)
    public List<NotificacionEventoRespuesta> listar(Long idEvento, Organizacion organizacion) {
        buscarEvento(idEvento, organizacion);
        return notificaciones.findTop100ByOrganizacionIdAndEventoIdOrderByFechaProgramadaDesc(
                        organizacion.getId(), idEvento)
                .stream().map(NotificacionEventoRespuesta::desde).toList();
    }

    @Transactional
    public NotificacionEventoRespuesta reintentar(Long idEvento, Long idNotificacion, Organizacion organizacion) {
        NotificacionEvento notificacion = notificaciones.findByIdAndOrganizacionId(
                        idNotificacion, organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Notificación no encontrada"));
        if (!idEvento.equals(notificacion.getEvento().getId())) {
            throw new ExcepcionNoEncontrado("Notificación no encontrada");
        }
        notificacion.reintentar(OffsetDateTime.now());
        return NotificacionEventoRespuesta.desde(notificaciones.save(notificacion));
    }

    @Transactional
    public int despacharPendientes() {
        List<NotificacionEvento> pendientes = notificaciones.bloquearPendientes(
                EstadoEnvioNotificacion.PENDIENTE, OffsetDateTime.now(), PageRequest.of(0, 25));
        for (NotificacionEvento notificacion : pendientes) {
            try {
                notificacion.iniciarEnvio();
                List<CorreoTransaccionalService.AdjuntoCorreo> adjuntos = adjuntos(notificacion);
                CorreoTransaccionalService.ResultadoCorreo resultado = correo.enviar(
                        notificacion.getUsuario().getCorreo(),
                        notificacion.getAsunto(),
                        plantillaHtml(notificacion),
                        adjuntos);
                notificacion.marcarEnviado(resultado.canal(), resultado.idMensaje(), OffsetDateTime.now());
            } catch (Exception exception) {
                notificacion.marcarFallido(mensajeSeguro(exception));
            }
        }
        notificaciones.flush();
        return pendientes.size();
    }

    private List<CorreoTransaccionalService.AdjuntoCorreo> adjuntos(NotificacionEvento notificacion) {
        if (notificacion.getTipo() == TipoNotificacionEvento.EVENTO_CANCELADO
                || notificacion.getInscripcion().getEstado() != EstadoInscripcion.CONFIRMADA) {
            return List.of();
        }
        CredencialDigitalRespuesta credencial = credenciales.emitirPara(notificacion.getInscripcion());
        return List.of(new CorreoTransaccionalService.AdjuntoCorreo(
                "credencial-" + notificacion.getInscripcion().getId() + ".png",
                "image/png",
                Base64.getDecoder().decode(credencial.qrPngBase64())));
    }

    private String plantillaHtml(NotificacionEvento notificacion) {
        Evento evento = notificacion.getEvento();
        String contenido = HtmlUtils.htmlEscape(notificacion.getCuerpoMensaje()).replace("\n", "<br>");
        String nombre = HtmlUtils.htmlEscape(notificacion.getUsuario().getNombres());
        String eventoSeguro = HtmlUtils.htmlEscape(evento.getNombre());
        String fecha = HtmlUtils.htmlEscape(fecha(evento.getFechaInicio()));
        String lugar = HtmlUtils.htmlEscape(ubicacion(evento));
        String adjunto = notificacion.getTipo() == TipoNotificacionEvento.EVENTO_CANCELADO
                ? ""
                : "<p style=\"margin:20px 0 0;color:#64746d;font-size:13px\">Tu credencial QR está adjunta a este correo.</p>";
        return "<div style=\"background:#f5f3ec;padding:28px;font-family:Arial,sans-serif;color:#172824\">"
                + "<div style=\"max-width:560px;margin:auto;background:#fffefa;border:1px solid #dce3dc;border-radius:18px;overflow:hidden\">"
                + "<div style=\"background:#17312c;padding:22px 26px;color:#fff;font-size:14px;font-weight:bold;letter-spacing:1px\">eventos.</div>"
                + "<div style=\"padding:26px\"><p style=\"color:#64746d;margin:0 0 8px\">Hola " + nombre + ",</p>"
                + "<h1 style=\"font-size:23px;line-height:1.25;margin:0 0 16px;color:#17312c\">" + eventoSeguro + "</h1>"
                + "<p style=\"font-size:15px;line-height:1.65\">" + contenido + "</p>"
                + "<div style=\"background:#e6efe8;border-radius:12px;padding:15px;margin-top:18px\">"
                + "<strong>Fecha</strong><br>" + fecha + "<br><br><strong>Lugar</strong><br>" + lugar
                + "</div>" + adjunto + "<p style=\"font-size:12px;color:#64746d;margin:24px 0 0\">Mensaje de la organización de tu evento.</p></div></div></div>";
    }

    private boolean crearSiNoExiste(Inscripcion inscripcion, Organizacion organizacion,
                                   TipoNotificacionEvento tipo, String asunto, String contenido,
                                   OffsetDateTime programada, String claveDeduplicacion) {
        if (notificaciones.existsByOrganizacionIdAndClaveDeduplicacion(
                organizacion.getId(), claveDeduplicacion)) return false;
        notificaciones.save(new NotificacionEvento(inscripcion.getEvento(), inscripcion,
                inscripcion.getParticipante(), organizacion, tipo, asunto, contenido,
                programada, claveDeduplicacion));
        return true;
    }

    private Evento buscarEvento(Long idEvento, Organizacion organizacion) {
        if (organizacion == null) throw new IllegalArgumentException("Se requiere la organización activa");
        return eventos.findByIdAndOrganizacionId(idEvento, organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Evento no encontrado"));
    }

    private String fecha(OffsetDateTime fecha) {
        return FECHA_CORREO.format(fecha);
    }

    private String ubicacion(Evento evento) {
        String principal = evento.getLugarPrincipal();
        if (principal != null && !principal.isBlank()) return principal;
        if (evento.getEspacio() == null) return "Lugar por confirmar";
        String nombre = evento.getEspacio().getNombre();
        String direccion = evento.getEspacio().getUbicacion();
        return direccion == null || direccion.isBlank() ? nombre : nombre + " · " + direccion;
    }

    private String mensajeSeguro(Exception exception) {
        String mensaje = exception.getMessage();
        return mensaje == null || mensaje.isBlank() ? "No fue posible entregar el correo" : mensaje;
    }
}
