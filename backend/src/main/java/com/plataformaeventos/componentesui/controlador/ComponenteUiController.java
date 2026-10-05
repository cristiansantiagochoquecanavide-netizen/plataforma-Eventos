package com.plataformaeventos.componentesui.controlador;

import com.plataformaeventos.componentesui.dto.DefinicionComponenteUiRespuesta;
import com.plataformaeventos.componentesui.dto.GuardarPrivilegiosGrupoSolicitud;
import com.plataformaeventos.componentesui.dto.PrivilegiosEfectivosRespuesta;
import com.plataformaeventos.componentesui.dto.PrivilegiosGrupoRespuesta;
import com.plataformaeventos.componentesui.servicio.ComponenteUiService;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/componentes-ui")
public class ComponenteUiController {
    private final ComponenteUiService servicio;
    private final ContextoOrganizacion contexto;

    public ComponenteUiController(ComponenteUiService servicio, ContextoOrganizacion contexto) {
        this.servicio = servicio;
        this.contexto = contexto;
    }

    @GetMapping("/catalogo")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<List<DefinicionComponenteUiRespuesta>> catalogo(HttpServletRequest request) {
        return ResponseEntity.ok(servicio.catalogo());
    }

    @GetMapping("/grupos/{idGrupo}/privilegios")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<PrivilegiosGrupoRespuesta> privilegiosGrupo(@PathVariable Long idGrupo,
                                                                      Authentication authentication,
                                                                      HttpServletRequest request) {
        return ResponseEntity.ok(servicio.privilegiosGrupo(organizacionActiva(authentication, request), idGrupo));
    }

    @PutMapping("/grupos/{idGrupo}/privilegios")
    @PreAuthorize("@contextoOrganizacion.esAdministrador(authentication, #request)")
    public ResponseEntity<PrivilegiosGrupoRespuesta> guardarPrivilegiosGrupo(@PathVariable Long idGrupo,
                                                                             @Valid @RequestBody GuardarPrivilegiosGrupoSolicitud solicitud,
                                                                             Authentication authentication,
                                                                             HttpServletRequest request) {
        return ResponseEntity.ok(servicio.guardarPrivilegiosGrupo(organizacionActiva(authentication, request), idGrupo, solicitud));
    }

    @GetMapping("/actual/privilegios")
    public ResponseEntity<PrivilegiosEfectivosRespuesta> privilegiosUsuarioActual(Authentication authentication,
                                                                                  HttpServletRequest request) {
        Organizacion organizacion = organizacionActiva(authentication, request);
        boolean administradorTenant = contexto.esAdministrador(authentication, request);
        return ResponseEntity.ok(servicio.privilegiosEfectivos(organizacion, authentication.getName(), administradorTenant));
    }

    private Organizacion organizacionActiva(Authentication authentication, HttpServletRequest request) {
        return contexto.resolver(authentication, request);
    }
}
