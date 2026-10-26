package com.plataformaeventos.notificaciones.dto;

import com.plataformaeventos.notificaciones.modelo.NotificacionEvento;

import java.time.OffsetDateTime;

public record NotificacionEventoRespuesta(
        Long id,
        Long idEvento,
        String evento,
        Long idInscripcion,
        String destinatario,
        String tipo,
        String canal,
        String asunto,
        OffsetDateTime fechaProgramada,
        OffsetDateTime fechaEnvio,
        String estado,
        String idMensajeExterno,
        String ultimoError,
        int intentos
) {
    public static NotificacionEventoRespuesta desde(NotificacionEvento notificacion) {
        return new NotificacionEventoRespuesta(
                notificacion.getId(),
                notificacion.getEvento().getId(),
                notificacion.getEvento().getNombre(),
                notificacion.getInscripcion().getId(),
                notificacion.getUsuario().getCorreo(),
                notificacion.getTipo().name(),
                notificacion.getCanalEnvio(),
                notificacion.getAsunto(),
                notificacion.getFechaProgramada(),
                notificacion.getFechaEnvio(),
                notificacion.getEstadoEnvio().name(),
                notificacion.getIdMensajeExterno(),
                notificacion.getUltimoError(),
                notificacion.getIntentos()
        );
    }
}
