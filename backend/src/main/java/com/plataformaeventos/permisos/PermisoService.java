package com.plataformaeventos.permisos;
 
import com.plataformaeventos.permisos.dto.*;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermisoService {

    private final PermisoRepository permisoRepository;

    public PermisoService(PermisoRepository permisoRepository) {
        this.permisoRepository = permisoRepository;
    }

    @Transactional(readOnly = true)
    public List<PermisoRespuesta> listarPermisos() {
        return permisoRepository.findAll(Sort.by("nombre").ascending()).stream()
                .map(permiso -> new PermisoRespuesta(
                        permiso.getId(), permiso.getNombre(), permiso.getDescripcion()
                ))
                .toList();
    }
}
