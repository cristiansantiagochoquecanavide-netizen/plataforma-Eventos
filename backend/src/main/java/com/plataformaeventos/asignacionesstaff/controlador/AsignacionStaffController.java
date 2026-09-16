package com.plataformaeventos.asignacionesstaff.controlador;

import com.plataformaeventos.asignacionesstaff.dto.*;
import com.plataformaeventos.asignacionesstaff.servicio.AsignacionStaffService;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/asignaciones-staff")
public class AsignacionStaffController {
    private final AsignacionStaffService service;
    private final ContextoOrganizacion contexto;
    private final UsuarioRepository usuarios;
    public AsignacionStaffController(AsignacionStaffService s, ContextoOrganizacion c, UsuarioRepository u) { service=s; contexto=c; usuarios=u; }

    @PostMapping
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ASIGNACION_STAFF_GESTIONAR')")
    public ResponseEntity<AsignacionStaffRespuesta> crear(@Valid @RequestBody CrearAsignacionStaffSolicitud s, Authentication a, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(s, contexto.resolver(a, request), usuario(a)));
    }

    @GetMapping
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ASIGNACION_STAFF_GESTIONAR')")
    public List<AsignacionStaffRespuesta> listar(Authentication a, HttpServletRequest request) { return service.listar(contexto.resolver(a, request).getId()); }

    @GetMapping("/mias")
    @PreAuthorize("isAuthenticated()")
    public List<AsignacionStaffRespuesta> mias(Authentication a, HttpServletRequest request) {
        return service.propias(contexto.resolver(a, request).getId(), usuario(a).getId());
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ASIGNACION_STAFF_GESTIONAR')")
    public AsignacionStaffRespuesta cancelar(@PathVariable Long id, Authentication a, HttpServletRequest request) {
        return service.cancelar(id, contexto.resolver(a, request), usuario(a));
    }

    private com.plataformaeventos.usuarios.modelo.Usuario usuario(Authentication a) { return usuarios.findByCorreo(a.getName()).orElseThrow(); }
}
