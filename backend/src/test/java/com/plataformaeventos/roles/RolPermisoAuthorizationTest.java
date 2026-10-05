package com.plataformaeventos.roles;

import com.plataformaeventos.permisos.controlador.PermisoController;
import com.plataformaeventos.roles.controlador.RolController;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RolPermisoAuthorizationTest {

    @Test
    void endpointsGlobalesDeRolesSonSoloParaSaasAdministrador() {
        assertThat(Arrays.stream(RolController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                .map(method -> method.getAnnotation(PreAuthorize.class).value()))
                .isNotEmpty()
                .allMatch("hasAuthority('SAAS_ADMINISTRADOR')"::equals);
    }

    @Test
    void endpointGlobalDePermisosEsSoloParaSaasAdministrador() throws NoSuchMethodException {
        Method listarPermisos = PermisoController.class.getDeclaredMethod("listarPermisos");

        assertThat(listarPermisos.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('SAAS_ADMINISTRADOR')");
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
