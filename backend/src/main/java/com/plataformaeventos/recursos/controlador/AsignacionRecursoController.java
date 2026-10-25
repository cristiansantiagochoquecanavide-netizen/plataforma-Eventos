package com.plataformaeventos.recursos.controlador;

import com.plataformaeventos.recursos.dto.AsignacionRecursoRespuesta;
import com.plataformaeventos.recursos.dto.CrearAsignacionSolicitud;
import com.plataformaeventos.recursos.modelo.EstadoAsignacion;
import com.plataformaeventos.recursos.servicio.AsignacionRecursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/asignaciones-recursos")
@Tag(name = "Asignaciones de Recursos", description = "Asignar recursos a eventos y salas (CU20)")
@SecurityRequirement(name = "bearerAuth")
public class AsignacionRecursoController {

    private final AsignacionRecursoService asignacionService;
    private final ContextoOrganizacion contexto;

    public AsignacionRecursoController(AsignacionRecursoService asignacionService, ContextoOrganizacion contexto) {
        this.asignacionService = asignacionService;
        this.contexto = contexto;
    }

    @GetMapping
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ASIGNACION_RECURSO_VER') or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'RECURSO_VER')")
    @Operation(summary = "Consultar recursos asociados a eventos y salas (CU20)")
    @ApiResponse(responseCode = "200", description = "Lista de asignaciones obtenida")
    public ResponseEntity<List<AsignacionRecursoRespuesta>> listarAsignaciones(
            @RequestParam(required = false) Long idEvento,
            @RequestParam(required = false) Long idRecurso,
            @RequestParam(required = false) EstadoAsignacion estado, Authentication auth, HttpServletRequest request
    ) {
        return ResponseEntity.ok(asignacionService.listarAsignaciones(idEvento, idRecurso, estado, contexto.resolver(auth, request).getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ASIGNACION_RECURSO_VER') or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'RECURSO_VER')")
    @Operation(summary = "Obtener detalle de una asignación")
    public ResponseEntity<AsignacionRecursoRespuesta> obtenerPorId(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        return ResponseEntity.ok(asignacionService.obtenerPorId(id, contexto.resolver(auth, request).getId()));
    }

    @PostMapping
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ASIGNACION_RECURSO_CREAR')")
    @Operation(summary = "Asignar recurso a evento y sala (CU20)")
    @ApiResponse(responseCode = "201", description = "Recurso asignado exitosamente")
    @ApiResponse(responseCode = "400", description = "Disponibilidad insuficiente o datos invalidos")
    public ResponseEntity<AsignacionRecursoRespuesta> crearAsignacion(@Valid @RequestBody CrearAsignacionSolicitud solicitud, Authentication auth, HttpServletRequest request) {
        AsignacionRecursoRespuesta creada = asignacionService.crearAsignacion(solicitud, contexto.resolver(auth, request).getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PatchMapping("/{id}/liberar")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ASIGNACION_RECURSO_LIBERAR')")
    @Operation(summary = "Devolver o liberar recurso asignado")
    @ApiResponse(responseCode = "200", description = "Recurso devuelto y stock restituido")
    @ApiResponse(responseCode = "400", description = "Asignacion no valida para devolucion")
    public ResponseEntity<AsignacionRecursoRespuesta> liberarAsignacion(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        return ResponseEntity.ok(asignacionService.liberarAsignacion(id, contexto.resolver(auth, request).getId()));
    }
}
