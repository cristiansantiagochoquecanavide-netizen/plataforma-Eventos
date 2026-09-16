package com.plataformaeventos.convocatorias.postulaciones;

import com.plataformaeventos.convocatorias.postulaciones.dto.PostulacionStaffRespuesta;
import com.plataformaeventos.convocatorias.postulaciones.dto.PostularseSolicitud;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Postulaciones", description = "CU16 - Postularse a una convocatoria de staff")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('CONVOCATORIA_POSTULARSE')")
public class PostulacionStaffController {

    private final PostulacionStaffService postulacionService;

    public PostulacionStaffController(PostulacionStaffService postulacionService) {
        this.postulacionService = postulacionService;
    }

    @PostMapping("/convocatorias/{id}/postulaciones")
    @Operation(summary = "Registrar postulacion del usuario autenticado")
    @ApiResponse(responseCode = "201", description = "Postulacion registrada")
    public ResponseEntity<PostulacionStaffRespuesta> postularse(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PostularseSolicitud solicitud,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postulacionService.postularse(id, authentication.getName()));
    }

    @GetMapping("/convocatorias/{id}/postulacion")
    @Operation(summary = "Consultar la postulacion propia a una convocatoria")
    @ApiResponse(responseCode = "204", description = "El usuario no se ha postulado")
    public ResponseEntity<PostulacionStaffRespuesta> consultar(@PathVariable Long id, Authentication authentication) {
        return postulacionService.consultar(id, authentication.getName())
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/postulaciones/mias")
    @Operation(summary = "Listar postulaciones del usuario autenticado")
    public ResponseEntity<List<PostulacionStaffRespuesta>> listarMias(Authentication authentication) {
        return ResponseEntity.ok(postulacionService.listarMias(authentication.getName()));
    }

    @PatchMapping("/postulaciones/{id}/retirar")
    @Operation(summary = "Retirar una postulacion propia pendiente")
    public ResponseEntity<PostulacionStaffRespuesta> retirar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(postulacionService.retirar(id, authentication.getName()));
    }
}
