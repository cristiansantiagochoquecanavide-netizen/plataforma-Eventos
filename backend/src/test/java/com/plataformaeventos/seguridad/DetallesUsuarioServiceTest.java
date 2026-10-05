package com.plataformaeventos.seguridad;

import com.plataformaeventos.permisos.modelo.Permiso;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DetallesUsuarioServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final DetallesUsuarioService servicio = new DetallesUsuarioService(usuarios);

    @Test
    void emiteRolesGlobalesActivosComoAuthoritiesAdemasDePermisos() {
        Usuario usuario = usuario("admin@eventos.local", rol("SAAS_ADMINISTRADOR", permiso("ROL_VER")));
        when(usuarios.findByCorreoConRolesYPermisos("admin@eventos.local")).thenReturn(Optional.of(usuario));

        UserDetails detalles = servicio.loadUserByUsername("admin@eventos.local");

        assertThat(detalles.getAuthorities())
                .extracting("authority")
                .contains("SAAS_ADMINISTRADOR", "ROL_VER");
    }

    @Test
    void noInventaAutoridadSaasParaAdministradorTenantSinRolGlobal() {
        Usuario usuario = usuario("tenant@eventos.local", rol("PARTICIPANTE", permiso("PERFIL_VER")));
        when(usuarios.findByCorreoConRolesYPermisos("tenant@eventos.local")).thenReturn(Optional.of(usuario));

        UserDetails detalles = servicio.loadUserByUsername("tenant@eventos.local");

        assertThat(detalles.getAuthorities())
                .extracting("authority")
                .contains("PARTICIPANTE", "PERFIL_VER")
                .doesNotContain("SAAS_ADMINISTRADOR");
    }

    private static Usuario usuario(String correo, Rol... roles) {
        Usuario usuario = new Usuario("Nombre", "Apellido", correo, "hash", EstadoUsuario.ACTIVO);
        usuario.asignarRoles(Set.of(roles));
        return usuario;
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
