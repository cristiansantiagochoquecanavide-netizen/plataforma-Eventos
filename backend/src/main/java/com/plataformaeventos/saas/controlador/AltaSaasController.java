package com.plataformaeventos.saas.controlador;

import com.plataformaeventos.saas.dto.ActivarPropietarioSaasSolicitud;
import com.plataformaeventos.saas.dto.AltaSaasIniciadaRespuesta;
import com.plataformaeventos.saas.dto.CrearAltaSaasSolicitud;
import com.plataformaeventos.saas.dto.PlanSaasPublico;
import com.plataformaeventos.saas.dto.VerificarAltaSaasRespuesta;
import com.plataformaeventos.saas.dto.VerificarAltaSaasSolicitud;
import com.plataformaeventos.saas.servicio.AltaSaasService;
import com.plataformaeventos.saas.servicio.CatalogoPlanesSaasService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saas")
public class AltaSaasController {
    private final CatalogoPlanesSaasService catalogo;
    private final AltaSaasService altas;

    public AltaSaasController(CatalogoPlanesSaasService catalogo, AltaSaasService altas) {
        this.catalogo = catalogo;
        this.altas = altas;
    }

    @GetMapping("/planes")
    public ResponseEntity<List<PlanSaasPublico>> listarPlanes() {
        return ResponseEntity.ok(catalogo.listarPublicos());
    }

    @PostMapping("/altas")
    public ResponseEntity<AltaSaasIniciadaRespuesta> iniciarAlta(@Valid @RequestBody CrearAltaSaasSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(altas.iniciar(solicitud));
    }

    @PostMapping("/altas/verificar-email")
    public ResponseEntity<VerificarAltaSaasRespuesta> verificarEmail(
            @Valid @RequestBody VerificarAltaSaasSolicitud solicitud
    ) {
        return ResponseEntity.ok(altas.verificarCorreo(solicitud.token()));
    }

    @PostMapping("/altas/activar")
    public ResponseEntity<Void> activarPropietario(
            @Valid @RequestBody ActivarPropietarioSaasSolicitud solicitud
    ) {
        altas.activarPropietario(solicitud);
        return ResponseEntity.noContent().build();
    }
}
