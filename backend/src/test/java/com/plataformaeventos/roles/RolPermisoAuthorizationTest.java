package com.plataformaeventos.roles;

import com.plataformaeventos.permisos.controlador.PermisoController;
import com.plataformaeventos.roles.controlador.RolController;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RolPermisoAuthorizationTest {

    @Test
    void endpointsGlobalesDeRolesRespetanPermisosTenantOAdminSaas() {
        Map<String, String> autorizaciones = Arrays.stream(RolController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                .collect(Collectors.toMap(Method::getName,
                        method -> method.getAnnotation(PreAuthorize.class).value()));

        assertThat(autorizaciones).containsExactlyInAnyOrderEntriesOf(Map.of(
                "listarRoles", "hasAuthority('ROL_VER') or hasAuthority('SAAS_ADMINISTRADOR')",
                "obtenerRol", "hasAuthority('ROL_VER') or hasAuthority('SAAS_ADMINISTRADOR')",
                "crearRol", "hasAuthority('ROL_CREAR') or hasAuthority('SAAS_ADMINISTRADOR')",
                "actualizarRol", "hasAuthority('ROL_EDITAR') or hasAuthority('SAAS_ADMINISTRADOR')",
                "obtenerPermisos", "hasAuthority('PERMISO_VER') or hasAuthority('SAAS_ADMINISTRADOR')",
                "asignarPermisos", "hasAuthority('PERMISO_ASIGNAR') or hasAuthority('SAAS_ADMINISTRADOR')"
        ));
    }

    @Test
    void endpointGlobalDePermisosPermitePermisoTenantOAdminSaas() throws NoSuchMethodException {
        Method listarPermisos = PermisoController.class.getDeclaredMethod("listarPermisos");

        assertThat(listarPermisos.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('PERMISO_VER') or hasAuthority('SAAS_ADMINISTRADOR')");
    }

    @Test
    void consultaDeResponsablesTenantUsaRolesDeMembresiaActiva() throws NoSuchMethodException {
        var metodo = UsuarioRepository.class.getDeclaredMethod(
                "findResponsablesDisponiblesPorOrganizacion",
                java.util.Set.class,
                Long.class
        );

        String consulta = metodo.getAnnotation(org.springframework.data.jpa.repository.Query.class).value();

        assertThat(consulta).contains("join membresia.roles rol");
        assertThat(consulta).contains("membresia.activo = true");
        assertThat(consulta).doesNotContain("join usuario.roles rol");
    }

    @Test
    void consultaDeAsignacionResponsableTenantExigeRolActivoEnMembresia() throws NoSuchMethodException {
        var metodo = UsuarioRepository.class.getDeclaredMethod(
                "findResponsableActivoPorOrganizacionYRol",
                Long.class,
                Long.class,
                java.util.Set.class
        );

        String consulta = metodo.getAnnotation(org.springframework.data.jpa.repository.Query.class).value();

        assertThat(consulta).contains("join membresia.roles rol");
        assertThat(consulta).contains("membresia.activo = true");
        assertThat(consulta).contains("rol.nombre in :roles");
        assertThat(consulta).doesNotContain("join usuario.roles rol");
    }

}
