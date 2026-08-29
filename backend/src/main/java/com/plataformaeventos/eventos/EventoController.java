package com.plataformaeventos.eventos;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Consulta de eventos disponibles en el sistema")
@SecurityRequirement(name = "bearerAuth")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVENTO_VER', 'RECURSO_VER', 'ROLE_ADMINISTRADOR', 'ROLE_ORGANIZADOR')")
    @Operation(summary = "Listar eventos activos")
    @ApiResponse(responseCode = "200", description = "Lista de eventos obtenida")
    public ResponseEntity<List<EventoRespuesta>> listarEventos() {
        return ResponseEntity.ok(eventoService.listarEventos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('EVENTO_VER', 'RECURSO_VER', 'ROLE_ADMINISTRADOR', 'ROLE_ORGANIZADOR')")
    @Operation(summary = "Obtener detalle de un evento")
    @ApiResponse(responseCode = "200", description = "Detalle del evento")
    @ApiResponse(responseCode = "404", description = "Evento no encontrado")
    public ResponseEntity<EventoRespuesta> obtenerEvento(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerPorId(id));
    }
}
