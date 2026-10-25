package com.plataformaeventos.usuarios.controlador;

import com.plataformaeventos.usuarios.dto.ActualizarUsuarioSolicitud;
import com.plataformaeventos.usuarios.dto.CambiarEstadoUsuarioSolicitud;
import com.plataformaeventos.usuarios.dto.CrearUsuarioSolicitud;
import com.plataformaeventos.usuarios.dto.PaginaUsuariosRespuesta;
import com.plataformaeventos.usuarios.dto.UsuarioRespuesta;
import com.plataformaeventos.usuarios.servicio.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;

@RestController
@Validated
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Administracion de usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ContextoOrganizacion contexto;

    public UsuarioController(UsuarioService usuarioService, ContextoOrganizacion contexto) {
        this.usuarioService = usuarioService;
        this.contexto = contexto;
    }

    @GetMapping
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'USUARIO_VER')")
    @Operation(summary = "Listar usuarios")
    @ApiResponse(responseCode = "200", description = "Usuarios consultados")
    public ResponseEntity<PaginaUsuariosRespuesta> listarUsuarios(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La pagina no puede ser negativa") int pagina,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamano debe ser al menos 1")
            @Max(value = 100, message = "El tamano no puede superar 100") int tamano, Authentication auth, HttpServletRequest request
    ) {
        return ResponseEntity.ok(usuarioService.listarUsuarios(pagina, tamano, contexto.resolver(auth, request)));
    }

    @GetMapping("/{idUsuario}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'USUARIO_VER')")
    @Operation(summary = "Consultar usuario por identificador")
    @ApiResponse(responseCode = "200", description = "Usuario consultado")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    public ResponseEntity<UsuarioRespuesta> obtenerUsuario(@PathVariable Long idUsuario, Authentication auth, HttpServletRequest request) {
        return ResponseEntity.ok(usuarioService.obtenerUsuario(idUsuario, contexto.resolver(auth, request)));
    }

    @PostMapping
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'USUARIO_CREAR')")
    @Operation(summary = "Crear usuario")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos o roles invalidos", content = @Content)
    public ResponseEntity<UsuarioRespuesta> crearUsuario(@Valid @RequestBody CrearUsuarioSolicitud solicitud, Authentication auth, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearUsuario(solicitud, contexto.resolver(auth, request)));
    }

    @PutMapping("/{idUsuario}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'USUARIO_EDITAR')")
    @Operation(summary = "Actualizar usuario")
    @ApiResponse(responseCode = "200", description = "Usuario actualizado")
    @ApiResponse(responseCode = "400", description = "Datos o roles invalidos", content = @Content)
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    public ResponseEntity<UsuarioRespuesta> actualizarUsuario(
            @PathVariable Long idUsuario,
            @Valid @RequestBody ActualizarUsuarioSolicitud solicitud, Authentication auth, HttpServletRequest request
    ) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(idUsuario, solicitud, contexto.resolver(auth, request)));
    }

    @PatchMapping("/{idUsuario}/estado")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'USUARIO_CAMBIAR_ESTADO')")
    @Operation(summary = "Activar o inactivar usuario")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Cambio de estado invalido", content = @Content)
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    public ResponseEntity<UsuarioRespuesta> cambiarEstado(
            @PathVariable Long idUsuario,
            @Valid @RequestBody CambiarEstadoUsuarioSolicitud solicitud,
            Authentication autenticacion, HttpServletRequest request
    ) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(idUsuario, solicitud.estado(), autenticacion.getName(), contexto.resolver(autenticacion, request)));
    }
}
