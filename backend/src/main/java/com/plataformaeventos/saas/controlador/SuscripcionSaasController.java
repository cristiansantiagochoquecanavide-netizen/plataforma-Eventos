package com.plataformaeventos.saas.controlador;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.saas.dto.ActualizarSuscripcionSolicitud;
import com.plataformaeventos.saas.dto.SuscripcionOrganizacionRespuesta;
import com.plataformaeventos.saas.servicio.SuscripcionOrganizacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/saas/suscripciones")
@PreAuthorize("hasAuthority('SAAS_ADMINISTRADOR')")
public class SuscripcionSaasController {
    private final SuscripcionOrganizacionService suscripciones;
    private final OrganizacionRepository organizaciones;

    public SuscripcionSaasController(SuscripcionOrganizacionService suscripciones,
                                     OrganizacionRepository organizaciones) {
        this.suscripciones = suscripciones;
        this.organizaciones = organizaciones;
    }

    @GetMapping
    public ResponseEntity<List<SuscripcionOrganizacionRespuesta>> listar() {
        return ResponseEntity.ok(suscripciones.listar());
    }

    @PutMapping("/{idOrganizacion}")
    public ResponseEntity<SuscripcionOrganizacionRespuesta> actualizar(
            @PathVariable Long idOrganizacion,
            @Valid @RequestBody ActualizarSuscripcionSolicitud solicitud
    ) {
        Organizacion organizacion = organizaciones.findById(idOrganizacion)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Organización no encontrada"));
        return ResponseEntity.ok(suscripciones.actualizar(organizacion, solicitud));
    }
}
