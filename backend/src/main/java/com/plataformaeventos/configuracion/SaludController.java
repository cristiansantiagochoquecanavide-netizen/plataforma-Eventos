package com.plataformaeventos.configuracion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salud")
@Tag(name = "Salud", description = "Estado basico de la API")
public class SaludController {

    @GetMapping
    @Operation(summary = "Consultar estado de la API")
    @ApiResponse(responseCode = "200", description = "API disponible")
    public ResponseEntity<SaludRespuesta> obtenerSalud() {
        return ResponseEntity.ok(new SaludRespuesta("ok", "backend"));
    }

    public record SaludRespuesta(String estado, String servicio) {
    }
}
