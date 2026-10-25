package com.plataformaeventos.espacios.controlador;

import com.plataformaeventos.espacios.dto.EspacioRespuesta;
import com.plataformaeventos.espacios.dto.GuardarEspacioSolicitud;
import com.plataformaeventos.espacios.servicio.EspacioService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
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
    private final ContextoOrganizacion contexto;

    public EspacioController(EspacioService espacioService, ContextoOrganizacion contexto) {
        this.espacioService = espacioService;
        this.contexto = contexto;
    }

    @GetMapping
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ESPACIO_VER')")
    public ResponseEntity<List<EspacioRespuesta>> listar(Authentication auth, HttpServletRequest request) {
        return ResponseEntity.ok(espacioService.listar(contexto.resolver(auth, request)));
    }

    @PostMapping
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ESPACIO_CREAR')")
    public ResponseEntity<EspacioRespuesta> crear(@Valid @RequestBody GuardarEspacioSolicitud solicitud, Authentication auth, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(espacioService.crear(solicitud, contexto.resolver(auth, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ESPACIO_EDITAR')")
    public ResponseEntity<EspacioRespuesta> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody GuardarEspacioSolicitud solicitud, Authentication auth, HttpServletRequest request
    ) {
        return ResponseEntity.ok(espacioService.actualizar(id, solicitud, contexto.resolver(auth, request)));
    }
}
