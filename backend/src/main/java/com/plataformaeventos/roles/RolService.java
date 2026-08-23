package com.plataformaeventos.roles;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RolService {

    private static final Set<String> ROLES_BASE = Set.of("ADMINISTRADOR", "ORGANIZADOR", "STAFF", "PARTICIPANTE");

    private final RolRepository rolRepository;

    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Transactional(readOnly = true)
    public List<RolRespuesta> listarRoles() {
        return rolRepository.findAll(Sort.by("nombre").ascending()).stream()
                .map(this::convertirRol)
                .toList();
    }

    @Transactional(readOnly = true)
    public RolRespuesta obtenerRol(Long idRol) {
        return convertirRol(buscarRol(idRol));
    }

    @Transactional
    public RolRespuesta crearRol(CrearRolSolicitud solicitud) {
        String nombre = normalizarNombre(solicitud.nombre());
        if (rolRepository.findByNombre(nombre).isPresent()) {
            throw new IllegalArgumentException("El nombre del rol ya se encuentra registrado");
        }
        Rol rol = new Rol(nombre, normalizarDescripcion(solicitud.descripcion()), EstadoRol.ACTIVO);
        return convertirRol(rolRepository.save(rol));
    }

    @Transactional
    public RolRespuesta actualizarRol(Long idRol, ActualizarRolSolicitud solicitud) {
        Rol rol = buscarRol(idRol);
        String nombre = normalizarNombre(solicitud.nombre());
        if (ROLES_BASE.contains(rol.getNombre())
                && (!rol.getNombre().equals(nombre) || rol.getEstado() != solicitud.estado())) {
            throw new IllegalArgumentException("El nombre y estado de un rol base no pueden modificarse");
        }
        if (!rol.getNombre().equals(nombre) && rolRepository.findByNombre(nombre).isPresent()) {
            throw new IllegalArgumentException("El nombre del rol ya se encuentra registrado");
        }
        rol.actualizarDatos(nombre, normalizarDescripcion(solicitud.descripcion()), solicitud.estado());
        return convertirRol(rol);
    }

    private Rol buscarRol(Long idRol) {
        return rolRepository.findById(idRol)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Rol no encontrado"));
    }

    private RolRespuesta convertirRol(Rol rol) {
        return new RolRespuesta(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.getEstado().name());
    }

    private String normalizarNombre(String nombre) {
        return nombre.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        return descripcion.trim();
    }
}
