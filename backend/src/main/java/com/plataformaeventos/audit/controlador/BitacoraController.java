package com.plataformaeventos.audit.controlador;

import com.plataformaeventos.audit.dto.BitacoraRespuesta;
import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin/bitacora")
public class BitacoraController {
    private final BitacoraService servicio; private final ContextoOrganizacion contexto;
    public BitacoraController(BitacoraService servicio, ContextoOrganizacion contexto) { this.servicio = servicio; this.contexto = contexto; }

    @GetMapping
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication,#request,'BITACORA_VER') or @contextoOrganizacion.esAdministrador(authentication,#request)")
    public ResponseEntity<Page<BitacoraRespuesta>> listar(
            @RequestParam(required = false) String modulo, @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String accion, @RequestParam(required = false) Boolean exito,
            @RequestParam(required = false) Long actor, @RequestParam(required = false) OffsetDateTime desde,
            @RequestParam(required = false) OffsetDateTime hasta,
            @PageableDefault(size = 25, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth, HttpServletRequest request) {
        Long organizacion = contexto.resolver(auth, request).getId();
        return ResponseEntity.ok(servicio.listar(organizacion, modulo, entidad, accion, exito, actor, desde, hasta, pageable));
    }
}
