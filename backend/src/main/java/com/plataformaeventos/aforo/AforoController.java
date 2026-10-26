package com.plataformaeventos.aforo;

import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/aforo")
@PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'ASISTENCIA_CONSULTAR')")
public class AforoController {
    private final AforoService servicio; private final ContextoOrganizacion contexto; private final AforoExportador exportador;
    public AforoController(AforoService servicio, ContextoOrganizacion contexto, AforoExportador exportador) {
        this.servicio=servicio; this.contexto=contexto; this.exportador=exportador;
    }
    @GetMapping("/opciones")
    public Map<String,Object> opciones(Authentication authentication, HttpServletRequest request) {
        long org=contexto.resolver(authentication,request).getId();
        return Map.of("eventos",servicio.opciones(org),"categorias",servicio.categorias(org));
    }
    @GetMapping
    public AforoService.Panel consultar(@RequestParam(required=false) Long eventoId, @RequestParam(required=false) Long salaId,
            @RequestParam(required=false) String categoria,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(defaultValue="0") int pagina, Authentication authentication, HttpServletRequest request) {
        long org=contexto.resolver(authentication,request).getId(); var f=new AforoFiltro(eventoId,salaId,categoria,desde,hasta);
        return servicio.panel(org,f,pagina);
    }
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam String formato,
            @RequestParam(required=false) Long eventoId, @RequestParam(required=false) Long salaId, @RequestParam(required=false) String categoria,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            Authentication authentication, HttpServletRequest request) {
        if (!List.of("csv","pdf").contains(formato)) throw new IllegalArgumentException("Formato permitido: csv o pdf");
        var f=new AforoFiltro(eventoId,salaId,categoria,desde,hasta);
        var n=servicio.nomina(contexto.resolver(authentication,request).getId(),f,0,5000);
        if(n.total()>5000) throw new IllegalArgumentException("La exportación supera 5000 registros; aplica más filtros");
        byte[] bytes=formato.equals("csv") ? exportador.csv(n.filas(),f) : exportador.pdf(n.filas(),f);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(formato.equals("csv") ? "text/csv;charset=UTF-8" : "application/pdf"))
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=asistencia."+formato)
            .cacheControl(CacheControl.noStore()).body(bytes);
    }
    public record CategoriaEntrada(String categoria) {}
    @PatchMapping("/inscripciones/{id}/categoria")
    public ResponseEntity<Void> clasificar(@PathVariable long id, @RequestBody CategoriaEntrada body,
                                           Authentication authentication, HttpServletRequest request) {
        servicio.clasificar(contexto.resolver(authentication,request).getId(),id,body.categoria());
        return ResponseEntity.noContent().build();
    }
}
