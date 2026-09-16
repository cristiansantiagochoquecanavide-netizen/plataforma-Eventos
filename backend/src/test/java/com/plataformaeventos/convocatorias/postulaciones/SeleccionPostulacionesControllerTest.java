package com.plataformaeventos.convocatorias.postulaciones;

import com.plataformaeventos.seguridad.DetallesUsuarioService;
import com.plataformaeventos.seguridad.JwtFiltro;
import com.plataformaeventos.seguridad.JwtService;
import com.plataformaeventos.seguridad.ManejadorAccesoDenegado;
import com.plataformaeventos.seguridad.ManejadorAccesoNoAutorizado;
import com.plataformaeventos.seguridad.SeguridadConfiguracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeleccionPostulacionesController.class)
@Import({SeguridadConfiguracion.class, JwtFiltro.class,
        ManejadorAccesoNoAutorizado.class, ManejadorAccesoDenegado.class})
class SeleccionPostulacionesControllerTest {

    @Autowired
    private MockMvc clienteHttp;
    @MockitoBean
    private GestionPostulacionesService servicio;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private DetallesUsuarioService detallesUsuarioService;

    @Test
    void rutasRequierenPermisoDeGestionDePostulaciones() throws Exception {
        autenticar("CONVOCATORIA_VER");

        clienteHttp.perform(get("/api/convocatorias/8/postulaciones")
                .header("Authorization", "Bearer prueba")).andExpect(status().isForbidden());
        clienteHttp.perform(patch("/api/postulaciones/6/seleccionar")
                .header("Authorization", "Bearer prueba")).andExpect(status().isForbidden());
        clienteHttp.perform(patch("/api/postulaciones/6/rechazar")
                .header("Authorization", "Bearer prueba")).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    void permisoDeGestionAutorizaConsultaYUsaIdentidadDelToken() throws Exception {
        autenticar("CONVOCATORIA_GESTIONAR_POSTULACIONES");

        clienteHttp.perform(get("/api/convocatorias/8/postulaciones")
                .header("Authorization", "Bearer prueba")).andExpect(status().isOk());

        verify(servicio).listarPostulantes(8L, "gestor@example.com");
    }

    private void autenticar(String permiso) {
        when(jwtService.extraerCorreo("prueba")).thenReturn("gestor@example.com");
        when(detallesUsuarioService.loadUserByUsername("gestor@example.com"))
                .thenReturn(User.withUsername("gestor@example.com").password("hash")
                        .authorities(permiso).build());
    }
}
