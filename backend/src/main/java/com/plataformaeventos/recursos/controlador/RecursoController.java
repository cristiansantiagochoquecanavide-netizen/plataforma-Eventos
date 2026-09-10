package com.plataformaeventos.recursos.controlador;

import com.plataformaeventos.recursos.dto.ActualizarRecursoSolicitud;
import com.plataformaeventos.recursos.dto.CambiarEstadoRecursoSolicitud;
import com.plataformaeventos.recursos.dto.CrearRecursoSolicitud;
import com.plataformaeventos.recursos.dto.RecursoRespuesta;
import com.plataformaeventos.recursos.modelo.EstadoRecurso;
import com.plataformaeventos.recursos.modelo.TipoRecurso;
import com.plataformaeventos.recursos.servicio.RecursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recursos")
@Tag(name = "Recursos", description = "Gestion y consulta de recursos para eventos y salas (CU19 y CU20)")
@SecurityRequirement(name = "bearerAuth")
public class RecursoController {

    private final RecursoService recursoService;

    public RecursoController(RecursoService recursoService) {
        this.recursoService = recursoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('RECURSO_VER', 'ROLE_ADMINISTRADOR', 'ROLE_ORGANIZADOR')")
    @Operation(summary = "Consultar recursos", description = "Lista recursos con filtros opcionales de busqueda por termino, tipo y estado.")
    @ApiResponse(responseCode = "200", description = "Lista de recursos obtenida")
    public ResponseEntity<List<RecursoRespuesta>> listarRecursos(
            @RequestParam(required = false) String termino,
            @RequestParam(required = false) TipoRecurso tipo,
            @RequestParam(required = false) EstadoRecurso estado
    ) {
        return ResponseEntity.ok(recursoService.listarRecursos(termino, tipo, estado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('RECURSO_VER', 'ROLE_ADMINISTRADOR', 'ROLE_ORGANIZADOR')")
    @Operation(summary = "Obtener recurso por ID")
    @ApiResponse(responseCode = "200", description = "Detalle del recurso")
    @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    public ResponseEntity<RecursoRespuesta> obtenerRecurso(@PathVariable Long id) {
        return ResponseEntity.ok(recursoService.obtenerRecursoPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('RECURSO_CREAR', 'ROLE_ADMINISTRADOR')")
    @Operation(summary = "Registrar nuevo recurso (CU19)")
    @ApiResponse(responseCode = "201", description = "Recurso registrado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos invalidos o codigo duplicado")
    public ResponseEntity<RecursoRespuesta> crearRecurso(@Valid @RequestBody CrearRecursoSolicitud solicitud) {
        RecursoRespuesta creado = recursoService.crearRecurso(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('RECURSO_EDITAR', 'ROLE_ADMINISTRADOR')")
    @Operation(summary = "Actualizar informacion del recurso")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    public ResponseEntity<RecursoRespuesta> actualizarRecurso(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarRecursoSolicitud solicitud
    ) {
        return ResponseEntity.ok(recursoService.actualizarRecurso(id, solicitud));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('RECURSO_CAMBIAR_ESTADO', 'ROLE_ADMINISTRADOR')")
    @Operation(summary = "Cambiar estado del recurso")
    @ApiResponse(responseCode = "200", description = "Estado del recurso actualizado")
    @ApiResponse(responseCode = "400", description = "Estado invalido")
    @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    public ResponseEntity<RecursoRespuesta> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRecursoSolicitud solicitud
    ) {
        return ResponseEntity.ok(recursoService.cambiarEstado(id, solicitud));
    }
}
