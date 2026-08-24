package com.plataformaeventos.autenticacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/autenticacion")
@Tag(name = "Autenticacion", description = "Registro y gestion de sesion")
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    public AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrar participante", description = "Crea un usuario activo con el rol PARTICIPANTE.")
    @ApiResponse(responseCode = "201", description = "Usuario registrado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos", content = @Content)
    public ResponseEntity<UsuarioActualRespuesta> registrar(@Valid @RequestBody RegistroSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(autenticacionService.registrarUsuario(solicitud));
    }

    @PostMapping("/iniciar-sesion")
    @Operation(summary = "Iniciar sesion")
    @ApiResponse(responseCode = "200", description = "Sesion creada")
    @ApiResponse(responseCode = "401", description = "Credenciales invalidas", content = @Content)
    public ResponseEntity<SesionRespuesta> iniciarSesion(@Valid @RequestBody InicioSesionSolicitud solicitud) {
        return ResponseEntity.ok(autenticacionService.iniciarSesion(solicitud));
    }

    @PostMapping("/refrescar")
    @Operation(summary = "Renovar sesion", description = "Rota el refresh token y emite un nuevo access token.")
    @ApiResponse(responseCode = "200", description = "Sesion renovada")
    @ApiResponse(responseCode = "401", description = "Token de refresco invalido", content = @Content)
    public ResponseEntity<SesionRespuesta> refrescar(@Valid @RequestBody TokenRefrescoSolicitud solicitud) {
        return ResponseEntity.ok(autenticacionService.refrescarSesion(solicitud));
    }

    @PostMapping("/cerrar-sesion")
    @Operation(summary = "Cerrar sesion", description = "Revoca el refresh token recibido.")
    @ApiResponse(responseCode = "204", description = "Sesion cerrada")
    public ResponseEntity<Void> cerrarSesion(@Valid @RequestBody TokenRefrescoSolicitud solicitud) {
        autenticacionService.cerrarSesion(solicitud);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/recuperar-clave", "/solicitar-recuperacion-clave"})
    @Operation(summary = "Solicitar recuperacion de clave", description = "Valida el correo y envia instrucciones de recuperacion.")
    @ApiResponse(responseCode = "204", description = "Solicitud procesada")
    @ApiResponse(responseCode = "400", description = "Correo no registrado o datos invalidos", content = @Content)
    public ResponseEntity<Void> solicitarRecuperacionClave(@Valid @RequestBody SolicitudRecuperacionClave solicitud) {
        autenticacionService.solicitarRecuperacionClave(solicitud);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restablecer-clave")
    @Operation(summary = "Restablecer clave", description = "Consume un token de recuperacion de un solo uso y revoca las sesiones existentes.")
    @ApiResponse(responseCode = "204", description = "Clave restablecida")
    @ApiResponse(responseCode = "400", description = "Token o nueva clave invalidos", content = @Content)
    public ResponseEntity<Void> restablecerClave(@Valid @RequestBody RestablecerClaveSolicitud solicitud) {
        autenticacionService.restablecerClave(solicitud);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuario-actual")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consultar usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Usuario autenticado")
    @ApiResponse(responseCode = "401", description = "JWT invalido o ausente", content = @Content)
    public ResponseEntity<UsuarioActualRespuesta> obtenerUsuarioActual(Authentication autenticacion) {
        return ResponseEntity.ok(autenticacionService.obtenerUsuarioActual(autenticacion.getName()));
    }
}
