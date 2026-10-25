package com.plataformaeventos.excepciones;

import com.plataformaeventos.audit.servicio.AuditoriaContexto;
import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.ObjectProvider;
import jakarta.validation.ConstraintViolationException;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class ManejadorExcepciones {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManejadorExcepciones.class);
    private final BitacoraService bitacora;
    private final ContextoOrganizacion contextoOrganizacion;
    private final UsuarioRepository usuarios;

    public ManejadorExcepciones(ObjectProvider<BitacoraService> bitacora,
                                ObjectProvider<ContextoOrganizacion> contextoOrganizacion,
                                ObjectProvider<UsuarioRepository> usuarios) {
        this.bitacora = bitacora.getIfAvailable();
        this.contextoOrganizacion = contextoOrganizacion.getIfAvailable();
        this.usuarios = usuarios.getIfAvailable();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(
            MethodArgumentNotValidException excepcion,
            HttpServletRequest solicitud
    ) {
        String mensaje = excepcion.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("La solicitud contiene datos invalidos");

        return crearRespuesta(HttpStatus.BAD_REQUEST, mensaje, solicitud);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> manejarSolicitudInvalida(HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es invalido", solicitud);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorRespuesta> manejarParametrosInvalidos(
            ConstraintViolationException excepcion,
            HttpServletRequest solicitud
    ) {
        String mensaje = excepcion.getConstraintViolations().stream()
                .findFirst()
                .map(violacion -> violacion.getMessage())
                .orElse("La solicitud contiene datos invalidos");
        return crearRespuesta(HttpStatus.BAD_REQUEST, mensaje, solicitud);
    }

    @ExceptionHandler(ExcepcionNoAutorizado.class)
    public ResponseEntity<ErrorRespuesta> manejarNoAutorizado(
            ExcepcionNoAutorizado excepcion,
            HttpServletRequest solicitud
    ) {
        return crearRespuesta(HttpStatus.UNAUTHORIZED, excepcion.getMessage(), solicitud);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorRespuesta> manejarAccesoDenegado(HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.FORBIDDEN, "No tiene permiso para esta operacion", solicitud);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRespuesta> manejarSolicitudIncorrecta(
            IllegalArgumentException excepcion,
            HttpServletRequest solicitud
    ) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, excepcion.getMessage(), solicitud);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorRespuesta> manejarEstadoIlegal(
            IllegalStateException excepcion,
            HttpServletRequest solicitud
    ) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, excepcion.getMessage(), solicitud);
    }

    @ExceptionHandler(ExcepcionNoEncontrado.class)
    public ResponseEntity<ErrorRespuesta> manejarNoEncontrado(
            ExcepcionNoEncontrado excepcion,
            HttpServletRequest solicitud
    ) {
        return crearRespuesta(HttpStatus.NOT_FOUND, excepcion.getMessage(), solicitud);
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorRespuesta> manejarErrorCorreo(
            MailException excepcion,
            HttpServletRequest solicitud
    ) {
        LOGGER.warn("No fue posible enviar correo en {}", solicitud.getRequestURI(), excepcion);
        return crearRespuesta(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No fue posible enviar las instrucciones de recuperaci\u00f3n.",
                solicitud
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorRespuesta> manejarEstadoHttp(
            ResponseStatusException excepcion,
            HttpServletRequest solicitud
    ) {
        HttpStatus estado = HttpStatus.resolve(excepcion.getStatusCode().value());
        if (estado == null) estado = HttpStatus.INTERNAL_SERVER_ERROR;
        String mensaje = excepcion.getReason() == null ? estado.getReasonPhrase() : excepcion.getReason();
        return crearRespuesta(estado, mensaje, solicitud);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> manejarErrorNoControlado(
            Exception excepcion,
            HttpServletRequest solicitud
    ) {
        LOGGER.error("Error no controlado en {}", solicitud.getRequestURI(), excepcion);
        return crearRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error interno", solicitud);
    }

    private ResponseEntity<ErrorRespuesta> crearRespuesta(
            HttpStatus estado,
            String mensaje,
            HttpServletRequest solicitud
    ) {
        registrarFallo(solicitud, mensaje);
        ErrorRespuesta respuesta = new ErrorRespuesta(
                OffsetDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                solicitud.getRequestURI()
        );
        return ResponseEntity.status(estado).body(respuesta);
    }

    private void registrarFallo(HttpServletRequest solicitud, String mensaje) {
        try {
            var autenticacion = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (bitacora == null || contextoOrganizacion == null || usuarios == null
                    || autenticacion == null || !autenticacion.isAuthenticated()
                    || solicitud.getRequestURI().startsWith("/api/admin/bitacora")) return;
            Organizacion organizacion = contextoOrganizacion.resolver(autenticacion, solicitud);
            var actor = usuarios.findByCorreo(autenticacion.getName()).orElse(null);
            if (actor == null) return;
            String[] partes = solicitud.getRequestURI().split("/");
            int indiceModulo = partes.length > 3 && "admin".equalsIgnoreCase(partes[2]) ? 3 : 2;
            String modulo = partes.length > indiceModulo ? partes[indiceModulo].toUpperCase() : "HTTP";
            String entidad = partes.length == 0 ? "HTTP" : partes[Math.max(0, partes.length - 1)].toUpperCase();
            bitacora.registrar(actor, organizacion, solicitud.getMethod(), modulo, entidad, null,
                    AuditoriaContexto.ip(solicitud), false, limitar(mensaje));
        } catch (RuntimeException ignorado) {
            LOGGER.debug("No fue posible registrar la operacion fallida", ignorado);
        }
    }

    private String limitar(String mensaje) {
        if (mensaje == null) return null;
        return mensaje.length() <= 1000 ? mensaje : mensaje.substring(0, 1000);
    }
}
