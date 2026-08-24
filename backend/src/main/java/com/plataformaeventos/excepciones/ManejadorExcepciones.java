package com.plataformaeventos.excepciones;

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
import jakarta.validation.ConstraintViolationException;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class ManejadorExcepciones {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManejadorExcepciones.class);

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
        ErrorRespuesta respuesta = new ErrorRespuesta(
                OffsetDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                solicitud.getRequestURI()
        );
        return ResponseEntity.status(estado).body(respuesta);
    }
}
