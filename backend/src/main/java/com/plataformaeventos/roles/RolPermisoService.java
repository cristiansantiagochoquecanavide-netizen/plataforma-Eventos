package com.plataformaeventos.roles;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.permisos.Permiso;
import com.plataformaeventos.permisos.PermisoRepository;
import com.plataformaeventos.permisos.PermisoRespuesta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RolPermisoService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;

    public RolPermisoService(RolRepository rolRepository, PermisoRepository permisoRepository) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
    }

    @Transactional(readOnly = true)
    public RolPermisosRespuesta obtenerPermisos(Long idRol) {
        return convertirRol(buscarRolConPermisos(idRol));
    }

    @Transactional
    public RolPermisosRespuesta asignarPermisos(Long idRol, AsignarPermisosSolicitud solicitud) {
        Rol rol = buscarRolConPermisos(idRol);
        Set<String> nombres = solicitud.permisos().stream()
                .map(nombre -> nombre.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<Permiso> permisos = permisoRepository.findByNombreIn(nombres);
        if (permisos.size() != nombres.size()) {
            throw new IllegalArgumentException("Uno o mas permisos no existen");
        }
        rol.asignarPermisos(Set.copyOf(permisos));
        return convertirRol(rol);
    }

    private Rol buscarRolConPermisos(Long idRol) {
        return rolRepository.findByIdWithPermisos(idRol)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Rol no encontrado"));
    }

    private RolPermisosRespuesta convertirRol(Rol rol) {
        List<PermisoRespuesta> permisos = rol.getPermisos().stream()
                .map(permiso -> new PermisoRespuesta(
                        permiso.getId(), permiso.getNombre(), permiso.getDescripcion()
                ))
                .sorted(java.util.Comparator.comparing(PermisoRespuesta::nombre))
                .toList();
        return new RolPermisosRespuesta(rol.getId(), rol.getNombre(), permisos);
    }
}
