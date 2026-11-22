package com.plataformaeventos.certificados.controlador;

import com.plataformaeventos.certificados.dto.VerificacionCertificadoRespuesta;
import com.plataformaeventos.certificados.servicio.CertificadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/certificados")
@Tag(name = "Verificacion Publica de Certificados", description = "Validacion publica de autenticidad de certificados sin requerir autenticacion")
public class CertificadoPublicoController {

    private final CertificadoService certificadoService;

    public CertificadoPublicoController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    @GetMapping("/verificar/{codigoVerificacion}")
    @Operation(summary = "Verificar la validez y autenticidad de un certificado digital mediante su codigo unico")
    public ResponseEntity<VerificacionCertificadoRespuesta> verificarCertificado(
            @PathVariable String codigoVerificacion
    ) {
        VerificacionCertificadoRespuesta respuesta = certificadoService.verificarPublico(codigoVerificacion);
        return ResponseEntity.ok(respuesta);
    }
}
