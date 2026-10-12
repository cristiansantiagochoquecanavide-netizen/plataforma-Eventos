package com.plataformaeventos.componentesui;

import com.plataformaeventos.componentesui.dto.GuardarPrivilegiosGrupoSolicitud;
import com.plataformaeventos.componentesui.dto.PrivilegioComponenteSolicitud;
import com.plataformaeventos.componentesui.modelo.EstadoPrivilegioUi;
import com.plataformaeventos.componentesui.modelo.GrupoComponenteUiPrivilegio;
import com.plataformaeventos.componentesui.modelo.TipoComponenteUi;
import com.plataformaeventos.componentesui.repositorio.ComponenteUiPrivilegioRepository;
import com.plataformaeventos.componentesui.servicio.ComponenteUiRegistry;
import com.plataformaeventos.componentesui.servicio.ComponenteUiService;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.grupos.modelo.GrupoUsuario;
import com.plataformaeventos.grupos.repositorio.GrupoUsuarioRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComponenteUiServiceTest {
    private final GrupoUsuarioRepository grupos = mock(GrupoUsuarioRepository.class);
    private final ComponenteUiPrivilegioRepository privilegios = mock(ComponenteUiPrivilegioRepository.class);
    private ComponenteUiService servicio;
    private Organizacion org;
    private GrupoUsuario grupo;

    @BeforeEach
    void setUp() {
        servicio = new ComponenteUiService(new ComponenteUiRegistry(), grupos, privilegios);
        org = org(1L);
        grupo = grupo(10L, org);
    }

    @Test
    void rechazaIdDeComponenteNoRegistradoAlGuardarPrivilegios() {
        when(grupos.findByIdAndOrganizacionId(10L, 1L)).thenReturn(Optional.of(grupo));

        var solicitud = new GuardarPrivilegiosGrupoSolicitud(Set.of(
                new PrivilegioComponenteSolicitud("inventado.desde.cliente", Set.of(EstadoPrivilegioUi.VISIBLE))
        ));

        assertThatThrownBy(() -> servicio.guardarPrivilegiosGrupo(org, 10L, solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no registrado");
        verify(privilegios, never()).saveAll(any());
    }

    @Test
    void rechazaEstadoNoSoportadoPorElTipoRegistrado() {
        when(grupos.findByIdAndOrganizacionId(10L, 1L)).thenReturn(Optional.of(grupo));

        var solicitud = new GuardarPrivilegiosGrupoSolicitud(Set.of(
                new PrivilegioComponenteSolicitud("events.menu", Set.of(EstadoPrivilegioUi.EDITABLE))
        ));

        assertThatThrownBy(() -> servicio.guardarPrivilegiosGrupo(org, 10L, solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no soporta");
        verify(privilegios, never()).saveAll(any());
    }

    @Test
    void rechazaHabilitadoSinVisibleAlGuardarPrivilegios() {
        when(grupos.findByIdAndOrganizacionId(10L, 1L)).thenReturn(Optional.of(grupo));

        var solicitud = new GuardarPrivilegiosGrupoSolicitud(Set.of(
                new PrivilegioComponenteSolicitud("event.submit", Set.of(EstadoPrivilegioUi.HABILITADO))
        ));

        assertThatThrownBy(() -> servicio.guardarPrivilegiosGrupo(org, 10L, solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VISIBLE");
        verify(privilegios, never()).saveAll(any());
    }

    @Test
    void rechazaEditableSinVisibleAlGuardarPrivilegios() {
        when(grupos.findByIdAndOrganizacionId(10L, 1L)).thenReturn(Optional.of(grupo));

        var solicitud = new GuardarPrivilegiosGrupoSolicitud(Set.of(
                new PrivilegioComponenteSolicitud("event.form", Set.of(EstadoPrivilegioUi.EDITABLE))
        ));

        assertThatThrownBy(() -> servicio.guardarPrivilegiosGrupo(org, 10L, solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VISIBLE");
        verify(privilegios, never()).saveAll(any());
    }

    @Test
    void noPermiteGuardarPrivilegiosDeGrupoDeOtroTenant() {
        when(grupos.findByIdAndOrganizacionId(99L, 1L)).thenReturn(Optional.empty());

        var solicitud = new GuardarPrivilegiosGrupoSolicitud(Set.of(
                new PrivilegioComponenteSolicitud("events.menu", Set.of(EstadoPrivilegioUi.VISIBLE))
        ));

        assertThatThrownBy(() -> servicio.guardarPrivilegiosGrupo(org, 99L, solicitud))
                .isInstanceOf(ExcepcionNoEncontrado.class)
                .hasMessageContaining("Grupo no encontrado");
        verify(privilegios, never()).deleteByOrganizacionIdAndGrupoId(1L, 99L);
    }

    @Test
    void guardaPrivilegiosTenantScopedSoloConEstadosValidos() {
        when(grupos.findByIdAndOrganizacionId(10L, 1L)).thenReturn(Optional.of(grupo));
        when(privilegios.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var respuesta = servicio.guardarPrivilegiosGrupo(org, 10L, new GuardarPrivilegiosGrupoSolicitud(Set.of(
                new PrivilegioComponenteSolicitud("event.form", Set.of(EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE)),
                new PrivilegioComponenteSolicitud("event.submit", Set.of(EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO))
        )));

        assertThat(respuesta.idGrupo()).isEqualTo(10L);
        assertThat(respuesta.privilegios()).hasSize(2);
        verify(privilegios).deleteByOrganizacionIdAndGrupoId(1L, 10L);
        verify(privilegios).saveAll(any());
    }

    @Test
    void lecturaDePrivilegiosGrupoOmiteGrantsPersistidosDesconocidosONoSoportados() {
        when(grupos.findByIdAndOrganizacionId(10L, 1L)).thenReturn(Optional.of(grupo));
        when(privilegios.findByOrganizacionIdAndGrupoId(1L, 10L)).thenReturn(List.of(
                grant("events.menu", EstadoPrivilegioUi.VISIBLE),
                grant("inventado.desde.cliente", EstadoPrivilegioUi.VISIBLE),
                grant("events.menu", EstadoPrivilegioUi.EDITABLE)
        ));

        var respuesta = servicio.privilegiosGrupo(org, 10L);

        assertThat(respuesta.privilegios()).hasSize(1);
        assertThat(respuesta.privilegios().getFirst().claveComponente()).isEqualTo("events.menu");
        assertThat(respuesta.privilegios().getFirst().estados()).containsExactly(EstadoPrivilegioUi.VISIBLE);
    }

    @Test
    void privilegiosEfectivosAplicanLosGrantsDeGrupoComoRestriccionSobrePermisosDeNegocio() {
        when(privilegios.findActivosPorUsuarioYOrganizacion("ana@test.com", 1L)).thenReturn(List.of(
                grant("events.menu", EstadoPrivilegioUi.VISIBLE),
                grant("event.submit", EstadoPrivilegioUi.VISIBLE),
                grant("event.submit", EstadoPrivilegioUi.HABILITADO)
        ));

        var respuesta = servicio.privilegiosEfectivos(org, "ana@test.com", false,
                Set.of("EVENTO_VER", "EVENTO_CREAR"), Set.of());

        assertThat(respuesta.componentes().get("events.menu").estadosPermitidos()).containsExactly(EstadoPrivilegioUi.VISIBLE);
        assertThat(respuesta.componentes().get("event.submit").estadosPermitidos())
                .containsExactlyInAnyOrder(EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertThat(respuesta.componentes().get("events.list").permitido(EstadoPrivilegioUi.VISIBLE)).isTrue();
    }

    @Test
    void controlesSinGrantQuedanDenegadosPorDefecto() {
        when(privilegios.findActivosPorUsuarioYOrganizacion("ana@test.com", 1L)).thenReturn(List.of());

        var respuesta = servicio.privilegiosEfectivos(org, "ana@test.com", false);

        assertThat(respuesta.componentes().get("events.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isFalse();
        assertThat(respuesta.componentes()).doesNotContainKey("inventado.desde.cliente");
    }

    @Test
    void permisosDelRolGeneranVisibilidadBaseSinNecesidadDeGruposUi() {
        when(privilegios.findActivosPorUsuarioYOrganizacion("participante@test.com", 1L)).thenReturn(List.of());

        var respuesta = servicio.privilegiosEfectivos(org, "participante@test.com", false,
                Set.of("EVENTO_VER", "INSCRIPCION_CREAR", "INSCRIPCION_VER_PROPIAS", "PERFIL_VER"),
                Set.of("PARTICIPANTE"));

        assertThat(respuesta.componentes().get("profile.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isTrue();
        assertThat(respuesta.componentes().get("events.search.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isTrue();
        assertThat(respuesta.componentes().get("event.registration.create").permitido(EstadoPrivilegioUi.HABILITADO)).isTrue();
        assertThat(respuesta.componentes().get("inscriptions.list").permitido(EstadoPrivilegioUi.VISIBLE)).isTrue();
        assertThat(respuesta.componentes().get("users.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isFalse();
        assertThat(respuesta.componentes().get("roles.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isFalse();
        assertThat(respuesta.componentes().get("events.create").permitido(EstadoPrivilegioUi.HABILITADO)).isFalse();
    }

    @Test
    void rolesGlobalesSaasNoSeInfierenDePermisosTenantConElMismoNombre() {
        when(privilegios.findActivosPorUsuarioYOrganizacion("tenant-admin@test.com", 1L)).thenReturn(List.of());
        when(privilegios.findActivosPorUsuarioYOrganizacion("saas-admin@test.com", 1L)).thenReturn(List.of());

        var tenant = servicio.privilegiosEfectivos(org, "tenant-admin@test.com", true,
                Set.of("ROL_VER", "ROL_CREAR", "PERMISO_ASIGNAR"), Set.of("ADMINISTRADOR"));
        var saas = servicio.privilegiosEfectivos(org, "saas-admin@test.com", true,
                Set.of(), Set.of("SAAS_ADMINISTRADOR"));

        assertThat(tenant.componentes().get("roles.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isFalse();
        assertThat(tenant.componentes().get("roles.create").permitido(EstadoPrivilegioUi.HABILITADO)).isFalse();
        assertThat(saas.componentes().get("roles.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isTrue();
        assertThat(saas.componentes().get("roles.create").permitido(EstadoPrivilegioUi.HABILITADO)).isTrue();
    }

    @Test
    void menusDeGestionSiguenLasMismasCapacidadesQueLasRutasYNoMezclanAdministracionDeMiembros() {
        when(privilegios.findActivosPorUsuarioYOrganizacion("organizador@test.com", 1L)).thenReturn(List.of());

        var respuesta = servicio.privilegiosEfectivos(org, "organizador@test.com", false,
                Set.of("EVENTO_CREAR", "RECURSO_CREAR", "ESPACIO_CREAR", "ORGANIZACION_GESTIONAR"),
                Set.of("ORGANIZADOR"));

        assertThat(respuesta.componentes().get("events.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isTrue();
        assertThat(respuesta.componentes().get("categories.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isTrue();
        assertThat(respuesta.componentes().get("resources.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isTrue();
        assertThat(respuesta.componentes().get("spaces.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isTrue();
        assertThat(respuesta.componentes().get("members.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isFalse();
        assertThat(respuesta.componentes().get("roles.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isFalse();
    }

    @Test
    void todosLosComponentesRegistradosTienenUnaPoliticaBaseExplicita() {
        var registry = new ComponenteUiRegistry();

        assertThat(registry.listar()).allSatisfy(definicion ->
                assertThat(registry.permisosBase(definicion.clave()))
                        .as("politica de " + definicion.clave())
                        .doesNotContain("__SIN_POLITICA__"));
    }

    @Test
    void unaConcesionUiNoPuedeAmpliarPermisosDeNegocio() {
        when(privilegios.findActivosPorUsuarioYOrganizacion("participante@test.com", 1L)).thenReturn(List.of(
                grant("events.create", EstadoPrivilegioUi.VISIBLE),
                grant("events.create", EstadoPrivilegioUi.HABILITADO)
        ));

        var respuesta = servicio.privilegiosEfectivos(org, "participante@test.com", false,
                Set.of("EVENTO_VER"), Set.of("PARTICIPANTE"));

        assertThat(respuesta.componentes().get("events.create").permitido(EstadoPrivilegioUi.VISIBLE)).isFalse();
        assertThat(respuesta.componentes().get("events.create").permitido(EstadoPrivilegioUi.HABILITADO)).isFalse();
    }

    @Test
    void unaConcesionDeGrupoPuedeDejarUnComponenteEnSoloLectura() {
        when(privilegios.findActivosPorUsuarioYOrganizacion("gestor@test.com", 1L)).thenReturn(List.of(
                grant("event.form", EstadoPrivilegioUi.VISIBLE)
        ));

        var respuesta = servicio.privilegiosEfectivos(org, "gestor@test.com", false,
                Set.of("EVENTO_EDITAR"), Set.of("ORGANIZADOR"));

        assertThat(respuesta.componentes().get("event.form").permitido(EstadoPrivilegioUi.VISIBLE)).isTrue();
        assertThat(respuesta.componentes().get("event.form").permitido(EstadoPrivilegioUi.EDITABLE)).isFalse();
    }

    @Test
    void consultaEfectivaDependeDeGruposYMembresiasActivasEnElRepositorio() throws NoSuchMethodException {
        var consulta = ComponenteUiPrivilegioRepository.class
                .getMethod("findActivosPorUsuarioYOrganizacion", String.class, Long.class)
                .getAnnotation(org.springframework.data.jpa.repository.Query.class);

        assertThat(consulta.value())
                .contains("g.activo = true")
                .contains("m.activo = true")
                .contains("g.organizacion.id = :organizacion")
                .contains("p.organizacion.id = :organizacion");
    }

    @Test
    void administradorTenantRecuperaSoloControlesDeConfiguracionMarcados() {
        when(privilegios.findActivosPorUsuarioYOrganizacion("admin@test.com", 1L)).thenReturn(List.of());

        var respuesta = servicio.privilegiosEfectivos(org, "admin@test.com", true);

        assertThat(respuesta.componentes().get("groups.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isTrue();
        assertThat(respuesta.componentes().get("ui-privileges.save").permitido(EstadoPrivilegioUi.HABILITADO)).isTrue();
        assertThat(respuesta.componentes().get("events.menu").permitido(EstadoPrivilegioUi.VISIBLE)).isFalse();
    }

    @Test
    void catalogoRegistraNavegacionYControlesDeGrupoConTiposEstadosYRecuperacionEsperados() {
        var registry = new ComponenteUiRegistry();

        assertMenuNoRecuperacion(registry, "profile.menu");
        assertMenuNoRecuperacion(registry, "users.menu");
        assertMenuNoRecuperacion(registry, "participants.menu");
        assertMenuNoRecuperacion(registry, "roles.menu");
        assertMenuNoRecuperacion(registry, "members.menu");
        assertMenuNoRecuperacion(registry, "audit.menu");
        assertMenuNoRecuperacion(registry, "events.search.menu");
        assertMenuNoRecuperacion(registry, "events.menu");
        assertMenuNoRecuperacion(registry, "categories.menu");
        assertMenuNoRecuperacion(registry, "resources.menu");
        assertMenuNoRecuperacion(registry, "spaces.menu");
        assertMenuNoRecuperacion(registry, "convocations.menu");
        assertMenuNoRecuperacion(registry, "staff-applications.menu");
        assertMenuNoRecuperacion(registry, "staff-assignments.menu");

        assertControl(registry, "groups.activate", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "groups.deactivate", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "groups.members.assign", TipoComponenteUi.FORMULARIO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
        assertControl(registry, "groups.roles.assign", TipoComponenteUi.FORMULARIO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
        assertControl(registry, "groups.assignments.save", TipoComponenteUi.ACCION, true,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertThat(registry.listar().stream()
                .map(definicion -> definicion.clave())
                .filter(clave -> clave.startsWith("groups.") && clave.endsWith(".save"))
                .toList())
                .containsExactly("groups.assignments.save");
        assertThat(registry.buscar("groups.members.save")).isEmpty();
        assertThat(registry.buscar("groups.roles.save")).isEmpty();
    }

    @Test
    void catalogoRegistraControlesDeUsuariosYRolesConTiposEstadosYRecuperacionEsperados() {
        var registry = new ComponenteUiRegistry();

        assertMenuNoRecuperacion(registry, "members.menu");
        assertControl(registry, "members.list", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "members.add", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "members.toggle-state", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "members.form.user", TipoComponenteUi.CAMPO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
        assertControl(registry, "members.form.roles", TipoComponenteUi.CAMPO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);

        assertMenuNoRecuperacion(registry, "users.menu");
        assertControl(registry, "users.list", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "users.create", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "users.edit", TipoComponenteUi.FORMULARIO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
        assertControl(registry, "users.activate", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "users.deactivate", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "users.save", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);

        assertMenuNoRecuperacion(registry, "roles.menu");
        assertControl(registry, "roles.list", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "roles.select", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "roles.create", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "roles.permissions.save", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "roles.save", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "roles.edit", TipoComponenteUi.FORMULARIO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
        assertControl(registry, "roles.permissions.assign", TipoComponenteUi.FORMULARIO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
    }

    @Test
    void catalogoRegistraControlesDeEventosYParticipantesConTiposEstadosYNoRecuperacionEsperados() {
        var registry = new ComponenteUiRegistry();

        assertMenuNoRecuperacion(registry, "events.menu");
        assertControl(registry, "events.list", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "events.filters", TipoComponenteUi.FORMULARIO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
        assertControl(registry, "events.create", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "events.edit", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "event.form", TipoComponenteUi.FORMULARIO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
        assertControl(registry, "event.submit", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "event.capacity", TipoComponenteUi.CAMPO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
        assertControl(registry, "event.summary", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "event.registration.create", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "event.responsibles.assign", TipoComponenteUi.FORMULARIO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);

        assertMenuNoRecuperacion(registry, "participants.menu");
        assertControl(registry, "participants.list", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "participants.filters", TipoComponenteUi.FORMULARIO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
        assertControl(registry, "participants.export", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "participants.history", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "participants.status.toggle", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "participants.detail", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "inscriptions.list", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "inscriptions.cancel", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "profile.edit", TipoComponenteUi.FORMULARIO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
        assertControl(registry, "profile.save", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
    }

    @Test
    void catalogoRegistraControlesDeStaffYAuditoriaConTiposEstadosYNoRecuperacionEsperados() {
        var registry = new ComponenteUiRegistry();

        assertMenuNoRecuperacion(registry, "convocations.menu");
        assertControl(registry, "convocations.list", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "convocations.create", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "convocations.close", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "convocations.cancel", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);

        assertMenuNoRecuperacion(registry, "staff-applications.menu");
        assertControl(registry, "staff-applications.form", TipoComponenteUi.FORMULARIO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
        assertControl(registry, "staff-applications.create", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "staff-applications.manage", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "staff-applications.accept", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "staff-applications.reject", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);

        assertMenuNoRecuperacion(registry, "staff-assignments.menu");
        assertControl(registry, "staff-assignments.create", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "staff-assignments.cancel", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);

        assertMenuNoRecuperacion(registry, "audit.menu");
        assertControl(registry, "audit.list", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "audit.filters", TipoComponenteUi.FORMULARIO, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);
    }

    @Test
    void catalogoRegistraControlesDeCategoriasEspaciosRecursosYAsignacionesConTiposEstadosEsperados() {
        var registry = new ComponenteUiRegistry();

        assertMenuNoRecuperacion(registry, "categories.menu");
        assertControl(registry, "categories.list", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "categories.create", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "categories.edit", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "categories.delete", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);

        assertMenuNoRecuperacion(registry, "spaces.menu");
        assertControl(registry, "spaces.list", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "spaces.create", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "spaces.edit", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);

        assertMenuNoRecuperacion(registry, "resources.menu");
        assertControl(registry, "resources.inventory.list", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "resource-assignments.list", TipoComponenteUi.TEXTO_INFORMATIVO, false,
                EstadoPrivilegioUi.VISIBLE);
        assertControl(registry, "resources.create", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "resources.edit", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "resources.change-state", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "resources.assign", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "resource-assignments.create", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
        assertControl(registry, "resource-assignments.release", TipoComponenteUi.ACCION, false,
                EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
    }

    private void assertMenuNoRecuperacion(ComponenteUiRegistry registry, String clave) {
        assertControl(registry, clave, TipoComponenteUi.MENU, false, EstadoPrivilegioUi.VISIBLE);
    }

    private void assertControl(ComponenteUiRegistry registry, String clave, TipoComponenteUi tipo,
                               boolean recuperacion, EstadoPrivilegioUi... estados) {
        var definicion = registry.buscar(clave).orElseThrow();

        assertThat(definicion.tipo()).isEqualTo(tipo);
        assertThat(definicion.controlRecuperacion()).isEqualTo(recuperacion);
        assertThat(definicion.estadosSoportados()).containsExactlyInAnyOrder(estados);
    }

    private GrupoComponenteUiPrivilegio grant(String componente, EstadoPrivilegioUi estado) {
        return new GrupoComponenteUiPrivilegio(org, grupo, componente, estado);
    }

    private Organizacion org(Long id) {
        Organizacion organizacion = new Organizacion("Org" + id, "org" + id);
        ReflectionTestUtils.setField(organizacion, "id", id);
        return organizacion;
    }

    private GrupoUsuario grupo(Long id, Organizacion organizacion) {
        GrupoUsuario nuevoGrupo = new GrupoUsuario(organizacion, "Operadores", null);
        ReflectionTestUtils.setField(nuevoGrupo, "id", id);
        return nuevoGrupo;
    }
}
