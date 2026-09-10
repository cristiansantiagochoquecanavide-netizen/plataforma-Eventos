package com.plataformaeventos.espacios.controlador;

import com.plataformaeventos.espacios.dto.EspacioRespuesta;
import com.plataformaeventos.espacios.dto.GuardarEspacioSolicitud;
import com.plataformaeventos.espacios.servicio.EspacioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
public class EspacioController {

    private final EspacioService espacioService;

    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ESPACIO_VER')")
    public ResponseEntity<List<EspacioRespuesta>> listar() {
        return ResponseEntity.ok(espacioService.listar());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ESPACIO_CREAR')")
    public ResponseEntity<EspacioRespuesta> crear(@Valid @RequestBody GuardarEspacioSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(espacioService.crear(solicitud));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ESPACIO_EDITAR')")
    public ResponseEntity<EspacioRespuesta> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody GuardarEspacioSolicitud solicitud
    ) {
        return ResponseEntity.ok(espacioService.actualizar(id, solicitud));
    }
}
