package com.plataformaeventos.notificaciones.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notificaciones.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class NotificacionesScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificacionesScheduler.class);
    private final NotificacionEventoService notificaciones;

    public NotificacionesScheduler(NotificacionEventoService notificaciones) {
        this.notificaciones = notificaciones;
    }

    @Scheduled(fixedDelayString = "${notificaciones.scheduler.intervalo-ms:60000}",
            initialDelayString = "${notificaciones.scheduler.inicio-ms:20000}")
    public void ejecutarCiclo() {
        try {
            notificaciones.programarRecordatoriosProximos();
            notificaciones.despacharPendientes();
        } catch (RuntimeException exception) {
            LOGGER.error("No se pudo completar el ciclo de notificaciones de eventos", exception);
        }
    }
}
