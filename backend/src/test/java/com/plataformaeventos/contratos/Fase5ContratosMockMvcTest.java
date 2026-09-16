package com.plataformaeventos.contratos;

import com.plataformaeventos.audit.controlador.BitacoraController;
import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.asignacionesstaff.controlador.AsignacionStaffController;
import com.plataformaeventos.asignacionesstaff.servicio.AsignacionStaffService;
import com.plataformaeventos.categorias.controlador.CategoriaController;
import com.plataformaeventos.categorias.servicio.CategoriaService;
import com.plataformaeventos.convocatorias.controlador.ConvocatoriaController;
import com.plataformaeventos.convocatorias.servicio.ConvocatoriaService;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.postulacionesstaff.controlador.PostulacionStaffController;
import com.plataformaeventos.postulacionesstaff.servicio.PostulacionStaffService;
import com.plataformaeventos.usuarios.controlador.UsuarioController;
import com.plataformaeventos.usuarios.dto.PaginaUsuariosRespuesta;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.usuarios.servicio.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Fase5ContratosMockMvcTest {
    private MockMvc mvc;
    private final ContextoOrganizacion contexto = mock(ContextoOrganizacion.class);
    private final Organizacion orgA = organizacion(10L);
    private final CategoriaService categorias = mock(CategoriaService.class);
    private final UsuarioService usuarios = mock(UsuarioService.class);
    private final ConvocatoriaService convocatorias = mock(ConvocatoriaService.class);
    private final BitacoraService bitacora = mock(BitacoraService.class);
    private final AsignacionStaffService asignaciones = mock(AsignacionStaffService.class);
    private final PostulacionStaffService postulaciones = mock(PostulacionStaffService.class);
    private final UsuarioRepository usuariosRepository = mock(UsuarioRepository.class);

    @BeforeEach
    void configurar() {
        when(contexto.resolver(any(), any())).thenReturn(orgA);
        when(categorias.listarCategorias(orgA)).thenReturn(List.of());
        when(convocatorias.listar(any(), any(), any(), any(), eq(orgA))).thenReturn(List.of());
        when(bitacora.listar(anyLong(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(Page.empty(PageRequest.of(0, 25)));
        when(asignaciones.listar(10L)).thenReturn(List.of());
        when(postulaciones.propias(anyString(), eq(10L))).thenReturn(List.of());
        when(usuarios.listarUsuarios(anyInt(), anyInt(), eq(orgA))).thenReturn(new PaginaUsuariosRespuesta(List.of(), 0, 20, 0, 0));
        mvc = MockMvcBuilders.standaloneSetup(
                new CategoriaController(categorias, contexto),
                new UsuarioController(usuarios, contexto),
                new ConvocatoriaController(convocatorias, contexto),
                new BitacoraController(bitacora, contexto),
                new AsignacionStaffController(asignaciones, contexto, usuariosRepository),
                new PostulacionStaffController(postulaciones, contexto, usuariosRepository)
        ).setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }

    @Test
    void endpointsDeConsultaNuevosRespetanLaCabeceraYContrato() throws Exception {
        mvc.perform(get("/api/categorias").header(ContextoOrganizacion.CABECERA, "10").principal(auth()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/usuarios").header(ContextoOrganizacion.CABECERA, "10").principal(auth()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/convocatorias").header(ContextoOrganizacion.CABECERA, "10").principal(auth()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/bitacora").header(ContextoOrganizacion.CABECERA, "10").principal(auth()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/asignaciones-staff").header(ContextoOrganizacion.CABECERA, "10").principal(auth()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/postulaciones-staff/mias").header(ContextoOrganizacion.CABECERA, "10").principal(auth()))
                .andExpect(status().isOk());

        verify(categorias).listarCategorias(orgA);
        verify(usuarios).listarUsuarios(0, 20, orgA);
        verify(convocatorias).listar(null, null, null, null, orgA);
        verify(bitacora).listar(eq(10L), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any());
        verify(asignaciones).listar(10L);
        verify(postulaciones).propias("ana@test", 10L);
    }

    @Test
    void endpointDeAsignacionValidaElContratoDeCreacion() throws Exception {
        when(usuariosRepository.findByCorreo("ana@test")).thenReturn(java.util.Optional.of(mock(com.plataformaeventos.usuarios.modelo.Usuario.class)));
        mvc.perform(post("/api/asignaciones-staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPostulacion\":7,\"tarea\":\"Apoyo\"}")
                        .header(ContextoOrganizacion.CABECERA, "10")
                        .principal(auth()))
                .andExpect(status().isCreated());
        verify(asignaciones).crear(any(), eq(orgA), any());
    }

    private static Organizacion organizacion(Long id) {
        Organizacion org = new Organizacion("A", "a");
        ReflectionTestUtils.setField(org, "id", id);
        return org;
    }

    private static UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken("ana@test", "N/A");
    }
}
