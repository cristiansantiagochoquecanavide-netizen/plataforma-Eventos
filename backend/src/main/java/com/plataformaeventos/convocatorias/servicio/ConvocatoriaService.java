package com.plataformaeventos.convocatorias.servicio;

import com.plataformaeventos.convocatorias.dto.ConvocatoriaRespuesta;
import com.plataformaeventos.convocatorias.dto.CrearConvocatoriaSolicitud;
import com.plataformaeventos.convocatorias.modelo.Convocatoria;
import com.plataformaeventos.convocatorias.modelo.EstadoConvocatoria;
import com.plataformaeventos.convocatorias.modelo.TipoPersonal;
import com.plataformaeventos.convocatorias.repositorio.ConvocatoriaRepository;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import com.plataformaeventos.organizaciones.modelo.Organizacion;

@Service
public class ConvocatoriaService {

    private final ConvocatoriaRepository convocatoriaRepository;
    private final EventoRepository eventoRepository;

    public ConvocatoriaService(
            ConvocatoriaRepository convocatoriaRepository,
            EventoRepository eventoRepository
    ) {
        this.convocatoriaRepository = convocatoriaRepository;
        this.eventoRepository = eventoRepository;
    }

    @Transactional(readOnly = true)
    public List<ConvocatoriaRespuesta> listar(
            Long idEvento,
            TipoPersonal tipoPersonal,
            EstadoConvocatoria estado,
            String termino
    ) {
        String terminoNormalizado = termino == null ? null : termino.trim();
        Sort orden = Sort.by(Sort.Direction.DESC, "fechaCreacion");
        return convocatoriaRepository
                .buscarConFiltros(idEvento, tipoPersonal, estado, terminoNormalizado, orden)
                .stream()
                .map(this::convertirARespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConvocatoriaRespuesta> listar(Long idEvento, TipoPersonal tipoPersonal, EstadoConvocatoria estado, String termino, Organizacion organizacion) {
        return convocatoriaRepository.buscarConFiltrosTenant(idEvento, tipoPersonal, estado, termino == null ? null : termino.trim(), organizacion.getId(), Sort.by(Sort.Direction.DESC, "fechaCreacion")).stream().map(this::convertirARespuesta).toList();
    }

    @Transactional(readOnly = true)
    public ConvocatoriaRespuesta obtenerPorId(Long id) {
        return convertirARespuesta(buscarPorId(id));
    }

    @Transactional(readOnly = true)
    public ConvocatoriaRespuesta obtenerPorId(Long id, Organizacion organizacion) {
        return convertirARespuesta(convocatoriaRepository.findByIdAndOrganizacionId(id, organizacion.getId()).orElseThrow(() -> new ExcepcionNoEncontrado("Convocatoria no encontrada con id " + id)));
    }

    @Transactional
    public ConvocatoriaRespuesta crear(CrearConvocatoriaSolicitud solicitud) {
        return crear(solicitud, null);
    }

    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "CONVOCATORIAS", entidad = "CONVOCATORIA", accion = "CREAR")
    public ConvocatoriaRespuesta crear(CrearConvocatoriaSolicitud solicitud, Organizacion organizacion) {
        Evento evento = (organizacion == null ? eventoRepository.findById(solicitud.idEvento()) : eventoRepository.findByIdAndOrganizacionId(solicitud.idEvento(), organizacion.getId()))
                .orElseThrow(() -> new ExcepcionNoEncontrado(
                        "Evento no encontrado con id " + solicitud.idEvento()
                ));

        validarEventoParaConvocatoria(evento, solicitud.fechaLimite());

        Convocatoria convocatoria = new Convocatoria(
                evento,
                solicitud.tipoPersonal(),
                solicitud.descripcion().trim(),
                solicitud.cantidadRequerida(),
                solicitud.fechaLimite()
        );
        if (organizacion != null) convocatoria.asignarOrganizacion(organizacion);
        return convertirARespuesta(convocatoriaRepository.save(convocatoria));
    }

    @Transactional
    public ConvocatoriaRespuesta cerrar(Long id) {
        Convocatoria convocatoria = buscarPorId(id);
        convocatoria.cerrar();
        return convertirARespuesta(convocatoriaRepository.save(convocatoria));
    }
    @Transactional @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "CONVOCATORIAS", entidad = "CONVOCATORIA", accion = "CERRAR") public ConvocatoriaRespuesta cerrar(Long id, Organizacion organizacion) { Convocatoria c=convocatoriaRepository.findByIdAndOrganizacionId(id,organizacion.getId()).orElseThrow(()->new ExcepcionNoEncontrado("Convocatoria no encontrada con id "+id)); c.cerrar(); return convertirARespuesta(convocatoriaRepository.save(c)); }

    @Transactional
    public ConvocatoriaRespuesta cancelar(Long id) {
        Convocatoria convocatoria = buscarPorId(id);
        convocatoria.cancelar();
        return convertirARespuesta(convocatoriaRepository.save(convocatoria));
    }
    @Transactional @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "CONVOCATORIAS", entidad = "CONVOCATORIA", accion = "CANCELAR") public ConvocatoriaRespuesta cancelar(Long id, Organizacion organizacion) { Convocatoria c=convocatoriaRepository.findByIdAndOrganizacionId(id,organizacion.getId()).orElseThrow(()->new ExcepcionNoEncontrado("Convocatoria no encontrada con id "+id)); c.cancelar(); return convertirARespuesta(convocatoriaRepository.save(c)); }

    private Convocatoria buscarPorId(Long id) {
        return convocatoriaRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado(
                        "Convocatoria no encontrada con id " + id
                ));
    }

    private void validarEventoParaConvocatoria(Evento evento, LocalDate fechaLimite) {
        if (evento.getEstado() == EstadoEvento.CANCELADO
                || evento.getEstado() == EstadoEvento.FINALIZADO) {
            throw new IllegalArgumentException(
                    "No se puede crear una convocatoria para un evento cancelado o finalizado"
            );
        }
        if (fechaLimite.isAfter(evento.getFechaInicio().toLocalDate())) {
            throw new IllegalArgumentException(
                    "La fecha limite no puede ser posterior al inicio del evento"
            );
        }
    }

    private ConvocatoriaRespuesta convertirARespuesta(Convocatoria convocatoria) {
        return new ConvocatoriaRespuesta(
                convocatoria.getId(),
                convocatoria.getEvento().getId(),
                convocatoria.getEvento().getNombre(),
                convocatoria.getTipoPersonal(),
                convocatoria.getDescripcion(),
                convocatoria.getCantidadRequerida(),
                convocatoria.getFechaLimite(),
                convocatoria.getEstado(),
                convocatoria.getFechaCreacion(),
                convocatoria.getFechaActualizacion()
        );
    }
}
