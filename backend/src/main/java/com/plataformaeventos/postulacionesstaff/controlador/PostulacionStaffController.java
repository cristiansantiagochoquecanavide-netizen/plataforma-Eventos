package com.plataformaeventos.postulacionesstaff.controlador;

import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.postulacionesstaff.dto.*;
import com.plataformaeventos.postulacionesstaff.servicio.PostulacionStaffService;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/postulaciones-staff")
public class PostulacionStaffController {
    private final PostulacionStaffService service; private final ContextoOrganizacion contexto; private final UsuarioRepository usuarios;
    public PostulacionStaffController(PostulacionStaffService s, ContextoOrganizacion c, UsuarioRepository u) { service=s; contexto=c; usuarios=u; }
    @PostMapping("/convocatorias/{idConvocatoria}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'POSTULACION_STAFF_CREAR')")
    public ResponseEntity<PostulacionStaffRespuesta> crear(@PathVariable Long idConvocatoria, @Valid @RequestBody CrearPostulacionStaffSolicitud solicitud, Authentication auth, HttpServletRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(idConvocatoria, auth.getName(), contexto.resolver(auth, request), solicitud)); }
    @GetMapping("/mias") @PreAuthorize("isAuthenticated()")
    public List<PostulacionStaffRespuesta> mias(Authentication a, HttpServletRequest r) { return service.propias(a.getName(), contexto.resolver(a, r).getId()); }
    @GetMapping("/convocatorias/{idConvocatoria}") @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'POSTULACION_STAFF_GESTIONAR')")
    public List<PostulacionStaffRespuesta> listar(@PathVariable Long idConvocatoria, Authentication a, HttpServletRequest request) { return service.listar(idConvocatoria, contexto.resolver(a, request).getId()); }
    @PatchMapping("/{id}/aceptar") @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'POSTULACION_STAFF_GESTIONAR')")
    public PostulacionStaffRespuesta aceptar(@PathVariable Long id, Authentication a, HttpServletRequest request) { return service.cambiar(id, contexto.resolver(a, request), true, usuario(a)); }
    @PatchMapping("/{id}/rechazar") @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'POSTULACION_STAFF_GESTIONAR')")
    public PostulacionStaffRespuesta rechazar(@PathVariable Long id, Authentication a, HttpServletRequest request) { return service.cambiar(id, contexto.resolver(a, request), false, usuario(a)); }
    private com.plataformaeventos.usuarios.modelo.Usuario usuario(Authentication a) { return usuarios.findByCorreo(a.getName()).orElseThrow(); }
}
