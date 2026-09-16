package com.plataformaeventos.convocatorias.postulaciones;

import com.plataformaeventos.convocatorias.postulaciones.dto.PostulanteConvocatoriaRespuesta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Seleccion de postulantes", description = "CU17 - Seleccionar Staff y voluntarios")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('CONVOCATORIA_GESTIONAR_POSTULACIONES')")
public class SeleccionPostulacionesController {

    private final GestionPostulacionesService gestionService;

    public SeleccionPostulacionesController(GestionPostulacionesService gestionService) {
        this.gestionService = gestionService;
    }

    @GetMapping("/convocatorias/{id}/postulaciones")
    @Operation(summary = "Listar postulantes de una convocatoria")
    public ResponseEntity<List<PostulanteConvocatoriaRespuesta>> listar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(gestionService.listarPostulantes(id, authentication.getName()));
    }

    @PatchMapping("/postulaciones/{id}/seleccionar")
    @Operation(summary = "Seleccionar un postulante y agregarlo al equipo")
    public ResponseEntity<PostulanteConvocatoriaRespuesta> seleccionar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(gestionService.seleccionar(id, authentication.getName()));
    }

    @PatchMapping("/postulaciones/{id}/rechazar")
    @Operation(summary = "Rechazar una postulacion pendiente")
    public ResponseEntity<PostulanteConvocatoriaRespuesta> rechazar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(gestionService.rechazar(id, authentication.getName()));
    }
}
