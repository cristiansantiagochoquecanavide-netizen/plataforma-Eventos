package com.plataformaeventos.aforo;

import com.plataformaeventos.asistencias.modelo.Asistencia;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.eventos.modelo.Evento;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class CambiosAforoListener {
    private final ApplicationEventPublisher eventos;
    public CambiosAforoListener(ApplicationEventPublisher eventos) { this.eventos = eventos; }
    @PostPersist @PostUpdate
    public void cambiado(Object entidad) {
        if (entidad instanceof com.plataformaeventos.espacios.modelo.Espacio espacio) {
            if (espacio.getOrganizacion() != null) eventos.publishEvent(new AforoActualizado(espacio.getOrganizacion().getId(), 0L));
            return;
        }
        Evento evento = entidad instanceof Asistencia a ? a.getInscripcion().getEvento()
                : entidad instanceof Inscripcion i ? i.getEvento() : (Evento) entidad;
        if (evento.getOrganizacion() != null)
            eventos.publishEvent(new AforoActualizado(evento.getOrganizacion().getId(), evento.getId()));
    }
}
