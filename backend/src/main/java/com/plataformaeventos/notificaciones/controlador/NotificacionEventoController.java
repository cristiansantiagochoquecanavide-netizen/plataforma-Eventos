package com.plataformaeventos.notificaciones.controlador;

import com.plataformaeventos.notificaciones.dto.NotificacionEventoRespuesta;
import com.plataformaeventos.notificaciones.dto.RecordatorioMasivoRespuesta;
import com.plataformaeventos.notificaciones.dto.RecordatorioMasivoSolicitud;
import com.plataformaeventos.notificaciones.servicio.NotificacionEventoService;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones/eventos")
@SecurityRequirement(name = "bearerAuth")
public class NotificacionEventoController {
    private final NotificacionEventoService notificaciones;
    private final ContextoOrganizacion contexto;

    public NotificacionEventoController(NotificacionEventoService notificaciones, ContextoOrganizacion contexto) {
        this.notificaciones = notificaciones;
        this.contexto = contexto;
    }

    @GetMapping("/{idEvento}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_EDITAR')")
    @Operation(summary = "Consultar notificaciones del evento (CU25)")
    public ResponseEntity<List<NotificacionEventoRespuesta>> listar(
            @PathVariable Long idEvento, Authentication autenticacion, HttpServletRequest request) {
        return ResponseEntity.ok(notificaciones.listar(idEvento, contexto.resolver(autenticacion, request)));
    }

    @PostMapping("/{idEvento}/recordatorios")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_EDITAR')")
    @Operation(summary = "Programar recordatorio masivo para inscripciones confirmadas (CU25)")
    public ResponseEntity<RecordatorioMasivoRespuesta> programarRecordatorio(
            @PathVariable Long idEvento, @Valid @RequestBody RecordatorioMasivoSolicitud solicitud,
            Authentication autenticacion, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificaciones.programarMasivo(
                idEvento, contexto.resolver(autenticacion, request), solicitud));
    }

    @PostMapping("/{idEvento}/notificaciones/{idNotificacion}/reintentar")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_EDITAR')")
    @Operation(summary = "Reintentar notificación fallida de un evento (CU25)")
    public ResponseEntity<NotificacionEventoRespuesta> reintentar(
        @PathVariable Long idEvento, @PathVariable Long idNotificacion,
            Authentication autenticacion, HttpServletRequest request) {
        var organizacion = contexto.resolver(autenticacion, request);
        return ResponseEntity.ok(notificaciones.reintentar(idEvento, idNotificacion, organizacion));
    }
}
