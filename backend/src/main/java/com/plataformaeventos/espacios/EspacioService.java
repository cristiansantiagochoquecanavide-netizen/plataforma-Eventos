package com.plataformaeventos.espacios;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
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

    @Transactional
    public EspacioRespuesta crear(GuardarEspacioSolicitud solicitud) {
        return convertir(espacioRepository.save(new Espacio(
                solicitud.nombre().trim(), solicitud.ubicacion().trim(), solicitud.capacidad())));
    }

    @Transactional
    public EspacioRespuesta actualizar(Long id, GuardarEspacioSolicitud solicitud) {
        Espacio espacio = espacioRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Espacio no encontrado"));
        espacio.actualizarDatos(solicitud.nombre().trim(), solicitud.ubicacion().trim(), solicitud.capacidad());
        return convertir(espacio);
    }

    private EspacioRespuesta convertir(Espacio espacio) {
        return new EspacioRespuesta(espacio.getId(), espacio.getNombre(), espacio.getUbicacion(), espacio.getCapacidad());
    }
}
