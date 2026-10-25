package com.plataformaeventos.organizaciones.controlador;

import com.plataformaeventos.organizaciones.dto.AgregarMembresiaSolicitud;
import com.plataformaeventos.organizaciones.dto.CrearOrganizacionSolicitud;
import com.plataformaeventos.organizaciones.dto.OrganizacionRespuesta;
import com.plataformaeventos.organizaciones.dto.MembresiaRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import com.plataformaeventos.organizaciones.servicio.OrganizacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizaciones")
public class OrganizacionController {
    private final OrganizacionService service;

    public OrganizacionController(OrganizacionService service) {
        this.service = service;
    }

    @GetMapping("/mias")
    public ResponseEntity<List<OrganizacionRespuesta>> listarMias(Authentication authentication) {
        return ResponseEntity.ok(service.listarMias(authentication.getName()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORGANIZACION_GESTIONAR') or hasAuthority('SAAS_ADMINISTRADOR')")
    public ResponseEntity<OrganizacionRespuesta> crear(@Valid @RequestBody CrearOrganizacionSolicitud solicitud,
                                                        Authentication authentication, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(solicitud, authentication.getName(), request));
    }

    @PostMapping("/{idOrganizacion}/membresias")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<Void> agregarMembresia(@PathVariable Long idOrganizacion,
                                                  @Valid @RequestBody AgregarMembresiaSolicitud solicitud, Authentication auth, HttpServletRequest request) {
        service.agregarMembresia(idOrganizacion, solicitud, auth.getName(), auth, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @GetMapping("/activa/miembros")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<List<MembresiaRespuesta>> listarMiembros(Authentication auth, HttpServletRequest request) { return ResponseEntity.ok(service.listarMiembros(auth, request)); }
    @PatchMapping("/{idOrganizacion}/membresias/{idUsuario}/estado")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<Void> cambiarEstadoMembresia(@PathVariable Long idOrganizacion, @PathVariable Long idUsuario, @RequestParam boolean activo, Authentication auth, HttpServletRequest request) {
        service.cambiarEstadoMembresia(idOrganizacion, idUsuario, activo, auth, request); return ResponseEntity.noContent().build();
    }
}
