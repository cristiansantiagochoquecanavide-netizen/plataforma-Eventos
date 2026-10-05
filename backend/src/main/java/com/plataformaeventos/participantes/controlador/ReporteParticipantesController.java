package com.plataformaeventos.participantes.controlador;

import com.plataformaeventos.audit.servicio.OperacionAuditable;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.participantes.dto.EnviarReporteParticipantesSolicitud;
import com.plataformaeventos.participantes.dto.FormatoReporte;
import com.plataformaeventos.participantes.dto.ReporteParticipantesSolicitud;
import com.plataformaeventos.participantes.dto.VistaPreviaReporteParticipantesRespuesta;
import com.plataformaeventos.participantes.servicio.ReporteParticipantesService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/participantes/reportes")
public class ReporteParticipantesController {
    private final ReporteParticipantesService reportes;
    private final ContextoOrganizacion contexto;

    public ReporteParticipantesController(ReporteParticipantesService reportes, ContextoOrganizacion contexto) {
        this.reportes = reportes;
        this.contexto = contexto;
    }

    @PostMapping("/vista-previa")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'PARTICIPANTE_VER') or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'USUARIO_VER')")
    public ResponseEntity<VistaPreviaReporteParticipantesRespuesta> vistaPrevia(
            @Valid @RequestBody ReporteParticipantesSolicitud solicitud,
            Authentication auth,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(reportes.vistaPrevia(solicitud, contexto.resolver(auth, request)));
    }

    @PostMapping("/exportar")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'PARTICIPANTE_VER') or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'USUARIO_VER')")
    @OperacionAuditable(modulo = "REPORTES", entidad = "PARTICIPANTES", accion = "EXPORTAR")
    public ResponseEntity<byte[]> exportar(
            @Valid @RequestBody ReporteParticipantesSolicitud solicitud,
            @RequestParam FormatoReporte formato,
            Authentication auth,
            HttpServletRequest request
    ) {
        var archivo = reportes.exportar(solicitud, formato, contexto.resolver(auth, request));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.nombre() + "\"")
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .body(archivo.contenido());
    }

    @PostMapping("/enviar")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'PARTICIPANTE_VER') or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'USUARIO_VER')")
    @OperacionAuditable(modulo = "REPORTES", entidad = "PARTICIPANTES", accion = "ENVIAR_CORREO")
    public ResponseEntity<Void> enviar(
            @Valid @RequestBody EnviarReporteParticipantesSolicitud solicitud,
            Authentication auth,
            HttpServletRequest request
    ) {
        reportes.enviar(solicitud, contexto.resolver(auth, request));
        return ResponseEntity.noContent().build();
    }
}
