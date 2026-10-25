package com.plataformaeventos.grupos;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.grupos.dto.ActualizarGrupoUsuarioSolicitud;
import com.plataformaeventos.grupos.dto.AsignarMiembrosGrupoSolicitud;
import com.plataformaeventos.grupos.dto.AsignarRolesGrupoSolicitud;
import com.plataformaeventos.grupos.dto.CrearGrupoUsuarioSolicitud;
import com.plataformaeventos.grupos.modelo.GrupoUsuario;
import com.plataformaeventos.grupos.repositorio.GrupoUsuarioRepository;
import com.plataformaeventos.grupos.servicio.GrupoUsuarioService;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrupoUsuarioServiceTest {

    private final GrupoUsuarioRepository grupos = mock(GrupoUsuarioRepository.class);
    private final RolRepository roles = mock(RolRepository.class);
    private GrupoUsuarioService servicio;
    private Organizacion organizacion;

    @BeforeEach
    void setUp() {
        servicio = new GrupoUsuarioService(grupos, roles);
        organizacion = org(1L);
    }

    @Test
    void creaGrupoTenantActivoConNombreUnicoPorOrganizacion() {
        when(grupos.existsByOrganizacionIdAndNombreIgnoreCase(1L, "Coordinadores")).thenReturn(false);
        when(grupos.save(any(GrupoUsuario.class))).thenAnswer(invocacion -> {
            GrupoUsuario grupo = invocacion.getArgument(0);
            ReflectionTestUtils.setField(grupo, "id", 10L);
            return grupo;
        });

        var respuesta = servicio.crear(organizacion, new CrearGrupoUsuarioSolicitud("  Coordinadores  ", "Equipo operativo"));

        assertThat(respuesta.id()).isEqualTo(10L);
        assertThat(respuesta.nombre()).isEqualTo("Coordinadores");
        assertThat(respuesta.descripcion()).isEqualTo("Equipo operativo");
        assertThat(respuesta.activo()).isTrue();
        verify(grupos).save(any(GrupoUsuario.class));
    }

    @Test
    void noPermiteDetalleDeGrupoDeOtraOrganizacion() {
        when(grupos.findByIdAndOrganizacionIdConMiembrosYRoles(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(organizacion, 99L))
                .isInstanceOf(ExcepcionNoEncontrado.class)
                .hasMessageContaining("Grupo no encontrado");
    }

    @Test
    void actualizaSoloGrupoDelTenantActivo() {
        GrupoUsuario grupo = grupo(7L, organizacion, "Staff", null);
        when(grupos.findByIdAndOrganizacionId(7L, 1L)).thenReturn(Optional.of(grupo));
        when(grupos.existsByOrganizacionIdAndNombreIgnoreCase(1L, "Produccion")).thenReturn(false);

        var respuesta = servicio.actualizar(organizacion, 7L, new ActualizarGrupoUsuarioSolicitud("Produccion", "Mesa tecnica"));

        assertThat(respuesta.nombre()).isEqualTo("Produccion");
        assertThat(respuesta.descripcion()).isEqualTo("Mesa tecnica");
        assertThat(respuesta.activo()).isTrue();
    }

    @Test
    void desactivaGrupoTenantSinEliminarlo() {
        GrupoUsuario grupo = grupo(7L, organizacion, "Staff", null);
        when(grupos.findByIdAndOrganizacionId(7L, 1L)).thenReturn(Optional.of(grupo));

        var respuesta = servicio.desactivar(organizacion, 7L);

        assertThat(respuesta.activo()).isFalse();
    }

    @Test
    void reactivaGrupoDesactivadoDelTenant() {
        GrupoUsuario grupo = grupo(7L, organizacion, "Staff", null);
        grupo.desactivar();
        when(grupos.findByIdAndOrganizacionId(7L, 1L)).thenReturn(Optional.of(grupo));

        var respuesta = servicio.activar(organizacion, 7L);

        assertThat(respuesta.activo()).isTrue();
        assertThat(grupo.isActivo()).isTrue();
    }

    @Test
    void asignaSoloMiembrosActivosDeLaMismaOrganizacion() {
        GrupoUsuario grupo = grupo(7L, organizacion, "Staff", null);
        Usuario ana = usuario(2L, EstadoUsuario.ACTIVO);
        Usuario luis = usuario(3L, EstadoUsuario.ACTIVO);
        when(grupos.findByIdAndOrganizacionId(7L, 1L)).thenReturn(Optional.of(grupo));
        when(grupos.findUsuariosConMembresiaActivaYCuentaActiva(1L, Set.of(2L, 3L))).thenReturn(List.of(ana, luis));

        var respuesta = servicio.asignarMiembros(organizacion, 7L, new AsignarMiembrosGrupoSolicitud(Set.of(2L, 3L)));

        assertThat(respuesta.miembros()).extracting("idUsuario").containsExactlyInAnyOrder(2L, 3L);
        assertThat(grupo.getMiembros()).containsExactlyInAnyOrder(ana, luis);
    }

    @Test
    void busquedaDeMiembrosNoDependeDelEstadoParticipante() throws NoSuchMethodException {
        Query consulta = GrupoUsuarioRepository.class
                .getMethod("findUsuariosConMembresiaActivaYCuentaActiva", Long.class, java.util.Collection.class)
                .getAnnotation(Query.class);

        assertThat(consulta.value()).doesNotContain("estadoParticipante");
    }

    @Test
    void rechazaMiembrosInactivosOCruzadosPorTenant() {
        GrupoUsuario grupo = grupo(7L, organizacion, "Staff", null);
        Usuario ana = usuario(2L, EstadoUsuario.ACTIVO);
        when(grupos.findByIdAndOrganizacionId(7L, 1L)).thenReturn(Optional.of(grupo));
        when(grupos.findUsuariosConMembresiaActivaYCuentaActiva(1L, Set.of(2L, 99L))).thenReturn(List.of(ana));

        assertThatThrownBy(() -> servicio.asignarMiembros(organizacion, 7L, new AsignarMiembrosGrupoSolicitud(Set.of(2L, 99L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("miembros");
        assertThat(grupo.getMiembros()).isEmpty();
    }

    @Test
    void consultaRolesHeredadosSoloUsaGrupoActivoMembresiaActivaYTenantSeleccionado() throws NoSuchMethodException {
        Query consulta = GrupoUsuarioRepository.class
                .getMethod("findRolesActivosHeredadosPorUsuarioYOrganizacion", String.class, Long.class)
                .getAnnotation(Query.class);

        assertThat(consulta.value())
                .contains("g.activo = true")
                .contains("m.activo = true")
                .contains("m.usuario = u")
                .contains("m.organizacion = g.organizacion")
                .contains("g.organizacion.id = :organizacion")
                .contains("r.nombre <> 'SAAS_ADMINISTRADOR'");
    }

    @Test
    void listaRolesDisponiblesIncluyeActivosAsignablesYExcluyeInactivosYSaasAdministrador() {
        Rol staff = new Rol("STAFF", null, EstadoRol.ACTIVO);
        ReflectionTestUtils.setField(staff, "id", 4L);
        Rol coordinador = new Rol("COORDINADOR", null, EstadoRol.ACTIVO);
        ReflectionTestUtils.setField(coordinador, "id", 5L);
        Rol inactivo = new Rol("INACTIVO", null, EstadoRol.INACTIVO);
        ReflectionTestUtils.setField(inactivo, "id", 6L);
        Rol saas = new Rol("SAAS_ADMINISTRADOR", null, EstadoRol.ACTIVO);
        ReflectionTestUtils.setField(saas, "id", 1L);
        when(roles.findAll()).thenReturn(List.of(inactivo, saas, coordinador, staff));

        var respuesta = servicio.listarRolesDisponibles();

        assertThat(respuesta).extracting("idRol").containsExactly(5L, 4L);
        assertThat(respuesta).extracting("nombre").containsExactly("COORDINADOR", "STAFF");
    }

    @Test
    void asignaRolesTenantUsablesYRechazaSaasAdministrador() {
        GrupoUsuario grupo = grupo(7L, organizacion, "Staff", null);
        Rol staff = new Rol("STAFF", null, EstadoRol.ACTIVO);
        ReflectionTestUtils.setField(staff, "id", 4L);
        when(grupos.findByIdAndOrganizacionId(7L, 1L)).thenReturn(Optional.of(grupo));
        when(roles.findAllById(Set.of(4L))).thenReturn(List.of(staff));

        var respuesta = servicio.asignarRoles(organizacion, 7L, new AsignarRolesGrupoSolicitud(Set.of(4L)));

        assertThat(respuesta.roles()).extracting("nombre").containsExactly("STAFF");
        assertThat(grupo.getRoles()).containsExactly(staff);
    }

    @Test
    void noAsignaSaasAdministradorAGrupoTenant() {
        GrupoUsuario grupo = grupo(7L, organizacion, "Staff", null);
        Rol saas = new Rol("SAAS_ADMINISTRADOR", null, EstadoRol.ACTIVO);
        ReflectionTestUtils.setField(saas, "id", 1L);
        when(grupos.findByIdAndOrganizacionId(7L, 1L)).thenReturn(Optional.of(grupo));
        when(roles.findAllById(Set.of(1L))).thenReturn(List.of(saas));

        assertThatThrownBy(() -> servicio.asignarRoles(organizacion, 7L, new AsignarRolesGrupoSolicitud(Set.of(1L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SAAS_ADMINISTRADOR");
        verify(grupos, never()).save(any());
    }

    private Organizacion org(Long id) {
        Organizacion org = new Organizacion("Org", "org" + id);
        ReflectionTestUtils.setField(org, "id", id);
        return org;
    }

    private GrupoUsuario grupo(Long id, Organizacion org, String nombre, String descripcion) {
        GrupoUsuario grupo = new GrupoUsuario(org, nombre, descripcion);
        ReflectionTestUtils.setField(grupo, "id", id);
        return grupo;
    }

    private Usuario usuario(Long id, EstadoUsuario estado) {
        Usuario usuario = new Usuario("Nombre" + id, "Apellido" + id, "u" + id + "@test.com", "hash", estado);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
