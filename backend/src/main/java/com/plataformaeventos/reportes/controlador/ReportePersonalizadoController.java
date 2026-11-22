package com.plataformaeventos.reportes.controlador;

import com.plataformaeventos.audit.servicio.OperacionAuditable;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.reportes.dto.ConsultaReporteIaDTO;
import com.plataformaeventos.reportes.dto.ExportarReporteDinamicoSolicitud;
import com.plataformaeventos.reportes.dto.ReporteMetricasRespuesta;
import com.plataformaeventos.reportes.dto.ResumenFinancieroDTO;
import com.plataformaeventos.reportes.servicio.BalanceFinancieroService;
import com.plataformaeventos.reportes.servicio.ExportadorReporteService;
import com.plataformaeventos.reportes.servicio.ReportePersonalizadoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes/personalizados")
public class ReportePersonalizadoController {

    private final ReportePersonalizadoService reporteService;
    private final ExportadorReporteService exportadorService;
    private final BalanceFinancieroService balanceFinancieroService;
    private final ContextoOrganizacion contexto;

    public ReportePersonalizadoController(
            ReportePersonalizadoService reporteService,
            ExportadorReporteService exportadorService,
            BalanceFinancieroService balanceFinancieroService,
            ContextoOrganizacion contexto
    ) {
        this.reporteService = reporteService;
        this.exportadorService = exportadorService;
        this.balanceFinancieroService = balanceFinancieroService;
        this.contexto = contexto;
    }

    @GetMapping("/balance-financiero")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request) or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_VER')")
    public ResponseEntity<ResumenFinancieroDTO> obtenerBalanceFinanciero(
            Authentication auth,
            HttpServletRequest request
    ) {
        Organizacion org = contexto.resolver(auth, request);
        return ResponseEntity.ok(balanceFinancieroService.calcularResumenFinanciero(org));
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

    @PostMapping("/generar-ia")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request) or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_VER')")
    public ResponseEntity<Map<String, Object>> generarReporteEjecutivoIa(
            @RequestBody ConsultaReporteIaDTO solicitud,
            Authentication auth,
            HttpServletRequest request
    ) {
        Organizacion org = contexto.resolver(auth, request);
        return ResponseEntity.ok(reporteService.generarReporteEjecutivoIa(org, solicitud.consulta()));
    }

    @PostMapping("/exportar-dinamico")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request) or @contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_VER')")
    public ResponseEntity<byte[]> exportarReporteDinamico(
            @RequestParam(defaultValue = "PDF") String formato,
            @RequestBody ExportarReporteDinamicoSolicitud solicitud,
            Authentication auth,
            HttpServletRequest request
    ) {
        Organizacion org = contexto.resolver(auth, request);
        String fmt = formato == null ? "PDF" : formato.trim().toUpperCase();
        String slug = slugArchivo(solicitud.titulo());
        String fecha = LocalDate.now().toString();

        if ("XLSX".equals(fmt) || "EXCEL".equals(fmt)) {
            byte[] bytes = exportadorService.exportarXlsx(solicitud, org.getNombre());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + slug + "-" + fecha + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        }

        byte[] bytes = exportadorService.exportarPdf(solicitud, org.getNombre());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + slug + "-" + fecha + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
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
            @RequestParam(required = false, defaultValue = "Analisis general") String consulta,
            @RequestBody ReporteMetricasRespuesta metricas,
            Authentication auth,
            HttpServletRequest request
    ) {
        contexto.resolver(auth, request);
        return ResponseEntity.ok(reporteService.generarInsightsIa(consulta, metricas));
    }

    private String slugArchivo(String titulo) {
        if (titulo == null || titulo.isBlank()) return "reporte-ejecutivo";
        String slug = titulo.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return slug.isBlank() ? "reporte-ejecutivo" : slug.substring(0, Math.min(slug.length(), 48));
    }
}
