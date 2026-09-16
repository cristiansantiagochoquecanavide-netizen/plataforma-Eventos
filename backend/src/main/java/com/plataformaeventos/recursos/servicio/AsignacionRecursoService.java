package com.plataformaeventos.recursos.servicio;

import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.recursos.dto.AsignacionRecursoRespuesta;
import com.plataformaeventos.recursos.dto.CrearAsignacionSolicitud;
import com.plataformaeventos.recursos.modelo.AsignacionRecurso;
import com.plataformaeventos.recursos.modelo.EstadoAsignacion;
import com.plataformaeventos.recursos.modelo.EstadoRecurso;
import com.plataformaeventos.recursos.modelo.Recurso;
import com.plataformaeventos.recursos.repositorio.AsignacionRecursoRepository;
import com.plataformaeventos.recursos.repositorio.RecursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AsignacionRecursoService {

    private final AsignacionRecursoRepository asignacionRepository;
    private final EventoRepository eventoRepository;
    private final RecursoRepository recursoRepository;

    public AsignacionRecursoService(
            AsignacionRecursoRepository asignacionRepository,
            EventoRepository eventoRepository,
            RecursoRepository recursoRepository
    ) {
        this.asignacionRepository = asignacionRepository;
        this.eventoRepository = eventoRepository;
        this.recursoRepository = recursoRepository;
    }

    @Transactional(readOnly = true)
    public List<AsignacionRecursoRespuesta> listarAsignaciones(Long idEvento, Long idRecurso, EstadoAsignacion estado) {
        return listarAsignaciones(idEvento, idRecurso, estado, null);
    }
    @Transactional(readOnly = true)
    public List<AsignacionRecursoRespuesta> listarAsignaciones(Long idEvento, Long idRecurso, EstadoAsignacion estado, Long organizacionId) {
        return (organizacionId == null ? asignacionRepository.buscarConFiltros(idEvento, idRecurso, estado) : asignacionRepository.buscarConFiltrosTenant(idEvento, idRecurso, estado, organizacionId))
                .stream()
                .map(this::convertirARespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public AsignacionRecursoRespuesta obtenerPorId(Long id) {
        return obtenerPorId(id, null);
    }
    @Transactional(readOnly = true)
    public AsignacionRecursoRespuesta obtenerPorId(Long id, Long organizacionId) {
        AsignacionRecurso asignacion = (organizacionId == null ? asignacionRepository.findById(id) : asignacionRepository.findByIdTenant(id, organizacionId))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Asignacion de recurso no encontrada con id " + id));
        return convertirARespuesta(asignacion);
    }

    @Transactional
    public AsignacionRecursoRespuesta crearAsignacion(CrearAsignacionSolicitud solicitud) {
        return crearAsignacion(solicitud, null);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "RECURSOS", entidad = "ASIGNACION_RECURSO", accion = "ASIGNAR")
    public AsignacionRecursoRespuesta crearAsignacion(CrearAsignacionSolicitud solicitud, Long organizacionId) {
        Evento evento = (organizacionId == null ? eventoRepository.findById(solicitud.idEvento()) : eventoRepository.findByIdAndOrganizacionId(solicitud.idEvento(), organizacionId))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Evento no encontrado con id " + solicitud.idEvento()));

        Recurso recurso = (organizacionId == null ? recursoRepository.findById(solicitud.idRecurso()) : recursoRepository.findByIdAndOrganizacionId(solicitud.idRecurso(), organizacionId))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Recurso no encontrado con id " + solicitud.idRecurso()));

        if (recurso.getEstado() == EstadoRecurso.DE_BAJA) {
            throw new IllegalArgumentException("No se puede asignar un recurso que ha sido dado de baja");
        }
        if (recurso.getEstado() == EstadoRecurso.MANTENIMIENTO) {
            throw new IllegalArgumentException("No se puede asignar un recurso que se encuentra en mantenimiento");
        }

        if (solicitud.cantidadAsignada() <= 0) {
            throw new IllegalArgumentException("La cantidad asignada debe ser mayor que cero");
        }

        if (solicitud.cantidadAsignada() > recurso.getCantidadDisponible()) {
            throw new IllegalArgumentException(
                    "No se puede asignar una cantidad superior a la disponibilidad existente. " +
                            "Disponible: " + recurso.getCantidadDisponible() +
                            ", Requerida: " + solicitud.cantidadAsignada()
            );
        }

        // Descontar stock disponible
        int nuevaDisponibilidad = recurso.getCantidadDisponible() - solicitud.cantidadAsignada();
        recurso.setCantidadDisponible(nuevaDisponibilidad);
        if (nuevaDisponibilidad == 0) {
            recurso.setEstado(EstadoRecurso.EN_USO);
        }
        recursoRepository.save(recurso);

        AsignacionRecurso asignacion = new AsignacionRecurso(
                evento,
                recurso,
                solicitud.salaEspacio().trim(),
                solicitud.cantidadAsignada(),
                EstadoAsignacion.ACTIVA,
                solicitud.observaciones() != null && !solicitud.observaciones().isBlank() ? solicitud.observaciones().trim() : null
        );

        return convertirARespuesta(asignacionRepository.save(asignacion));
    }

    @Transactional
    public AsignacionRecursoRespuesta liberarAsignacion(Long id) {
        return liberarAsignacion(id, null);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "RECURSOS", entidad = "ASIGNACION_RECURSO", accion = "LIBERAR")
    public AsignacionRecursoRespuesta liberarAsignacion(Long id, Long organizacionId) {
        AsignacionRecurso asignacion = (organizacionId == null ? asignacionRepository.findById(id) : asignacionRepository.findByIdTenant(id, organizacionId))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Asignacion no encontrada con id " + id));

        if (asignacion.getEstado() != EstadoAsignacion.ACTIVA) {
            throw new IllegalArgumentException("La asignacion ya se encuentra en estado " + asignacion.getEstado());
        }

        Recurso recurso = asignacion.getRecurso();
        int nuevaDisponibilidad = Math.min(recurso.getCantidadTotal(), recurso.getCantidadDisponible() + asignacion.getCantidadAsignada());
        recurso.setCantidadDisponible(nuevaDisponibilidad);

        if (recurso.getEstado() == EstadoRecurso.EN_USO && nuevaDisponibilidad > 0) {
            recurso.setEstado(EstadoRecurso.DISPONIBLE);
        }
        recursoRepository.save(recurso);

        asignacion.setEstado(EstadoAsignacion.DEVUELTA);
        asignacion.setFechaDevolucion(OffsetDateTime.now());

        return convertirARespuesta(asignacionRepository.save(asignacion));
    }

    private AsignacionRecursoRespuesta convertirARespuesta(AsignacionRecurso asignacion) {
        return new AsignacionRecursoRespuesta(
                asignacion.getId(),
                asignacion.getEvento().getId(),
                asignacion.getEvento().getNombre(),
                asignacion.getRecurso().getId(),
                asignacion.getRecurso().getCodigo(),
                asignacion.getRecurso().getNombre(),
                asignacion.getRecurso().getTipo(),
                asignacion.getSalaEspacio(),
                asignacion.getCantidadAsignada(),
                asignacion.getEstado(),
                asignacion.getObservaciones(),
                asignacion.getFechaAsignacion(),
                asignacion.getFechaDevolucion()
        );
    }
}
