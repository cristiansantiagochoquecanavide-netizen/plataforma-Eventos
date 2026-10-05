package com.plataformaeventos.organizaciones;

import com.plataformaeventos.audit.modelo.Bitacora;
import com.plataformaeventos.asignacionesstaff.repositorio.AsignacionStaffRepository;
import com.plataformaeventos.asignacionesstaff.servicio.AsignacionStaffService;
import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.categorias.servicio.CategoriaService;
import com.plataformaeventos.convocatorias.repositorio.ConvocatoriaRepository;
import com.plataformaeventos.convocatorias.servicio.ConvocatoriaService;
import com.plataformaeventos.excepciones.ExcepcionNoAutorizado;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.saas.servicio.SuscripcionOrganizacionService;
import com.plataformaeventos.postulacionesstaff.repositorio.PostulacionStaffRepository;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.usuarios.servicio.UsuarioService;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.seguridad.TokenRefrescoService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Fase5TenantIsolationTest {
    private final OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
    private final MembresiaOrganizacionRepository membresias = mock(MembresiaOrganizacionRepository.class);
    private final Authentication auth = mock(Authentication.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final Organizacion orgA = org(10L, "A", "ACTIVA");
    private final Organizacion orgB = org(20L, "B", "ACTIVA");

    @Test
    void contextoPermiteAperoRechazaBParaElMismoUsuario() {
        ContextoOrganizacion contexto = contextoConSuscripcionesActivas();
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("ana@test");
        when(request.getHeader(ContextoOrganizacion.CABECERA)).thenReturn("10", "20");
        when(organizaciones.findById(10L)).thenReturn(Optional.of(orgA));
        when(organizaciones.findById(20L)).thenReturn(Optional.of(orgB));
        when(membresias.existsByUsuarioCorreoAndOrganizacionIdAndActivoTrue("ana@test", 10L)).thenReturn(true);
        when(membresias.existsByUsuarioCorreoAndOrganizacionIdAndActivoTrue("ana@test", 20L)).thenReturn(false);

        assertThat(contexto.resolver(auth, request)).isSameAs(orgA);
        assertThatThrownBy(() -> contexto.resolver(auth, request))
                .isInstanceOf(ExcepcionNoAutorizado.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void permisoContextualTambienDependeDelTenantYDeLaMembresiaActiva() {
        ContextoOrganizacion contexto = contextoConSuscripcionesActivas();
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("ana@test");
        when(request.getHeader(ContextoOrganizacion.CABECERA)).thenReturn("10");
        when(organizaciones.findById(10L)).thenReturn(Optional.of(orgA));
        when(organizaciones.findById(20L)).thenReturn(Optional.of(orgB));
        when(membresias.existsByUsuarioCorreoAndOrganizacionIdAndActivoTrue("ana@test", 10L)).thenReturn(true);
        when(membresias.existsByUsuarioCorreoAndOrganizacionIdAndActivoTrue("ana@test", 20L)).thenReturn(true);
        when(membresias.tienePermisoActivo("ana@test", 10L, "ASIGNACION_STAFF_GESTIONAR")).thenReturn(true);
        when(membresias.tienePermisoActivo("ana@test", 20L, "ASIGNACION_STAFF_GESTIONAR")).thenReturn(false);

        assertThat(contexto.tieneAutoridad(auth, request, "ASIGNACION_STAFF_GESTIONAR")).isTrue();
        when(request.getHeader(ContextoOrganizacion.CABECERA)).thenReturn("20");
        assertThat(contexto.tieneAutoridad(auth, request, "ASIGNACION_STAFF_GESTIONAR")).isFalse();
    }

    @Test
    void categoriasUsuariosYConvocatoriasNuncaBuscanFueraDelTenant() {
        CategoriaRepository categorias = mock(CategoriaRepository.class);
        CategoriaService categoriaService = new CategoriaService(categorias, mock(com.plataformaeventos.eventos.repositorio.EventoRepository.class));
        when(categorias.findByIdAndOrganizacionId(7L, 20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> categoriaService.obtenerCategoria(7L, orgB)).isInstanceOf(RuntimeException.class);
        verify(categorias).findByIdAndOrganizacionId(7L, 20L);

        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        UsuarioService usuarioService = new UsuarioService(usuarios, mock(RolRepository.class), mock(org.springframework.security.crypto.password.PasswordEncoder.class), mock(TokenRefrescoService.class), membresias);
        when(usuarios.findByIdAndOrganizacionId(7L, 20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> usuarioService.obtenerUsuario(7L, orgB)).isInstanceOf(RuntimeException.class);
        verify(usuarios).findByIdAndOrganizacionId(7L, 20L);

        ConvocatoriaRepository convocatorias = mock(ConvocatoriaRepository.class);
        ConvocatoriaService convocatoriaService = new ConvocatoriaService(convocatorias, mock(com.plataformaeventos.eventos.repositorio.EventoRepository.class));
        when(convocatorias.findByIdAndOrganizacionId(7L, 20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> convocatoriaService.obtenerPorId(7L, orgB)).isInstanceOf(RuntimeException.class);
        verify(convocatorias).findByIdAndOrganizacionId(7L, 20L);
    }

    @Test
    void postulacionesYAsignacionesRechazanElIdDeOtraOrganizacion() {
        PostulacionStaffRepository postulaciones = mock(PostulacionStaffRepository.class);
        when(postulaciones.findByIdAndOrganizacionId(7L, 20L)).thenReturn(Optional.empty());
        var postService = new com.plataformaeventos.postulacionesstaff.servicio.PostulacionStaffService(
                postulaciones, mock(ConvocatoriaRepository.class), mock(UsuarioRepository.class), membresias, mock(BitacoraService.class));
        assertThatThrownBy(() -> postService.cambiar(7L, orgB, true, null)).isInstanceOf(RuntimeException.class);
        verify(postulaciones).findByIdAndOrganizacionId(7L, 20L);

        AsignacionStaffRepository asignaciones = mock(AsignacionStaffRepository.class);
        when(asignaciones.findByIdAndOrganizacionId(7L, 20L)).thenReturn(Optional.empty());
        var asignacionService = new AsignacionStaffService(asignaciones, postulaciones, mock(BitacoraService.class));
        assertThatThrownBy(() -> asignacionService.cancelar(7L, orgB, null)).isInstanceOf(RuntimeException.class);
        verify(asignaciones).findByIdAndOrganizacionId(7L, 20L);
    }

    @Test
    void bitacoraEsInmutableTambienEnCallbacksJPA() throws Exception {
        Bitacora bitacora = new Bitacora(null, orgA, "CREAR", "TEST", "TEST", 1L, null, true, "ok");
        Method callback = Bitacora.class.getDeclaredMethod("impedirMutacion");
        callback.setAccessible(true);
        assertThatThrownBy(() -> callback.invoke(bitacora)).hasCauseInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> callback.invoke(bitacora)).hasCauseInstanceOf(IllegalStateException.class);
    }

    private static Organizacion org(Long id, String slug, String estado) {
        Organizacion org = new Organizacion("Organizacion " + slug, "org-" + slug);
        ReflectionTestUtils.setField(org, "id", id);
        ReflectionTestUtils.setField(org, "estado", estado);
        return org;
    }

    private ContextoOrganizacion contextoConSuscripcionesActivas() {
        SuscripcionOrganizacionService suscripciones = mock(SuscripcionOrganizacionService.class);
        when(suscripciones.tieneAccesoActivo(anyLong(), any())).thenReturn(true);
        return new ContextoOrganizacion(organizaciones, membresias, suscripciones);
    }
}
