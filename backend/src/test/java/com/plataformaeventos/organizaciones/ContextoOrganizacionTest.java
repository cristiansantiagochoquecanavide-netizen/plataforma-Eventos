package com.plataformaeventos.organizaciones;

import com.plataformaeventos.excepciones.ExcepcionNoAutorizado;
import com.plataformaeventos.configuracion.AdministradorInicializador;
import com.plataformaeventos.organizaciones.dto.AgregarMembresiaSolicitud;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.organizaciones.servicio.OrganizacionService;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.seguridad.TokenRefrescoService;
import com.plataformaeventos.usuarios.dto.CrearUsuarioSolicitud;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.usuarios.servicio.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.saas.servicio.SuscripcionOrganizacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ContextoOrganizacionTest {
    private final OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
    private final MembresiaOrganizacionRepository membresias = mock(MembresiaOrganizacionRepository.class);
    private final SuscripcionOrganizacionService suscripcionesActivas = mock(SuscripcionOrganizacionService.class);
    private final ContextoOrganizacion contexto = new ContextoOrganizacion(organizaciones, membresias, suscripcionesActivas);
    private final Authentication auth = mock(Authentication.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @BeforeEach
    void suscripcionesVigentesPorDefecto() {
        when(suscripcionesActivas.tieneAccesoActivo(anyLong(), any())).thenReturn(true);
    }

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

    @Test
    void rechazaAccesoTenantCuandoLaSuscripcionFueSuspendidaPeroSaasAdminPuedeAdministrar() {
        Organizacion organizacion = organizacion(12L, "ACTIVA");
        SuscripcionOrganizacionService suscripciones = mock(SuscripcionOrganizacionService.class);
        ContextoOrganizacion conSuscripciones = new ContextoOrganizacion(organizaciones, membresias, suscripciones);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("persona@correo.test");
        when(request.getHeader(ContextoOrganizacion.CABECERA)).thenReturn("12");
        when(organizaciones.findById(12L)).thenReturn(Optional.of(organizacion));
        when(suscripciones.tieneAccesoActivo(eq(12L), any())).thenReturn(false);

        assertThatThrownBy(() -> conSuscripciones.resolver(auth, request))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("suscripcion");

        org.springframework.security.core.Authentication saas =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "saas@plataforma.test", "N/A", java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("SAAS_ADMINISTRADOR")));
        when(suscripciones.tieneAccesoActivo(eq(12L), any())).thenReturn(false);
        assertThat(conSuscripciones.resolver(saas, request)).isSameAs(organizacion);
    }


    @Test
    void autorizacionTenantConsultaRolesDirectosYRolesDeGruposActivosSinSaas() throws NoSuchMethodException {
        Query permisoGrupo = MembresiaOrganizacionRepository.class
                .getMethod("tienePermisoGrupoActivo", String.class, Long.class, String.class)
                .getAnnotation(Query.class);
        Query adminGrupo = MembresiaOrganizacionRepository.class
                .getMethod("esAdministradorGrupoActivo", String.class, Long.class)
                .getAnnotation(Query.class);

        assertThat(permisoGrupo.value())
                .contains("g.activo = true")
                .contains("m.activo = true")
                .contains("g.organizacion.id = :organizacion")
                .contains("r.nombre <> 'SAAS_ADMINISTRADOR'");
        assertThat(adminGrupo.value())
                .contains("g.activo = true")
                .contains("m.activo = true")
                .contains("g.organizacion.id = :organizacion")
                .doesNotContain("SAAS_ADMINISTRADOR");
    }

    @Test
    void rechazaRolSaasAdministradorComoMembresiaDeOrganizacion() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        ContextoOrganizacion contextoMock = mock(ContextoOrganizacion.class);
        OrganizacionService servicio = new OrganizacionService(organizaciones, membresias, usuarios, roles, contextoMock);
        Organizacion activa = organizacion(7L, "ACTIVA");
        when(contextoMock.resolver(any(), any())).thenReturn(activa);
        when(contextoMock.esAdministrador(any(), any())).thenReturn(true);
        when(organizaciones.findById(7L)).thenReturn(Optional.of(activa));
        Usuario usuario = new Usuario("Ana", "Tenant", "ana@test", "hash", EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", 9L);
        when(usuarios.findById(9L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicio.agregarMembresia(7L, new AgregarMembresiaSolicitud(9L, Set.of("SAAS_ADMINISTRADOR")), "actor@test", auth, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SAAS_ADMINISTRADOR");

        verify(membresias, never()).save(any());
    }

    @Test
    void rechazaRolSaasAdministradorEnCreacionTenantDeUsuario() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        UsuarioService servicio = new UsuarioService(usuarios, roles, mock(PasswordEncoder.class), mock(TokenRefrescoService.class), membresias);
        Organizacion organizacion = organizacion(3L, "ACTIVA");

        assertThatThrownBy(() -> servicio.crearUsuario(new CrearUsuarioSolicitud("Ana", "Tenant", "ana@test", "Clave123!", Set.of("SAAS_ADMINISTRADOR")), organizacion))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SAAS_ADMINISTRADOR");

        verify(membresias, never()).save(any());
    }

    @Test
    void administradorInicialRecibeRolGlobalSaasYAdminTenantPrincipal() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
        org.springframework.security.crypto.password.PasswordEncoder codificador = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        AdministradorInicializador inicializador = new AdministradorInicializador();
        Rol saas = rol(1L, "SAAS_ADMINISTRADOR");
        Rol adminTenant = rol(2L, "ADMINISTRADOR");
        Organizacion principal = organizacion(1L, "ACTIVA");
        when(roles.findByNombre("SAAS_ADMINISTRADOR")).thenReturn(Optional.of(saas));
        when(roles.findByNombre("ADMINISTRADOR")).thenReturn(Optional.of(adminTenant));
        when(roles.findByNombre("ORGANIZADOR")).thenReturn(Optional.of(rol(3L, "ORGANIZADOR")));
        when(roles.findByNombre("STAFF")).thenReturn(Optional.of(rol(4L, "STAFF")));
        when(roles.findByNombre("PARTICIPANTE")).thenReturn(Optional.of(rol(5L, "PARTICIPANTE")));
        when(usuarios.findByCorreoConRoles("admin@eventos.local")).thenReturn(Optional.empty());
        when(usuarios.existsByCorreo(anyString())).thenReturn(true);
        when(codificador.encode("Admin123!")).thenReturn("hash");
        when(organizaciones.findBySlug("principal")).thenReturn(Optional.of(principal));
        when(membresias.findByUsuarioIdAndOrganizacionIdConRoles(null, 1L)).thenReturn(Optional.empty());

        ReflectionTestUtils.invokeMethod(inicializador, "crearAdministrador", usuarios, roles, organizaciones, membresias, codificador, "admin@eventos.local", "Admin123!");

        verify(usuarios).save(argThat(usuario -> usuario.getRoles().contains(saas) && !usuario.getRoles().contains(adminTenant)));
        verify(membresias).save(argThat(membresia -> membresia.getRoles().contains(adminTenant) && !membresia.getRoles().contains(saas)));
    }

    private Rol rol(Long id, String nombre) {
        Rol rol = new Rol(nombre, nombre, EstadoRol.ACTIVO);
        ReflectionTestUtils.setField(rol, "id", id);
        return rol;
    }

    private Organizacion organizacion(Long id, String estado) {
        Organizacion organizacion = new Organizacion("Org", "org");
        ReflectionTestUtils.setField(organizacion, "id", id);
        ReflectionTestUtils.setField(organizacion, "estado", estado);
        return organizacion;
    }
}
