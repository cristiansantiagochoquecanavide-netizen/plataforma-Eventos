package com.plataformaeventos.participantes.controlador;

import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.participantes.dto.CambiarEstadoParticipanteSolicitud;
import com.plataformaeventos.participantes.dto.PaginaParticipantesRespuesta;
import com.plataformaeventos.participantes.dto.ParticipanteDetalleRespuesta;
import com.plataformaeventos.participantes.dto.ParticipanteResumenRespuesta;
import com.plataformaeventos.participantes.servicio.ParticipanteService;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/participantes")
@Tag(name = "Participantes", description = "Gestión de participantes y seguimiento de inscripciones (CU10)")
@SecurityRequirement(name = "bearerAuth")
public class ParticipanteController {

    private final ParticipanteService participanteService;
    private final ContextoOrganizacion contextoOrganizacion;

    public ParticipanteController(
            ParticipanteService participanteService,
            ContextoOrganizacion contextoOrganizacion
    ) {
        this.participanteService = participanteService;
        this.contextoOrganizacion = contextoOrganizacion;
    }

    @GetMapping
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'PARTICIPANTE_VER') or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'USUARIO_VER')")
    @Operation(summary = "Listar participantes con filtros y paginación (CU10)")
    @ApiResponse(responseCode = "200", description = "Padrón de participantes consultado exitosamente")
    public ResponseEntity<PaginaParticipantesRespuesta> listarParticipantes(
            @RequestParam(required = false) Long eventoId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) EstadoUsuario estado,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamaño debe ser al menos 1")
            @Max(value = 100, message = "El tamaño no puede superar 100") int tamano,
            Authentication auth,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(participanteService.listarParticipantes(
                eventoId,
                busqueda,
                estado,
                pagina,
                tamano,
                contextoOrganizacion.resolver(auth, request)
        ));
    }

    @GetMapping("/{idParticipante}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'PARTICIPANTE_VER') or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'USUARIO_VER')")
    @Operation(summary = "Consultar detalle de un participante e historial de eventos (CU10)")
    @ApiResponse(responseCode = "200", description = "Detalle del participante consultado")
    @ApiResponse(responseCode = "404", description = "Participante no encontrado en la organización", content = @Content)
    public ResponseEntity<ParticipanteDetalleRespuesta> obtenerDetalle(
            @PathVariable Long idParticipante,
            Authentication auth,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(participanteService.obtenerDetalleParticipante(
                idParticipante,
                contextoOrganizacion.resolver(auth, request)
        ));
    }

    @PatchMapping("/{idParticipante}/estado")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'PARTICIPANTE_GESTIONAR') or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'USUARIO_CAMBIAR_ESTADO')")
    @Operation(summary = "Modificar estado de un participante (CU10)")
    @ApiResponse(responseCode = "200", description = "Estado del participante actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    @ApiResponse(responseCode = "404", description = "Participante no encontrado en la organización", content = @Content)
    public ResponseEntity<ParticipanteResumenRespuesta> cambiarEstado(
            @PathVariable Long idParticipante,
            @Valid @RequestBody CambiarEstadoParticipanteSolicitud solicitud,
            Authentication auth,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(participanteService.cambiarEstado(
                idParticipante,
                solicitud,
                contextoOrganizacion.resolver(auth, request)
        ));
    }

    @GetMapping("/exportar")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'PARTICIPANTE_VER') or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'USUARIO_VER')")
    @Operation(summary = "Exportar lista de participantes a CSV (CU10)")
    @ApiResponse(responseCode = "200", description = "Archivo CSV generado")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false) Long eventoId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) EstadoUsuario estado,
            Authentication auth,
            HttpServletRequest request
    ) {
        byte[] csv = participanteService.exportarParticipantesCsv(
                eventoId,
                busqueda,
                estado,
                contextoOrganizacion.resolver(auth, request)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"participantes.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
