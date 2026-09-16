package com.plataformaeventos.organizaciones;

import com.plataformaeventos.excepciones.ExcepcionNoAutorizado;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ContextoOrganizacionTest {
    private final OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
    private final MembresiaOrganizacionRepository membresias = mock(MembresiaOrganizacionRepository.class);
    private final ContextoOrganizacion contexto = new ContextoOrganizacion(organizaciones, membresias);
    private final Authentication auth = mock(Authentication.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void rechazaOrganizacionInactiva() {
        Organizacion organizacion = organizacion(1L, "INACTIVA");
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("persona@correo.test");
        when(request.getHeader(ContextoOrganizacion.CABECERA)).thenReturn("1");
        when(organizaciones.findById(1L)).thenReturn(Optional.of(organizacion));

        assertThatThrownBy(() -> contexto.resolver(auth, request))
                .isInstanceOf(ExcepcionNoAutorizado.class)
                .hasMessageContaining("inactiva");
    }

    @Test
    void rechazaMembresiaInactivaYNoHaceFallbackSilencioso() {
        Organizacion principal = organizacion(1L, "ACTIVA");
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("persona@correo.test");
        when(request.getHeader(ContextoOrganizacion.CABECERA)).thenReturn(null);
        when(organizaciones.findBySlug("principal")).thenReturn(Optional.of(principal));
        when(membresias.existsByUsuarioCorreoAndOrganizacionIdAndActivoTrue("persona@correo.test", 1L)).thenReturn(false);

        assertThatThrownBy(() -> contexto.resolver(auth, request))
                .isInstanceOf(ExcepcionNoAutorizado.class)
                .hasMessageContaining("no pertenece");
    }

    private Organizacion organizacion(Long id, String estado) {
        Organizacion organizacion = new Organizacion("Org", "org");
        ReflectionTestUtils.setField(organizacion, "id", id);
        ReflectionTestUtils.setField(organizacion, "estado", estado);
        return organizacion;
    }
}
