package com.plataformaeventos.convocatorias;

import com.plataformaeventos.convocatorias.dto.ConvocatoriaRespuesta;
import com.plataformaeventos.convocatorias.dto.CrearConvocatoriaSolicitud;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CU15: Gestion de Convocatorias de Staff y Voluntarios.
 *
 * Permite registrar una convocatoria asociada obligatoriamente a un evento,
 * indicando el tipo de personal, descripcion, cantidad requerida y fecha limite.
 * Expone consultas con filtros y conserva la trazabilidad mediante los estados
 * ABIERTA, CERRADA y CANCELADA. Solo usuarios autorizados pueden crear, cerrar
 * o cancelar; los datos obligatorios se validan antes de llegar al servicio.
 */
@RestController
@RequestMapping("/api/convocatorias")
@Tag(name = "Convocatorias", description = "CU15 - Gestion de convocatorias de staff y voluntarios")
@SecurityRequirement(name = "bearerAuth")
public class ConvocatoriaController {

    private final ConvocatoriaService convocatoriaService;

    public ConvocatoriaController(ConvocatoriaService convocatoriaService) {
        this.convocatoriaService = convocatoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('CONVOCATORIA_VER', 'ROLE_ADMINISTRADOR', 'ROLE_ORGANIZADOR')")
    @Operation(summary = "Consultar convocatorias", description = "Lista convocatorias con filtros opcionales.")
    @ApiResponse(responseCode = "200", description = "Convocatorias obtenidas")
    public ResponseEntity<List<ConvocatoriaRespuesta>> listar(
            @RequestParam(required = false) Long idEvento,
            @RequestParam(required = false) TipoPersonal tipoPersonal,
            @RequestParam(required = false) EstadoConvocatoria estado,
            @RequestParam(required = false) String termino
    ) {
        return ResponseEntity.ok(
                convocatoriaService.listar(idEvento, tipoPersonal, estado, termino)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIA_VER', 'ROLE_ADMINISTRADOR', 'ROLE_ORGANIZADOR')")
    @Operation(summary = "Consultar una convocatoria")
    @ApiResponse(responseCode = "200", description = "Convocatoria obtenida")
    @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada")
    public ResponseEntity<ConvocatoriaRespuesta> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(convocatoriaService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CONVOCATORIA_CREAR', 'ROLE_ADMINISTRADOR')")
    @Operation(summary = "Registrar convocatoria")
    @ApiResponse(responseCode = "201", description = "Convocatoria registrada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    public ResponseEntity<ConvocatoriaRespuesta> crear(
            @Valid @RequestBody CrearConvocatoriaSolicitud solicitud
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(convocatoriaService.crear(solicitud));
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIA_CERRAR', 'ROLE_ADMINISTRADOR')")
    @Operation(summary = "Cerrar convocatoria")
    @ApiResponse(responseCode = "200", description = "Convocatoria cerrada")
    @ApiResponse(responseCode = "400", description = "Transicion de estado invalida")
    public ResponseEntity<ConvocatoriaRespuesta> cerrar(@PathVariable Long id) {
        return ResponseEntity.ok(convocatoriaService.cerrar(id));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIA_CANCELAR', 'ROLE_ADMINISTRADOR')")
    @Operation(summary = "Cancelar convocatoria")
    @ApiResponse(responseCode = "200", description = "Convocatoria cancelada")
    @ApiResponse(responseCode = "400", description = "Transicion de estado invalida")
    public ResponseEntity<ConvocatoriaRespuesta> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(convocatoriaService.cancelar(id));
    }
}
