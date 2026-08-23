package com.plataformaeventos.perfil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
@Tag(name = "Perfil", description = "Consulta y actualizacion del perfil propio")
@SecurityRequirement(name = "bearerAuth")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERFIL_VER')")
    @Operation(summary = "Consultar perfil propio")
    @ApiResponse(responseCode = "200", description = "Perfil consultado")
    @ApiResponse(responseCode = "401", description = "JWT invalido o ausente", content = @Content)
    @ApiResponse(responseCode = "403", description = "Sin permiso para consultar el perfil", content = @Content)
    public ResponseEntity<PerfilRespuesta> obtenerPerfil(Authentication autenticacion) {
        return ResponseEntity.ok(perfilService.obtenerPerfil(autenticacion.getName()));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PERFIL_EDITAR')")
    @Operation(summary = "Actualizar perfil propio")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos o correo ya registrado", content = @Content)
    @ApiResponse(responseCode = "401", description = "JWT invalido o ausente", content = @Content)
    @ApiResponse(responseCode = "403", description = "Sin permiso para editar el perfil", content = @Content)
    public ResponseEntity<PerfilRespuesta> actualizarPerfil(
            Authentication autenticacion,
            @Valid @RequestBody ActualizarPerfilSolicitud solicitud
    ) {
        return ResponseEntity.ok(perfilService.actualizarPerfil(autenticacion.getName(), solicitud));
    }

    @PatchMapping("/clave")
    @PreAuthorize("hasAuthority('PERFIL_EDITAR')")
    @Operation(summary = "Cambiar clave propia", description = "Exige la clave actual y revoca las sesiones existentes.")
    @ApiResponse(responseCode = "204", description = "Clave actualizada")
    @ApiResponse(responseCode = "400", description = "Clave actual o nueva clave invalidas", content = @Content)
    @ApiResponse(responseCode = "401", description = "JWT invalido o ausente", content = @Content)
    @ApiResponse(responseCode = "403", description = "Sin permiso para editar el perfil", content = @Content)
    public ResponseEntity<Void> cambiarClave(
            Authentication autenticacion,
            @Valid @RequestBody CambiarClaveSolicitud solicitud
    ) {
        perfilService.cambiarClave(autenticacion.getName(), solicitud);
        return ResponseEntity.noContent().build();
    }
}
