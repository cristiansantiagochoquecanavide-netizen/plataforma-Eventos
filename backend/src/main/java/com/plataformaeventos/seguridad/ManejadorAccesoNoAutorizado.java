package com.plataformaeventos.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plataformaeventos.excepciones.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
public class ManejadorAccesoNoAutorizado implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ManejadorAccesoNoAutorizado(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest solicitud,
            HttpServletResponse respuesta,
            AuthenticationException excepcion
    ) throws IOException {
        escribirRespuesta(solicitud, respuesta, HttpStatus.UNAUTHORIZED, "Se requiere autenticacion valida");
    }

    void escribirRespuesta(
            HttpServletRequest solicitud,
            HttpServletResponse respuesta,
            HttpStatus estado,
            String mensaje
    ) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(respuesta.getOutputStream(), new ErrorRespuesta(
                OffsetDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                solicitud.getRequestURI()
        ));
    }
}
