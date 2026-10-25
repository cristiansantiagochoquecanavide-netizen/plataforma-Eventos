package com.plataformaeventos.reportes.controlador;

import com.plataformaeventos.audit.servicio.OperacionAuditable;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.reportes.dto.ConsultaReporteIaDTO;
import com.plataformaeventos.reportes.dto.ReporteMetricasRespuesta;
import com.plataformaeventos.reportes.servicio.ReportePersonalizadoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reportes/personalizados")
public class ReportePersonalizadoController {

    private final ReportePersonalizadoService reporteService;
    private final ContextoOrganizacion contexto;

    public ReportePersonalizadoController(ReportePersonalizadoService reporteService, ContextoOrganizacion contexto) {
        this.reporteService = reporteService;
        this.contexto = contexto;
    }

    @GetMapping("/metricas")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request) or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_VER')")
    public ResponseEntity<ReporteMetricasRespuesta> obtenerMetricas(
            @RequestParam(required = false) Integer rangoDias,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String categoria,
            Authentication auth,
            HttpServletRequest request
    ) {
        Organizacion org = contexto.resolver(auth, request);
        return ResponseEntity.ok(reporteService.obtenerMetricasOrganizacion(org, rangoDias, estado, categoria));
    }

    @PostMapping("/interpretar-ia")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request) or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_VER')")
    public ResponseEntity<Map<String, Object>> interpretarConsultaIa(
            @RequestBody ConsultaReporteIaDTO solicitud,
            Authentication auth,
            HttpServletRequest request
    ) {
        contexto.resolver(auth, request);
        return ResponseEntity.ok(reporteService.interpretarConsultaIa(solicitud));
    }

    @PostMapping("/insights-ia")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request) or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_VER')")
    public ResponseEntity<Map<String, Object>> generarInsightsIa(
            @RequestParam(required = false, defaultValue = "Análisis general") String consulta,
            @RequestBody ReporteMetricasRespuesta metricas,
            Authentication auth,
            HttpServletRequest request
    ) {
        contexto.resolver(auth, request);
        return ResponseEntity.ok(reporteService.generarInsightsIa(consulta, metricas));
    }
}
