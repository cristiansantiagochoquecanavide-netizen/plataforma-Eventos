package com.plataformaeventos.permisos.controlador;

import com.plataformaeventos.permisos.dto.PermisoRespuesta;
import com.plataformaeventos.permisos.servicio.PermisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permisos")
@Tag(name = "Permisos", description = "Catalogo de permisos del sistema")
@SecurityRequirement(name = "bearerAuth")
public class PermisoController {

    private final PermisoService permisoService;

    public PermisoController(PermisoService permisoService) {
        this.permisoService = permisoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISO_VER') or hasAuthority('SAAS_ADMINISTRADOR')")
    @Operation(summary = "Listar permisos")
    @ApiResponse(responseCode = "200", description = "Permisos consultados")
    public ResponseEntity<List<PermisoRespuesta>> listarPermisos() {
        return ResponseEntity.ok(permisoService.listarPermisos());
    }
}
