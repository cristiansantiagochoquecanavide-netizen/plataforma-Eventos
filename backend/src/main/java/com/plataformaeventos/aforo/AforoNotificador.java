package com.plataformaeventos.aforo;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AforoNotificador {
    private final SimpMessagingTemplate mensajes;
    public AforoNotificador(SimpMessagingTemplate mensajes) { this.mensajes = mensajes; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(AforoActualizado evento) {
        // Solo invalidación: el cliente vuelve a consultar su instantánea autorizada.
        try {
            mensajes.convertAndSend("/topic/aforo/" + evento.organizacionId(),
                    java.util.Map.of("eventoId", evento.eventoId()));
        } catch (org.springframework.messaging.MessagingException ex) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("No se pudo notificar el cambio de aforo; se recuperará al resincronizar", ex);
        }
    }
}
