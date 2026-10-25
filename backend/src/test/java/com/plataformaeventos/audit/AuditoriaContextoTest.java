package com.plataformaeventos.audit;

import com.plataformaeventos.audit.servicio.AuditoriaContexto;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuditoriaContextoTest {
    @Test
    void usaPrimerIpValidoDeXForwardedFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("no-es-ip, 2001:db8::1, 192.0.2.4");
        when(request.getRemoteAddr()).thenReturn("198.51.100.7");
        assertThat(AuditoriaContexto.ip(request)).isEqualTo("2001:db8::1");
    }

    @Test
    void descartaCabeceraInvalidaYUsaRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("hostil.example");
        when(request.getRemoteAddr()).thenReturn("198.51.100.7");
        assertThat(AuditoriaContexto.ip(request)).isEqualTo("198.51.100.7");
    }
}
