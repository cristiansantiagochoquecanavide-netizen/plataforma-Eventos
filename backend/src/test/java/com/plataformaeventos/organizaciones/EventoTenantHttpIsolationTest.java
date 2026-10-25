package com.plataformaeventos.organizaciones;

import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.espacios.repositorio.EspacioRepository;
import com.plataformaeventos.eventos.controlador.EventoController;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.eventos.servicio.EventoService;
import com.plataformaeventos.excepciones.ManejadorExcepciones;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.saas.servicio.SuscripcionOrganizacionService;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventoTenantHttpIsolationTest {

    private final EventoRepository eventos = mock(EventoRepository.class);
    private final CategoriaRepository categorias = mock(CategoriaRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final EspacioRepository espacios = mock(EspacioRepository.class);
    private final OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
    private final MembresiaOrganizacionRepository membresias = mock(MembresiaOrganizacionRepository.class);
    private final SuscripcionOrganizacionService suscripciones = mock(SuscripcionOrganizacionService.class);
    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        when(suscripciones.tieneAccesoActivo(anyLong(), any())).thenReturn(true);
        ContextoOrganizacion contexto = new ContextoOrganizacion(organizaciones, membresias, suscripciones);
        EventoService eventoService = new EventoService(eventos, categorias, usuarios, espacios);
        mvc = MockMvcBuilders.standaloneSetup(new EventoController(eventoService, contexto))
                .setControllerAdvice(manejadorExcepcionesSinAuditoria())
                .build();
    }

    @Test
    void obtenerEventoDevuelve404CuandoElEventoPerteneceAOtroTenant() throws Exception {
        Organizacion organizacion = organizacionActiva(20L);
        when(organizaciones.findById(20L)).thenReturn(Optional.of(organizacion));
        when(membresias.existsByUsuarioCorreoAndOrganizacionIdAndActivoTrue("ana@test", 20L)).thenReturn(true);
        when(eventos.findByIdAndOrganizacionId(7L, 20L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/eventos/{idEvento}", 7L)
                        .header(ContextoOrganizacion.CABECERA, "20")
                        .principal(new UsernamePasswordAuthenticationToken("ana@test", "N/A", List.of())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Evento no encontrado"));

        verify(eventos).findByIdAndOrganizacionId(7L, 20L);
        verify(eventos, never()).findById(7L);
    }

    @Test
    void elMismoIdSoloDevuelveElEventoDentroDelTenantPropietario() throws Exception {
        Organizacion organizacionA = organizacionActiva(10L);
        Organizacion organizacionB = organizacionActiva(20L);
        Categoria categoria = new Categoria("Conferencia", null, organizacionA);
        ReflectionTestUtils.setField(categoria, "id", 3L);
        Evento evento = new Evento("Evento privado A", null, OffsetDateTime.now().plusDays(1), null,
                "Auditorio", EstadoEvento.PLANIFICADO, categoria);
        ReflectionTestUtils.setField(evento, "id", 7L);
        evento.asignarOrganizacion(organizacionA);

        when(organizaciones.findById(10L)).thenReturn(Optional.of(organizacionA));
        when(organizaciones.findById(20L)).thenReturn(Optional.of(organizacionB));
        when(membresias.existsByUsuarioCorreoAndOrganizacionIdAndActivoTrue("ana@test", 10L)).thenReturn(true);
        when(membresias.existsByUsuarioCorreoAndOrganizacionIdAndActivoTrue("ana@test", 20L)).thenReturn(true);
        when(eventos.findByIdAndOrganizacionId(7L, 10L)).thenReturn(Optional.of(evento));
        when(eventos.findByIdAndOrganizacionId(7L, 20L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/eventos/{idEvento}", 7L)
                        .header(ContextoOrganizacion.CABECERA, "10")
                        .principal(new UsernamePasswordAuthenticationToken("ana@test", "N/A", List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Evento privado A"));

        mvc.perform(get("/api/eventos/{idEvento}", 7L)
                        .header(ContextoOrganizacion.CABECERA, "20")
                        .principal(new UsernamePasswordAuthenticationToken("ana@test", "N/A", List.of())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Evento no encontrado"));

        verify(eventos).findByIdAndOrganizacionId(7L, 10L);
        verify(eventos).findByIdAndOrganizacionId(7L, 20L);
        verify(eventos, never()).findById(7L);
    }

    private static Organizacion organizacionActiva(Long id) {
        Organizacion organizacion = new Organizacion("Organizacion B", "org-b");
        ReflectionTestUtils.setField(organizacion, "id", id);
        ReflectionTestUtils.setField(organizacion, "estado", "ACTIVA");
        return organizacion;
    }

    @SuppressWarnings("unchecked")
    private static ManejadorExcepciones manejadorExcepcionesSinAuditoria() {
        ObjectProvider<BitacoraService> bitacora = mock(ObjectProvider.class);
        ObjectProvider<ContextoOrganizacion> contexto = mock(ObjectProvider.class);
        ObjectProvider<UsuarioRepository> usuarios = mock(ObjectProvider.class);
        return new ManejadorExcepciones(bitacora, contexto, usuarios);
    }
}
