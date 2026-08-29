package com.plataformaeventos.eventos;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;

    public EventoService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @Transactional(readOnly = true)
    public List<EventoRespuesta> listarEventos() {
        return eventoRepository.findByEstadoNotOrderByFechaInicioAsc(EstadoEvento.CANCELADO)
                .stream()
                .map(this::convertirARespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventoRespuesta obtenerPorId(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Evento no encontrado con id " + id));
        return convertirARespuesta(evento);
    }

    private EventoRespuesta convertirARespuesta(Evento evento) {
        return new EventoRespuesta(
                evento.getId(),
                evento.getNombre(),
                evento.getDescripcion(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                evento.getLugarPrincipal(),
                evento.getEstado()
        );
    }
}
