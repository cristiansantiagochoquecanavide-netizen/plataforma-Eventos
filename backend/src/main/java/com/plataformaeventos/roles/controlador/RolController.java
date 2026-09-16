package com.plataformaeventos.roles.controlador;

import com.plataformaeventos.roles.dto.ActualizarRolSolicitud;
import com.plataformaeventos.roles.dto.AsignarPermisosSolicitud;
import com.plataformaeventos.roles.dto.CrearRolSolicitud;
import com.plataformaeventos.roles.dto.RolPermisosRespuesta;
import com.plataformaeventos.roles.dto.RolRespuesta;
import com.plataformaeventos.roles.servicio.RolPermisoService;
import com.plataformaeventos.roles.servicio.RolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Administracion de roles")
@SecurityRequirement(name = "bearerAuth")
public class RolController {

    private final RolService rolService;
    private final RolPermisoService rolPermisoService;

    public RolController(RolService rolService, RolPermisoService rolPermisoService) {
        this.rolService = rolService;
        this.rolPermisoService = rolPermisoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SAAS_ADMINISTRADOR')")
    @Operation(summary = "Listar roles")
    @ApiResponse(responseCode = "200", description = "Roles consultados")
    public ResponseEntity<List<RolRespuesta>> listarRoles() {
        return ResponseEntity.ok(rolService.listarRoles());
    }

    @GetMapping("/{idRol}")
    @PreAuthorize("hasAuthority('SAAS_ADMINISTRADOR')")
    @Operation(summary = "Consultar rol por identificador")
    @ApiResponse(responseCode = "200", description = "Rol consultado")
    @ApiResponse(responseCode = "404", description = "Rol no encontrado", content = @Content)
    public ResponseEntity<RolRespuesta> obtenerRol(@PathVariable Long idRol) {
        return ResponseEntity.ok(rolService.obtenerRol(idRol));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SAAS_ADMINISTRADOR')")
    @Operation(summary = "Crear rol")
    @ApiResponse(responseCode = "201", description = "Rol creado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos o nombre duplicado", content = @Content)
    public ResponseEntity<RolRespuesta> crearRol(@Valid @RequestBody CrearRolSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crearRol(solicitud));
    }

    @PutMapping("/{idRol}")
    @PreAuthorize("hasAuthority('SAAS_ADMINISTRADOR')")
    @Operation(summary = "Actualizar rol")
    @ApiResponse(responseCode = "200", description = "Rol actualizado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos o cambio no permitido", content = @Content)
    @ApiResponse(responseCode = "404", description = "Rol no encontrado", content = @Content)
    public ResponseEntity<RolRespuesta> actualizarRol(
            @PathVariable Long idRol,
            @Valid @RequestBody ActualizarRolSolicitud solicitud
    ) {
        return ResponseEntity.ok(rolService.actualizarRol(idRol, solicitud));
    }

    @GetMapping("/{idRol}/permisos")
    @PreAuthorize("hasAuthority('SAAS_ADMINISTRADOR')")
    @Operation(summary = "Consultar permisos de un rol")
    @ApiResponse(responseCode = "200", description = "Permisos del rol consultados")
    @ApiResponse(responseCode = "404", description = "Rol no encontrado", content = @Content)
    public ResponseEntity<RolPermisosRespuesta> obtenerPermisos(@PathVariable Long idRol) {
        return ResponseEntity.ok(rolPermisoService.obtenerPermisos(idRol));
    }

    @PutMapping("/{idRol}/permisos")
    @PreAuthorize("hasAuthority('SAAS_ADMINISTRADOR')")
    @Operation(summary = "Asignar permisos a un rol", description = "Reemplaza la asignacion actual de permisos del rol.")
    @ApiResponse(responseCode = "200", description = "Permisos asignados")
    @ApiResponse(responseCode = "400", description = "Permisos invalidos", content = @Content)
    @ApiResponse(responseCode = "404", description = "Rol no encontrado", content = @Content)
    public ResponseEntity<RolPermisosRespuesta> asignarPermisos(
            @PathVariable Long idRol,
            @Valid @RequestBody AsignarPermisosSolicitud solicitud
    ) {
        return ResponseEntity.ok(rolPermisoService.asignarPermisos(idRol, solicitud));
    }
}
