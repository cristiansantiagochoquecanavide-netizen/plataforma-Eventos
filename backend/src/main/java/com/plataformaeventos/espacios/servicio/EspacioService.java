package com.plataformaeventos.espacios.servicio;

import com.plataformaeventos.espacios.dto.EspacioRespuesta;
import com.plataformaeventos.espacios.dto.GuardarEspacioSolicitud;
import com.plataformaeventos.espacios.modelo.Espacio;
import com.plataformaeventos.espacios.repositorio.EspacioRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EspacioService {

    private final EspacioRepository espacioRepository;

    public EspacioService(EspacioRepository espacioRepository) {
        this.espacioRepository = espacioRepository;
    }

    @Transactional(readOnly = true)
    public List<EspacioRespuesta> listar() {
        return espacioRepository.findAllByOrderByNombreAsc().stream().map(this::convertir).toList();
    }
    @Transactional(readOnly = true)
    public List<EspacioRespuesta> listar(Organizacion organizacion) {
        return espacioRepository.findByOrganizacionIdOrderByNombreAsc(organizacion.getId()).stream().map(this::convertir).toList();
    }

    @Transactional
    public EspacioRespuesta crear(GuardarEspacioSolicitud solicitud) {
        Espacio espacio = new Espacio(
                solicitud.nombre().trim(), solicitud.ubicacion().trim(), solicitud.capacidad());
        aplicarCostoAlquiler(espacio, solicitud.costoAlquiler());
        return convertir(espacioRepository.save(espacio));
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "ESPACIOS", entidad = "ESPACIO", accion = "CREAR")
    public EspacioRespuesta crear(GuardarEspacioSolicitud solicitud, Organizacion organizacion) {
        Espacio espacio = new Espacio(solicitud.nombre().trim(), solicitud.ubicacion().trim(), solicitud.capacidad());
        aplicarCostoAlquiler(espacio, solicitud.costoAlquiler());
        espacio.asignarOrganizacion(organizacion);
        return convertir(espacioRepository.save(espacio));
    }

    @Transactional
    public EspacioRespuesta actualizar(Long id, GuardarEspacioSolicitud solicitud) {
        Espacio espacio = espacioRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Espacio no encontrado"));
        espacio.actualizarDatos(solicitud.nombre().trim(), solicitud.ubicacion().trim(), solicitud.capacidad());
        aplicarCostoAlquiler(espacio, solicitud.costoAlquiler());
        return convertir(espacio);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "ESPACIOS", entidad = "ESPACIO", accion = "ACTUALIZAR")
    public EspacioRespuesta actualizar(Long id, GuardarEspacioSolicitud solicitud, Organizacion organizacion) {
        Espacio espacio = espacioRepository.findByIdAndOrganizacionId(id, organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Espacio no encontrado"));
        espacio.actualizarDatos(solicitud.nombre().trim(), solicitud.ubicacion().trim(), solicitud.capacidad());
        aplicarCostoAlquiler(espacio, solicitud.costoAlquiler());
        return convertir(espacio);
    }

    private void aplicarCostoAlquiler(Espacio espacio, java.math.BigDecimal costo) {
        if (costo != null) {
            if (costo.compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El costo de alquiler no puede ser negativo");
            }
            espacio.setCostoAlquiler(costo);
        } else if (espacio.getCostoAlquiler() == null) {
            espacio.setCostoAlquiler(java.math.BigDecimal.ZERO);
        }
    }

    private EspacioRespuesta convertir(Espacio espacio) {
        return new EspacioRespuesta(
                espacio.getId(),
                espacio.getNombre(),
                espacio.getUbicacion(),
                espacio.getCapacidad(),
                espacio.getCostoAlquiler()
        );
    }
}
