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
        return convertir(espacioRepository.save(new Espacio(
                solicitud.nombre().trim(), solicitud.ubicacion().trim(), solicitud.capacidad())));
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "ESPACIOS", entidad = "ESPACIO", accion = "CREAR")
    public EspacioRespuesta crear(GuardarEspacioSolicitud solicitud, Organizacion organizacion) {
        Espacio espacio = new Espacio(solicitud.nombre().trim(), solicitud.ubicacion().trim(), solicitud.capacidad());
        espacio.asignarOrganizacion(organizacion);
        return convertir(espacioRepository.save(espacio));
    }

    @Transactional
    public EspacioRespuesta actualizar(Long id, GuardarEspacioSolicitud solicitud) {
        Espacio espacio = espacioRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Espacio no encontrado"));
        espacio.actualizarDatos(solicitud.nombre().trim(), solicitud.ubicacion().trim(), solicitud.capacidad());
        return convertir(espacio);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "ESPACIOS", entidad = "ESPACIO", accion = "ACTUALIZAR")
    public EspacioRespuesta actualizar(Long id, GuardarEspacioSolicitud solicitud, Organizacion organizacion) {
        Espacio espacio = espacioRepository.findByIdAndOrganizacionId(id, organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Espacio no encontrado"));
        espacio.actualizarDatos(solicitud.nombre().trim(), solicitud.ubicacion().trim(), solicitud.capacidad());
        return convertir(espacio);
    }

    private EspacioRespuesta convertir(Espacio espacio) {
        return new EspacioRespuesta(espacio.getId(), espacio.getNombre(), espacio.getUbicacion(), espacio.getCapacidad());
    }
}
