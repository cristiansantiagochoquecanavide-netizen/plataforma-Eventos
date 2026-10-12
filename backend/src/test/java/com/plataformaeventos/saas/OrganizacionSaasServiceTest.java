package com.plataformaeventos.saas;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.saas.dto.ActualizarEstadoOrganizacionSaasSolicitud;
import com.plataformaeventos.saas.dto.OrganizacionSaasFila;
import com.plataformaeventos.saas.modelo.EstadoSuscripcion;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import com.plataformaeventos.saas.servicio.OrganizacionSaasService;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizacionSaasServiceTest {
    private final OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
    private final OrganizacionSaasService servicio = new OrganizacionSaasService(organizaciones);

    @Test
    void paginaResumenIncluyeSuscripcionYConteoDeMiembrosSinExponerCamposStripe() {
        var inicio = OffsetDateTime.now().minusDays(2);
        var fila = new OrganizacionSaasFila(9L, "Casa Norte", "casa-norte", "ACTIVA", inicio,
                12L, PlanSuscripcion.PROFESIONAL, EstadoSuscripcion.ACTIVA, inicio, null);
        when(organizaciones.listarParaAdministracionSaas("", "", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(fila), PageRequest.of(0, 20), 1));

        var pagina = servicio.listar(0, 20, " ", null);

        assertThat(pagina.totalElementos()).isEqualTo(1);
        assertThat(pagina.organizaciones()).singleElement().satisfies(organizacion -> {
            assertThat(organizacion.nombre()).isEqualTo("Casa Norte");
            assertThat(organizacion.miembrosActivos()).isEqualTo(12);
            assertThat(organizacion.plan()).isEqualTo(PlanSuscripcion.PROFESIONAL);
            assertThat(organizacion.suscripcionVigente()).isTrue();
        });
    }

    @Test
    void filtraPorNombreSlugYEstadoConPaginacion() {
        when(organizaciones.listarParaAdministracionSaas("casa", "INACTIVA", PageRequest.of(1, 10)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        servicio.listar(1, 10, " casa ", " inactiva ");

        verify(organizaciones).listarParaAdministracionSaas("casa", "INACTIVA", PageRequest.of(1, 10));
    }

    @Test
    void permiteSuspenderYReactivarOrganizacionesSinBorrarHistorial() {
        Organizacion org = organizacion(9L, "casa-norte", "ACTIVA");
        when(organizaciones.findById(9L)).thenReturn(Optional.of(org));

        servicio.actualizarEstado(9L, new ActualizarEstadoOrganizacionSaasSolicitud("INACTIVA"));
        assertThat(org.getEstado()).isEqualTo("INACTIVA");

        servicio.actualizarEstado(9L, new ActualizarEstadoOrganizacionSaasSolicitud("ACTIVA"));
        assertThat(org.getEstado()).isEqualTo("ACTIVA");
    }

    @Test
    void noPermiteInactivarLaOrganizacionDeControlDeLaPlataforma() {
        Organizacion principal = organizacion(1L, "principal", "ACTIVA");
        when(organizaciones.findById(1L)).thenReturn(Optional.of(principal));

        assertThatThrownBy(() -> servicio.actualizarEstado(1L, new ActualizarEstadoOrganizacionSaasSolicitud("INACTIVA")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("principal");
    }

    @Test
    void informaCuandoLaOrganizacionNoExiste() {
        when(organizaciones.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizarEstado(99L, new ActualizarEstadoOrganizacionSaasSolicitud("ACTIVA")))
                .isInstanceOf(ExcepcionNoEncontrado.class);
    }

    @Test
    void auditaLaSuspensionEnElTenantAfectado() {
        BitacoraService bitacora = mock(BitacoraService.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        OrganizacionSaasService auditado = new OrganizacionSaasService(organizaciones, bitacora, usuarios);
        Organizacion club = organizacion(8L, "club-illimani", "ACTIVA");
        Usuario admin = new Usuario("Admin", "SaaS", "admin@eventos.local", "hash", EstadoUsuario.ACTIVO);
        Authentication autenticacion = mock(Authentication.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(autenticacion.getName()).thenReturn("admin@eventos.local");
        when(organizaciones.findById(8L)).thenReturn(Optional.of(club));
        when(usuarios.findByCorreo("admin@eventos.local")).thenReturn(Optional.of(admin));

        auditado.actualizarEstado(8L, new ActualizarEstadoOrganizacionSaasSolicitud("INACTIVA"), autenticacion, request);

        verify(bitacora).registrar(admin, club, "DESACTIVAR", "SAAS", "ORGANIZACION", 8L,
                null, true, "Estado de organización actualizado");
    }

    private Organizacion organizacion(Long id, String slug, String estado) {
        Organizacion organizacion = new Organizacion("Org " + slug, slug);
        ReflectionTestUtils.setField(organizacion, "id", id);
        organizacion.actualizarEstado(estado);
        return organizacion;
    }
}
