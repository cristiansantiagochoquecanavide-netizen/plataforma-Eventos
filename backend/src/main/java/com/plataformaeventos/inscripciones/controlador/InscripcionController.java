package com.plataformaeventos.inscripciones.controlador;

import com.plataformaeventos.inscripciones.dto.DisponibilidadEventoRespuesta;
import com.plataformaeventos.inscripciones.dto.InscripcionRespuesta;
import com.plataformaeventos.inscripciones.servicio.InscripcionService;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inscripciones")
@Tag(name = "Inscripciones", description = "Gestión de inscripciones a eventos para participantes")
@SecurityRequirement(name = "bearerAuth")
public class InscripcionController {

    private final InscripcionService inscripcionService;
    private final ContextoOrganizacion contextoOrganizacion;

    public InscripcionController(InscripcionService inscripcionService, ContextoOrganizacion contextoOrganizacion) {
        this.inscripcionService = inscripcionService;
        this.contextoOrganizacion = contextoOrganizacion;
    }

    @PostMapping("/eventos/{idEvento}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'INSCRIPCION_CREAR')")
    @Operation(summary = "Inscribirse a un evento (CU11)")
    @ApiResponse(responseCode = "201", description = "Inscripción realizada exitosamente")
    @ApiResponse(responseCode = "400", description = "Aforo completo, evento no publicado o duplicado", content = @Content)
    @ApiResponse(responseCode = "404", description = "Evento o usuario no encontrado", content = @Content)
    public ResponseEntity<InscripcionRespuesta> inscribirse(
            @PathVariable Long idEvento,
            Authentication autenticacion, HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inscripcionService.inscribir(idEvento, autenticacion.getName(), contextoOrganizacion.resolver(autenticacion, request)));
    }

    @GetMapping("/mis-inscripciones")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'INSCRIPCION_VER_PROPIAS')")
    @Operation(summary = "Consultar estado de inscripciones propias (CU12)")
    @ApiResponse(responseCode = "200", description = "Listado de inscripciones del usuario")
    public ResponseEntity<List<InscripcionRespuesta>> listarMisInscripciones(Authentication autenticacion, HttpServletRequest request) {
        return ResponseEntity.ok(inscripcionService.listarMisInscripciones(autenticacion.getName(), contextoOrganizacion.resolver(autenticacion, request)));
    }

    @PatchMapping("/{idInscripcion}/cancelar")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'INSCRIPCION_CANCELAR')")
    @Operation(summary = "Cancelar inscripción propia (CU13)")
    @ApiResponse(responseCode = "200", description = "Inscripción cancelada exitosamente")
    @ApiResponse(responseCode = "400", description = "Inscripción ya cancelada o evento ya iniciado", content = @Content)
    @ApiResponse(responseCode = "404", description = "Inscripción no encontrada", content = @Content)
    public ResponseEntity<InscripcionRespuesta> cancelarInscripcion(
            @PathVariable Long idInscripcion,
            Authentication autenticacion, HttpServletRequest request
    ) {
        return ResponseEntity.ok(inscripcionService.cancelarInscripcion(idInscripcion, autenticacion.getName(), contextoOrganizacion.resolver(autenticacion, request)));
    }

    @GetMapping("/eventos/{idEvento}/disponibilidad")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_VER')")
    @Operation(summary = "Consultar aforo y disponibilidad de cupos de un evento")
    @ApiResponse(responseCode = "200", description = "Disponibilidad calculada")
    @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content)
    public ResponseEntity<DisponibilidadEventoRespuesta> consultarDisponibilidad(
            @PathVariable Long idEvento,
            Authentication autenticacion, HttpServletRequest request
    ) {
        String correo = autenticacion != null ? autenticacion.getName() : null;
        return ResponseEntity.ok(inscripcionService.consultarDisponibilidad(idEvento, correo, contextoOrganizacion.resolver(autenticacion, request)));
    }
}
