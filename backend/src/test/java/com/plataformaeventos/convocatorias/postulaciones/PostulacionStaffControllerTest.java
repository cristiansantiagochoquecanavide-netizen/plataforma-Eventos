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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostulacionStaffController.class)
@Import({SeguridadConfiguracion.class, JwtFiltro.class,
        ManejadorAccesoNoAutorizado.class, ManejadorAccesoDenegado.class})
class PostulacionStaffControllerTest {

    @Autowired
    private MockMvc clienteHttp;
    @MockitoBean
    private PostulacionStaffService servicio;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private DetallesUsuarioService detallesUsuarioService;

    @Test
    void tokenDeUsuarioInexistenteDevuelve401() throws Exception {
        when(jwtService.extraerCorreo("prueba")).thenReturn("eliminado@example.com");
        when(detallesUsuarioService.loadUserByUsername("eliminado@example.com"))
                .thenThrow(new UsernameNotFoundException("Usuario no encontrado"));
        clienteHttp.perform(post("/api/convocatorias/2/postulaciones")
                .header("Authorization", "Bearer prueba")).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void tokenDeUsuarioInactivoDevuelve401() throws Exception {
        autenticar("CONVOCATORIA_POSTULARSE");
        when(detallesUsuarioService.loadUserByUsername("ana@example.com"))
                .thenReturn(User.withUsername("ana@example.com").password("hash")
                        .authorities("CONVOCATORIA_POSTULARSE").disabled(true).build());
        clienteHttp.perform(post("/api/convocatorias/2/postulaciones")
                .header("Authorization", "Bearer prueba")).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void todasLasRutasExigenAutenticacion() throws Exception {
        clienteHttp.perform(post("/api/convocatorias/2/postulaciones")).andExpect(status().isUnauthorized());
        clienteHttp.perform(get("/api/convocatorias/2/postulacion")).andExpect(status().isUnauthorized());
        clienteHttp.perform(get("/api/postulaciones/mias")).andExpect(status().isUnauthorized());
        clienteHttp.perform(patch("/api/postulaciones/3/retirar")).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void permisoDeConsultaNoPermitePostularse() throws Exception {
        autenticar("CONVOCATORIA_VER");
        clienteHttp.perform(post("/api/convocatorias/2/postulaciones")
                .header("Authorization", "Bearer prueba")).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    void postularseUsaCorreoDelTokenAunqueEnvienOtroUsuario() throws Exception {
        autenticar("CONVOCATORIA_POSTULARSE");
        clienteHttp.perform(post("/api/convocatorias/2/postulaciones")
                .header("Authorization", "Bearer prueba")
                .contentType(MediaType.APPLICATION_JSON).content("{\"idUsuario\":999}"))
                .andExpect(status().isCreated());
        verify(servicio).postularse(2L, "ana@example.com");
    }

    @Test
    void postularseSinCuerpoEsValido() throws Exception {
        autenticar("CONVOCATORIA_POSTULARSE");
        clienteHttp.perform(post("/api/convocatorias/2/postulaciones")
                .header("Authorization", "Bearer prueba")).andExpect(status().isCreated());
        verify(servicio).postularse(2L, "ana@example.com");
    }

    @Test
    void consultaSinPostulacionDevuelve204() throws Exception {
        autenticar("CONVOCATORIA_POSTULARSE");
        when(servicio.consultar(2L, "ana@example.com")).thenReturn(Optional.empty());
        clienteHttp.perform(get("/api/convocatorias/2/postulacion")
                .header("Authorization", "Bearer prueba")).andExpect(status().isNoContent());
    }

    @Test
    void listarYRetirarUsanCorreoAutenticado() throws Exception {
        autenticar("CONVOCATORIA_POSTULARSE");
        clienteHttp.perform(get("/api/postulaciones/mias").header("Authorization", "Bearer prueba"))
                .andExpect(status().isOk());
        clienteHttp.perform(patch("/api/postulaciones/3/retirar").header("Authorization", "Bearer prueba"))
                .andExpect(status().isOk());
        verify(servicio).listarMias("ana@example.com");
        verify(servicio).retirar(3L, "ana@example.com");
    }

    private void autenticar(String permiso) {
        when(jwtService.extraerCorreo("prueba")).thenReturn("ana@example.com");
        when(detallesUsuarioService.loadUserByUsername("ana@example.com"))
                .thenReturn(User.withUsername("ana@example.com").password("hash").authorities(permiso).build());
    }
}
