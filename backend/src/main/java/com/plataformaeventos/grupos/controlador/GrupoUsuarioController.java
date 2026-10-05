package com.plataformaeventos.grupos.controlador;

import com.plataformaeventos.grupos.dto.ActualizarGrupoUsuarioSolicitud;
import com.plataformaeventos.grupos.dto.AsignarMiembrosGrupoSolicitud;
import com.plataformaeventos.grupos.dto.AsignarRolesGrupoSolicitud;
import com.plataformaeventos.grupos.dto.CrearGrupoUsuarioSolicitud;
import com.plataformaeventos.grupos.dto.GrupoUsuarioRespuesta;
import com.plataformaeventos.grupos.dto.RolGrupoRespuesta;
import com.plataformaeventos.grupos.servicio.GrupoUsuarioService;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/grupos")
public class GrupoUsuarioController {
    private final GrupoUsuarioService servicio;
    private final ContextoOrganizacion contexto;

    public GrupoUsuarioController(GrupoUsuarioService servicio, ContextoOrganizacion contexto) {
        this.servicio = servicio;
        this.contexto = contexto;
    }

    @GetMapping
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<List<GrupoUsuarioRespuesta>> listar(Authentication authentication, HttpServletRequest request) {
        return ResponseEntity.ok(servicio.listar(organizacionActiva(authentication, request)));
    }

    @GetMapping("/roles-disponibles")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<List<RolGrupoRespuesta>> listarRolesDisponibles(Authentication authentication, HttpServletRequest request) {
        organizacionActiva(authentication, request);
        return ResponseEntity.ok(servicio.listarRolesDisponibles());
    }

    @GetMapping("/{idGrupo}")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<GrupoUsuarioRespuesta> obtener(@PathVariable Long idGrupo, Authentication authentication, HttpServletRequest request) {
        return ResponseEntity.ok(servicio.obtener(organizacionActiva(authentication, request), idGrupo));
    }

    @PostMapping
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<GrupoUsuarioRespuesta> crear(@Valid @RequestBody CrearGrupoUsuarioSolicitud solicitud,
                                                       Authentication authentication,
                                                       HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(organizacionActiva(authentication, request), solicitud));
    }

    @PutMapping("/{idGrupo}")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<GrupoUsuarioRespuesta> actualizar(@PathVariable Long idGrupo,
                                                            @Valid @RequestBody ActualizarGrupoUsuarioSolicitud solicitud,
                                                            Authentication authentication,
                                                            HttpServletRequest request) {
        return ResponseEntity.ok(servicio.actualizar(organizacionActiva(authentication, request), idGrupo, solicitud));
    }

    @PatchMapping("/{idGrupo}/desactivar")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<GrupoUsuarioRespuesta> desactivar(@PathVariable Long idGrupo, Authentication authentication, HttpServletRequest request) {
        return ResponseEntity.ok(servicio.desactivar(organizacionActiva(authentication, request), idGrupo));
    }

    @PatchMapping("/{idGrupo}/activar")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<GrupoUsuarioRespuesta> activar(@PathVariable Long idGrupo, Authentication authentication, HttpServletRequest request) {
        return ResponseEntity.ok(servicio.activar(organizacionActiva(authentication, request), idGrupo));
    }

    @PutMapping("/{idGrupo}/miembros")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<GrupoUsuarioRespuesta> asignarMiembros(@PathVariable Long idGrupo,
                                                                 @Valid @RequestBody AsignarMiembrosGrupoSolicitud solicitud,
                                                                 Authentication authentication,
                                                                 HttpServletRequest request) {
        return ResponseEntity.ok(servicio.asignarMiembros(organizacionActiva(authentication, request), idGrupo, solicitud));
    }

    @PutMapping("/{idGrupo}/roles")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<GrupoUsuarioRespuesta> asignarRoles(@PathVariable Long idGrupo,
                                                              @Valid @RequestBody AsignarRolesGrupoSolicitud solicitud,
                                                              Authentication authentication,
                                                              HttpServletRequest request) {
        return ResponseEntity.ok(servicio.asignarRoles(organizacionActiva(authentication, request), idGrupo, solicitud));
    }

    private Organizacion organizacionActiva(Authentication authentication, HttpServletRequest request) {
        return contexto.resolver(authentication, request);
    }
}
