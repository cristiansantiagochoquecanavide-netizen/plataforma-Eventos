package com.plataformaeventos.recursos;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.recursos.dto.ActualizarRecursoSolicitud;
import com.plataformaeventos.recursos.dto.CambiarEstadoRecursoSolicitud;
import com.plataformaeventos.recursos.dto.CrearRecursoSolicitud;
import com.plataformaeventos.recursos.dto.RecursoRespuesta;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class RecursoService {

    private final RecursoRepository recursoRepository;

    public RecursoService(RecursoRepository recursoRepository) {
        this.recursoRepository = recursoRepository;
    }

    @Transactional(readOnly = true)
    public List<RecursoRespuesta> listarRecursos(String termino, TipoRecurso tipo, EstadoRecurso estado) {
        String terminoNormalizado = termino != null ? termino.trim() : null;
        Sort orden = Sort.by(Sort.Direction.ASC, "nombre");
        return recursoRepository.buscarConFiltros(terminoNormalizado, tipo, estado, orden)
                .stream()
                .map(this::convertirARespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecursoRespuesta obtenerRecursoPorId(Long id) {
        Recurso recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Recurso no encontrado con id " + id));
        return convertirARespuesta(recurso);
    }

    @Transactional
    public RecursoRespuesta crearRecurso(CrearRecursoSolicitud solicitud) {
        String codigoNormalizado = normalizarCodigo(solicitud.codigo());
        if (recursoRepository.existsByCodigo(codigoNormalizado)) {
            throw new IllegalArgumentException("Ya existe un recurso con el codigo " + codigoNormalizado);
        }

        Recurso recurso = new Recurso(
                codigoNormalizado,
                solicitud.nombre().trim(),
                solicitud.tipo(),
                solicitud.cantidadTotal(),
                solicitud.cantidadTotal(), // inicialmente todo disponible
                EstadoRecurso.DISPONIBLE,
                limpiarTexto(solicitud.ubicacion()),
                limpiarTexto(solicitud.descripcion())
        );

        return convertirARespuesta(recursoRepository.save(recurso));
    }

    @Transactional
    public RecursoRespuesta actualizarRecurso(Long id, ActualizarRecursoSolicitud solicitud) {
        Recurso recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Recurso no encontrado con id " + id));

        String codigoNormalizado = normalizarCodigo(solicitud.codigo());
        if (recursoRepository.existsByCodigoAndIdNot(codigoNormalizado, id)) {
            throw new IllegalArgumentException("Ya existe otro recurso con el codigo " + codigoNormalizado);
        }

        if (solicitud.cantidadDisponible() > solicitud.cantidadTotal()) {
            throw new IllegalArgumentException("La cantidad disponible no puede ser mayor que la cantidad total");
        }

        recurso.setCodigo(codigoNormalizado);
        recurso.setNombre(solicitud.nombre().trim());
        recurso.setTipo(solicitud.tipo());
        recurso.setCantidadTotal(solicitud.cantidadTotal());
        recurso.setCantidadDisponible(solicitud.cantidadDisponible());
        recurso.setEstado(solicitud.estado());
        recurso.setUbicacion(limpiarTexto(solicitud.ubicacion()));
        recurso.setDescripcion(limpiarTexto(solicitud.descripcion()));

        return convertirARespuesta(recursoRepository.save(recurso));
    }

    @Transactional
    public RecursoRespuesta cambiarEstado(Long id, CambiarEstadoRecursoSolicitud solicitud) {
        Recurso recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Recurso no encontrado con id " + id));

        recurso.setEstado(solicitud.estado());
        return convertirARespuesta(recursoRepository.save(recurso));
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El codigo del recurso no puede estar vacio");
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private String limpiarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private RecursoRespuesta convertirARespuesta(Recurso recurso) {
        return new RecursoRespuesta(
                recurso.getId(),
                recurso.getCodigo(),
                recurso.getNombre(),
                recurso.getTipo(),
                recurso.getCantidadTotal(),
                recurso.getCantidadDisponible(),
                recurso.getEstado(),
                recurso.getUbicacion(),
                recurso.getDescripcion(),
                recurso.getFechaCreacion(),
                recurso.getFechaActualizacion()
        );
    }
}
