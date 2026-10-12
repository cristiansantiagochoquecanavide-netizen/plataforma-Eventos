package com.plataformaeventos.autenticacion;

import com.plataformaeventos.autenticacion.dto.UsuarioActualRespuesta;
import com.plataformaeventos.autenticacion.servicio.AutenticacionService;
import com.plataformaeventos.grupos.repositorio.GrupoUsuarioRepository;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.permisos.modelo.Permiso;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.seguridad.JwtService;
import com.plataformaeventos.seguridad.RecuperacionClaveService;
import com.plataformaeventos.seguridad.TokenRefrescoService;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutenticacionServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RolRepository roles = mock(RolRepository.class);
    private final OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
    private final PasswordEncoder codificador = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final TokenRefrescoService tokens = mock(TokenRefrescoService.class);
    private final RecuperacionClaveService recuperacion = mock(RecuperacionClaveService.class);
    private final MembresiaOrganizacionRepository membresias = mock(MembresiaOrganizacionRepository.class);
    private final GrupoUsuarioRepository grupos = mock(GrupoUsuarioRepository.class);
    private final AutenticacionService servicio = new AutenticacionService(
            usuarios,
            roles,
            organizaciones,
            membresias,
            codificador,
            jwt,
            tokens,
            recuperacion,
            membresias,
            grupos
    );

    @Test
    void usuarioActualMantieneRolesGlobalesSeparadosDelPerfilTenantActivo() {
        Usuario usuario = usuario("admin@eventos.local", rol("SAAS_ADMINISTRADOR", permiso("ROL_VER")));
        MembresiaOrganizacion membresia = membresia(usuario, 9L, rol("ADMINISTRADOR", permiso("USUARIO_VER"), permiso("EVENTO_VER")));
        when(usuarios.findByCorreoConRolesYPermisos("admin@eventos.local")).thenReturn(Optional.of(usuario));
        when(membresias.findByUsuarioCorreoAndOrganizacionIdAndActivoTrueConRolesYPermisos("admin@eventos.local", 9L))
                .thenReturn(Optional.of(membresia));

        UsuarioActualRespuesta respuesta = servicio.obtenerUsuarioActual("ADMIN@EVENTOS.LOCAL", 9L);

        assertThat(respuesta.roles()).containsExactly("SAAS_ADMINISTRADOR");
        assertThat(respuesta.permisos()).containsExactly("ROL_VER");
        assertThat(respuesta.perfilOrganizacionActiva()).isNotNull();
        assertThat(respuesta.perfilOrganizacionActiva().idOrganizacion()).isEqualTo(9L);
        assertThat(respuesta.perfilOrganizacionActiva().roles()).containsExactly("ADMINISTRADOR");
        assertThat(respuesta.perfilOrganizacionActiva().permisos()).containsExactly("EVENTO_VER", "USUARIO_VER");
        assertThat(respuesta.perfilOrganizacionActiva().administrador()).isTrue();
    }

    @Test
    void usuarioActualPerfilTenantCombinaRolesDirectosYRolesDeGruposActivosSinEscalarSaas() {
        Usuario usuario = usuario("persona@eventos.local", rol("PARTICIPANTE", permiso("EVENTO_VER")));
        MembresiaOrganizacion membresia = membresia(usuario, 9L, rol("STAFF", permiso("INSCRIPCION_VER")));
        when(usuarios.findByCorreoConRolesYPermisos("persona@eventos.local")).thenReturn(Optional.of(usuario));
        when(membresias.findByUsuarioCorreoAndOrganizacionIdAndActivoTrueConRolesYPermisos("persona@eventos.local", 9L))
                .thenReturn(Optional.of(membresia));
        when(grupos.findRolesActivosHeredadosPorUsuarioYOrganizacion("persona@eventos.local", 9L))
                .thenReturn(Set.of(
                        rol("ORGANIZADOR", permiso("EVENTO_CREAR")),
                        rol("SAAS_ADMINISTRADOR", permiso("ROL_ADMINISTRAR"))
                ));

        UsuarioActualRespuesta respuesta = servicio.obtenerUsuarioActual("persona@eventos.local", 9L);

        assertThat(respuesta.roles()).containsExactly("PARTICIPANTE");
        assertThat(respuesta.permisos()).containsExactly("EVENTO_VER");
        assertThat(respuesta.perfilOrganizacionActiva().roles()).containsExactly("ORGANIZADOR", "STAFF");
        assertThat(respuesta.perfilOrganizacionActiva().permisos()).containsExactly("EVENTO_CREAR", "INSCRIPCION_VER");
        assertThat(respuesta.perfilOrganizacionActiva().administrador()).isFalse();
    }

    @Test
    void saasAdminRecibeCapacidadesAdministrativasEnCualquierOrganizacionSeleccionada() {
        Usuario usuario = usuario("admin@eventos.local", rol("SAAS_ADMINISTRADOR", permiso("ROL_VER")));
        Rol adminTenant = rol("ADMINISTRADOR", permiso("EVENTO_VER"), permiso("USUARIO_VER"));
        when(usuarios.findByCorreoConRolesYPermisos("admin@eventos.local")).thenReturn(Optional.of(usuario));
        when(membresias.findByUsuarioCorreoAndOrganizacionIdAndActivoTrueConRolesYPermisos("admin@eventos.local", 99L))
                .thenReturn(Optional.empty());
        when(roles.findByNombre("ADMINISTRADOR")).thenReturn(Optional.of(adminTenant));

        UsuarioActualRespuesta respuesta = servicio.obtenerUsuarioActual("admin@eventos.local", 99L);

        assertThat(respuesta.roles()).containsExactly("SAAS_ADMINISTRADOR");
        assertThat(respuesta.perfilOrganizacionActiva()).isNotNull();
        assertThat(respuesta.perfilOrganizacionActiva().idOrganizacion()).isEqualTo(99L);
        assertThat(respuesta.perfilOrganizacionActiva().roles()).containsExactly("ADMINISTRADOR");
        assertThat(respuesta.perfilOrganizacionActiva().permisos()).containsExactly("EVENTO_VER", "USUARIO_VER");
        assertThat(respuesta.perfilOrganizacionActiva().administrador()).isTrue();
    }

    @Test
    void usuarioSinMembresiaYQueNoEsSaasNoRecibePerfilTenantInventado() {
        Usuario usuario = usuario("persona@eventos.local", rol("PARTICIPANTE", permiso("PERFIL_VER")));
        when(usuarios.findByCorreoConRolesYPermisos("persona@eventos.local")).thenReturn(Optional.of(usuario));
        when(membresias.findByUsuarioCorreoAndOrganizacionIdAndActivoTrueConRolesYPermisos("persona@eventos.local", 99L))
                .thenReturn(Optional.empty());

        UsuarioActualRespuesta respuesta = servicio.obtenerUsuarioActual("persona@eventos.local", 99L);

        assertThat(respuesta.perfilOrganizacionActiva()).isNull();
    }

    private static Usuario usuario(String correo, Rol... roles) {
        Usuario usuario = new Usuario("Nombre", "Apellido", correo, "hash", EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", 1L);
        usuario.asignarRoles(Set.of(roles));
        return usuario;
    }

    private static MembresiaOrganizacion membresia(Usuario usuario, Long idOrganizacion, Rol... roles) {
        Organizacion organizacion = new Organizacion("Organizacion", "organizacion");
        ReflectionTestUtils.setField(organizacion, "id", idOrganizacion);
        MembresiaOrganizacion membresia = new MembresiaOrganizacion(usuario, organizacion);
        membresia.asignarRoles(Set.of(roles));
        return membresia;
    }

    private static Rol rol(String nombre, Permiso... permisos) {
        Rol rol = new Rol(nombre, nombre, EstadoRol.ACTIVO);
        rol.asignarPermisos(Set.of(permisos));
        return rol;
    }

    private static Permiso permiso(String nombre) {
        Permiso permiso = BeanUtils.instantiateClass(Permiso.class);
        ReflectionTestUtils.setField(permiso, "nombre", nombre);
        return permiso;
    }
}
