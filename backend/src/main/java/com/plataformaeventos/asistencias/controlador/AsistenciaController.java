package com.plataformaeventos.asistencias.controlador;

import com.plataformaeventos.asistencias.dto.AsistenciaRespuesta;
import com.plataformaeventos.asistencias.dto.InscripcionElegibleAsistenciaRespuesta;
import com.plataformaeventos.asistencias.dto.InscripcionQrValidadaRespuesta;
import com.plataformaeventos.asistencias.dto.RegistrarAsistenciaSolicitud;
import com.plataformaeventos.asistencias.dto.ValidarCodigoQrSolicitud;
import com.plataformaeventos.asistencias.servicio.AsistenciaService;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/asistencias")
public class AsistenciaController {
    private final AsistenciaService asistencias;
    private final ContextoOrganizacion contexto;

    public AsistenciaController(AsistenciaService asistencias, ContextoOrganizacion contexto) {
        this.asistencias = asistencias;
        this.contexto = contexto;
    }

    @PostMapping("/check-in")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ASISTENCIA_REGISTRAR')")
    public ResponseEntity<AsistenciaRespuesta> registrar(@Valid @RequestBody RegistrarAsistenciaSolicitud solicitud,
                                                         Authentication autenticacion,
                                                         HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asistencias.registrar(
                solicitud.idInscripcion(), autenticacion.getName(), contexto.resolver(autenticacion, request)));
    }

    @PostMapping("/validar-qr")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ASISTENCIA_REGISTRAR')")
    public ResponseEntity<InscripcionQrValidadaRespuesta> validarQr(@Valid @RequestBody ValidarCodigoQrSolicitud solicitud,
                                                                      Authentication autenticacion,
                                                                      HttpServletRequest request) {
        return ResponseEntity.ok(asistencias.validarCodigoQr(
                solicitud.codigoQr(), autenticacion.getName(), contexto.resolver(autenticacion, request)));
    }

    @GetMapping("/inscripciones-elegibles")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ASISTENCIA_REGISTRAR')")
    public ResponseEntity<List<InscripcionElegibleAsistenciaRespuesta>> listarInscripcionesElegibles(
            Authentication autenticacion, HttpServletRequest request) {
        return ResponseEntity.ok(asistencias.listarInscripcionesElegibles(
                autenticacion.getName(), contexto.resolver(autenticacion, request)));
    }
}
