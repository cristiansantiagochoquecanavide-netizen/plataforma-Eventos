package com.plataformaeventos.recursos.servicio;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.recursos.dto.ActualizarRecursoSolicitud;
import com.plataformaeventos.recursos.dto.CambiarEstadoRecursoSolicitud;
import com.plataformaeventos.recursos.dto.CrearRecursoSolicitud;
import com.plataformaeventos.recursos.dto.RecursoRespuesta;
import com.plataformaeventos.recursos.modelo.EstadoRecurso;
import com.plataformaeventos.recursos.modelo.Recurso;
import com.plataformaeventos.recursos.modelo.TipoRecurso;
import com.plataformaeventos.recursos.repositorio.RecursoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.plataformaeventos.organizaciones.modelo.Organizacion;

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
        return listarRecursos(termino, tipo, estado, null);
    }
    @Transactional(readOnly = true)
    public List<RecursoRespuesta> listarRecursos(String termino, TipoRecurso tipo, EstadoRecurso estado, Organizacion organizacion) {
        String terminoNormalizado = termino != null ? termino.trim() : null;
        Sort orden = Sort.by(Sort.Direction.ASC, "nombre");
        return (organizacion == null ? recursoRepository.buscarConFiltros(terminoNormalizado, tipo, estado, orden) : recursoRepository.buscarConFiltrosTenant(terminoNormalizado, tipo, estado, organizacion.getId(), orden))
                .stream()
                .map(this::convertirARespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecursoRespuesta obtenerRecursoPorId(Long id) {
        return obtenerRecursoPorId(id, null);
    }
    @Transactional(readOnly = true)
    public RecursoRespuesta obtenerRecursoPorId(Long id, Organizacion organizacion) {
        Recurso recurso = (organizacion == null ? recursoRepository.findById(id) : recursoRepository.findByIdAndOrganizacionId(id, organizacion.getId()))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Recurso no encontrado con id " + id));
        return convertirARespuesta(recurso);
    }

    @Transactional
    public RecursoRespuesta crearRecurso(CrearRecursoSolicitud solicitud) {
        return crearRecurso(solicitud, null);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "RECURSOS", entidad = "RECURSO", accion = "CREAR")
    public RecursoRespuesta crearRecurso(CrearRecursoSolicitud solicitud, Organizacion organizacion) {
        String codigoNormalizado = normalizarCodigo(solicitud.codigo());
        if ((organizacion == null ? recursoRepository.existsByCodigo(codigoNormalizado) : recursoRepository.existsByOrganizacionIdAndCodigo(organizacion.getId(), codigoNormalizado))) {
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
        aplicarCostoUnitario(recurso, solicitud.costoUnitario());
        recurso.asignarOrganizacion(organizacion);

        return convertirARespuesta(recursoRepository.save(recurso));
    }

    @Transactional
    public RecursoRespuesta actualizarRecurso(Long id, ActualizarRecursoSolicitud solicitud) {
        return actualizarRecurso(id, solicitud, null);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "RECURSOS", entidad = "RECURSO", accion = "ACTUALIZAR")
    public RecursoRespuesta actualizarRecurso(Long id, ActualizarRecursoSolicitud solicitud, Organizacion organizacion) {
        Recurso recurso = (organizacion == null ? recursoRepository.findById(id) : recursoRepository.findByIdAndOrganizacionId(id, organizacion.getId()))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Recurso no encontrado con id " + id));

        String codigoNormalizado = normalizarCodigo(solicitud.codigo());
        if ((organizacion == null ? recursoRepository.existsByCodigoAndIdNot(codigoNormalizado, id) : recursoRepository.existsByOrganizacionIdAndCodigoAndIdNot(organizacion.getId(), codigoNormalizado, id))) {
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
        aplicarCostoUnitario(recurso, solicitud.costoUnitario());

        return convertirARespuesta(recursoRepository.save(recurso));
    }

    @Transactional
    public RecursoRespuesta cambiarEstado(Long id, CambiarEstadoRecursoSolicitud solicitud) {
        return cambiarEstado(id, solicitud, null);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "RECURSOS", entidad = "RECURSO", accion = "CAMBIAR_ESTADO")
    public RecursoRespuesta cambiarEstado(Long id, CambiarEstadoRecursoSolicitud solicitud, Organizacion organizacion) {
        Recurso recurso = (organizacion == null ? recursoRepository.findById(id) : recursoRepository.findByIdAndOrganizacionId(id, organizacion.getId()))
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

    private void aplicarCostoUnitario(Recurso recurso, java.math.BigDecimal costo) {
        if (costo != null) {
            if (costo.compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El costo unitario del recurso no puede ser negativo");
            }
            recurso.setCostoUnitario(costo);
        } else if (recurso.getCostoUnitario() == null) {
            recurso.setCostoUnitario(java.math.BigDecimal.ZERO);
        }
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
                recurso.getCostoUnitario(),
                recurso.getFechaCreacion(),
                recurso.getFechaActualizacion()
        );
    }
}
