package com.plataformaeventos.audit;

import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.excepciones.ManejadorExcepciones;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import java.util.Optional;
import static org.mockito.Mockito.*;

class ManejadorExcepcionesAuditoriaTest {
    @Test
    void registraFalloSinCambiarElManejador() {
        BitacoraService bitacora = mock(BitacoraService.class);
        ContextoOrganizacion contexto = mock(ContextoOrganizacion.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/admin/eventos");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("192.0.2.4");
        Usuario actor = mock(Usuario.class);
        Organizacion organizacion = mock(Organizacion.class);
        when(usuarios.findByCorreo("actor@test")).thenReturn(Optional.of(actor));
        when(contexto.resolver(any(), eq(request))).thenReturn(organizacion);
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("actor@test");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            new ManejadorExcepciones(provider(bitacora), provider(contexto), provider(usuarios))
                    .manejarSolicitudIncorrecta(new IllegalArgumentException("fallo"), request);
            verify(bitacora).registrar(eq(actor), eq(organizacion), eq("POST"), eq("EVENTOS"),
                    eq("EVENTOS"), isNull(), eq("192.0.2.4"), eq(false), eq("fallo"));
        } finally { SecurityContextHolder.clearContext(); }
    }

    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
