package com.plataformaeventos.saas.controlador;

import com.plataformaeventos.saas.dto.ActualizarEstadoOrganizacionSaasSolicitud;
import com.plataformaeventos.saas.dto.PaginaOrganizacionesSaasRespuesta;
import com.plataformaeventos.saas.servicio.OrganizacionSaasService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@Validated
@RequestMapping("/api/saas/organizaciones")
@PreAuthorize("hasAuthority('SAAS_ADMINISTRADOR')")
public class OrganizacionSaasController {
    private final OrganizacionSaasService servicio;

    public OrganizacionSaasController(OrganizacionSaasService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ResponseEntity<PaginaOrganizacionesSaasRespuesta> listar(
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado
    ) {
        return ResponseEntity.ok(servicio.listar(pagina, tamano, busqueda, estado));
    }

    @PatchMapping("/{idOrganizacion}/estado")
    public ResponseEntity<Void> actualizarEstado(
            @PathVariable @Min(1) Long idOrganizacion,
            @Valid @RequestBody ActualizarEstadoOrganizacionSaasSolicitud solicitud,
            Authentication authentication,
            HttpServletRequest request
    ) {
        servicio.actualizarEstado(idOrganizacion, solicitud, authentication, request);
        return ResponseEntity.noContent().build();
    }
}
