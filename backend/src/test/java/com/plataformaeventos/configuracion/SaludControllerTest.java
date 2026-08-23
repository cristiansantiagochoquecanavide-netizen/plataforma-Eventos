package com.plataformaeventos.configuracion;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SaludController.class)
@Import({
        SeguridadConfiguracion.class,
        JwtFiltro.class,
        ManejadorAccesoNoAutorizado.class,
        ManejadorAccesoDenegado.class
})
class SaludControllerTest {

    @Autowired
    private MockMvc clienteHttp;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private DetallesUsuarioService detallesUsuarioService;

    @Test
    void obtenerSaludDevuelveEstadoDisponible() throws Exception {
        clienteHttp.perform(get("/api/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ok"))
                .andExpect(jsonPath("$.servicio").value("backend"));
    }

    @Test
    void rutaProtegidaSinTokenDevuelveNoAutorizado() throws Exception {
        clienteHttp.perform(get("/api/ruta-protegida"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
