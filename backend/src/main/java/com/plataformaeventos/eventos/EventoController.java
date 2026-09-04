package com.plataformaeventos.eventos;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Gestion de eventos y responsables")
@SecurityRequirement(name = "bearerAuth")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar eventos activos")
    @ApiResponse(responseCode = "200", description = "Eventos consultados")
    public ResponseEntity<List<EventoRespuesta>> listarEventos(
            @RequestParam(required = false) String termino,
            @RequestParam(required = false) String categoria
    ) {
        return ResponseEntity.ok(eventoService.listarEventos(termino, categoria));
    }

    @GetMapping("/responsables-disponibles")
    @PreAuthorize("hasAuthority('EVENTO_ASIGNAR_RESPONSABLE')")
    @Operation(summary = "Listar responsables disponibles")
    @ApiResponse(responseCode = "200", description = "Responsables disponibles consultados")
    public ResponseEntity<List<ResponsableEventoRespuesta>> listarResponsablesDisponibles() {
        return ResponseEntity.ok(eventoService.listarResponsablesDisponibles());
    }

    @GetMapping("/{idEvento}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar evento por identificador")
    @ApiResponse(responseCode = "200", description = "Evento consultado")
    @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content)
    public ResponseEntity<EventoRespuesta> obtenerEvento(@PathVariable Long idEvento) {
        return ResponseEntity.ok(eventoService.obtenerEvento(idEvento));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EVENTO_CREAR')")
    @Operation(summary = "Crear evento")
    @ApiResponse(responseCode = "201", description = "Evento creado")
    @ApiResponse(responseCode = "400", description = "Datos o responsables invalidos", content = @Content)
    public ResponseEntity<EventoRespuesta> crearEvento(@Valid @RequestBody GuardarEventoSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crearEvento(solicitud));
    }

    @PutMapping("/{idEvento}")
    @PreAuthorize("hasAuthority('EVENTO_EDITAR')")
    @Operation(summary = "Actualizar evento")
    @ApiResponse(responseCode = "200", description = "Evento actualizado")
    @ApiResponse(responseCode = "400", description = "Datos o responsables invalidos", content = @Content)
    @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content)
    public ResponseEntity<EventoRespuesta> actualizarEvento(
            @PathVariable Long idEvento,
            @Valid @RequestBody GuardarEventoSolicitud solicitud
    ) {
        return ResponseEntity.ok(eventoService.actualizarEvento(idEvento, solicitud));
    }

    @PutMapping("/{idEvento}/responsables")
    @PreAuthorize("hasAuthority('EVENTO_ASIGNAR_RESPONSABLE')")
    @Operation(summary = "Asignar responsables a evento")
    @ApiResponse(responseCode = "200", description = "Responsables asignados")
    @ApiResponse(responseCode = "400", description = "Responsables invalidos", content = @Content)
    @ApiResponse(responseCode = "404", description = "Evento o responsable no encontrado", content = @Content)
    public ResponseEntity<EventoRespuesta> asignarResponsables(
            @PathVariable Long idEvento,
            @Valid @RequestBody AsignarResponsablesSolicitud solicitud
    ) {
        return ResponseEntity.ok(eventoService.asignarResponsables(idEvento, solicitud));
    }
}
