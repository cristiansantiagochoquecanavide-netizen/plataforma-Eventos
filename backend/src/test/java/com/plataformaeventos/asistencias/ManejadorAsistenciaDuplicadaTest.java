package com.plataformaeventos.asistencias;

import com.plataformaeventos.excepciones.ManejadorExcepciones;
import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManejadorAsistenciaDuplicadaTest {
    @Test
    void responde409CuandoYaExisteAsistencia() {
        ObjectProvider<BitacoraService> bitacora = mock(ObjectProvider.class);
        ObjectProvider<ContextoOrganizacion> contexto = mock(ObjectProvider.class);
        ObjectProvider<UsuarioRepository> usuarios = mock(ObjectProvider.class);
        when(bitacora.getIfAvailable()).thenReturn(null);
        when(contexto.getIfAvailable()).thenReturn(null);
        when(usuarios.getIfAvailable()).thenReturn(null);
        var manejador = new ManejadorExcepciones(bitacora, contexto, usuarios);
        var request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/asistencias/check-in");

        var respuesta = manejador.manejarAsistenciaDuplicada(request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
