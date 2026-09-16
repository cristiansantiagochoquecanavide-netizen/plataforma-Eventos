package com.plataformaeventos.convocatorias.postulaciones;

import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.convocatorias.modelo.Convocatoria;
import com.plataformaeventos.convocatorias.modelo.TipoPersonal;
import com.plataformaeventos.convocatorias.repositorio.ConvocatoriaRepository;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ejecutar solo contra PostgreSQL temporal con CU17_INTEGRACION=true. */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "CU17_INTEGRACION", matches = "true")
class GestionPostulacionesIntegracionTest {

    @Autowired private GestionPostulacionesService gestion;
    @Autowired private PostulacionStaffService postulaciones;
    @Autowired private PostulacionStaffRepository postulacionRepository;
    @Autowired private AsignacionStaffRepository asignacionRepository;
    @Autowired private ConvocatoriaRepository convocatoriaRepository;
    @Autowired private EventoRepository eventoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void flywayRegistraAsignacionesYPermisoDeEncargado() {
        assertThat(jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '16'", Boolean.class)).isTrue();
        assertThat(jdbc.queryForList("""
                SELECT rol.nombre FROM rol
                JOIN rol_permiso USING (id_rol)
                JOIN permiso USING (id_permiso)
                WHERE permiso.nombre = 'CONVOCATORIA_GESTIONAR_POSTULACIONES'
                """, String.class)).contains("ADMINISTRADOR", "ORGANIZADOR", "STAFF");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = current_schema() AND table_name = 'asignacion_staff'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void consultaSeleccionaYRechazaPostulacionesDelMismoFlujoCU16() {
        DatosPrueba datos = crearDatos("decisiones");
        Usuario seleccionada = crearUsuario("seleccionada");
        Usuario rechazada = crearUsuario("rechazada");
        var primera = postulaciones.postularse(datos.convocatoria().getId(), seleccionada.getCorreo());
        var segunda = postulaciones.postularse(datos.convocatoria().getId(), rechazada.getCorreo());

        var lista = gestion.listarPostulantes(datos.convocatoria().getId(), datos.gestor().getCorreo());
        assertThat(lista).hasSize(2);
        assertThat(lista).extracting("correo")
                .containsExactlyInAnyOrder(seleccionada.getCorreo(), rechazada.getCorreo());

        assertThat(gestion.seleccionar(primera.id(), datos.gestor().getCorreo()).estado())
                .isEqualTo(EstadoPostulacion.SELECCIONADA);
        assertThat(gestion.rechazar(segunda.id(), datos.gestor().getCorreo()).estado())
                .isEqualTo(EstadoPostulacion.RECHAZADA);
        assertThat(asignacionRepository.countByPostulacionConvocatoriaId(datos.convocatoria().getId()))
                .isEqualTo(1);
        assertThat(postulacionRepository.findById(primera.id()).orElseThrow().getEstado())
                .isEqualTo(EstadoPostulacion.SELECCIONADA);
    }

    @Test
    void seleccionDuplicadaYConvocatoriaCerradaSonRechazadas() {
        DatosPrueba datos = crearDatos("duplicada");
        Usuario candidato = crearUsuario("candidato");
        var postulacion = postulaciones.postularse(datos.convocatoria().getId(), candidato.getCorreo());

        gestion.seleccionar(postulacion.id(), datos.gestor().getCorreo());
        assertThatThrownBy(() -> gestion.seleccionar(postulacion.id(), datos.gestor().getCorreo()))
                .isInstanceOf(IllegalArgumentException.class);

        Usuario otro = crearUsuario("convocatoria-cerrada");
        Convocatoria cerrada = convocatoriaRepository.saveAndFlush(new Convocatoria(
                datos.evento(), TipoPersonal.VOLUNTARIO, "Apoyo", 2, LocalDate.now().plusDays(1)));
        var pendiente = postulaciones.postularse(cerrada.getId(), otro.getCorreo());
        cerrada.cerrar();
        convocatoriaRepository.saveAndFlush(cerrada);

        assertThatThrownBy(() -> gestion.seleccionar(pendiente.id(), datos.gestor().getCorreo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("convocatorias abiertas");
    }

    private DatosPrueba crearDatos(String sufijo) {
        String unico = UUID.randomUUID().toString();
        Rol organizador = rolRepository.findByNombre("ORGANIZADOR").orElseThrow();
        Usuario gestor = new Usuario("Gestor", sufijo, "gestor-" + unico + "@example.com", "hash",
                EstadoUsuario.ACTIVO);
        gestor.asignarRoles(Set.of(organizador));
        usuarioRepository.saveAndFlush(gestor);

        Categoria categoria = categoriaRepository.saveAndFlush(new Categoria("CU17-" + unico, "CU17"));
        Evento evento = new Evento("Evento CU17 " + sufijo, "Prueba", OffsetDateTime.now().plusDays(5),
                OffsetDateTime.now().plusDays(6), "Auditorio", EstadoEvento.PUBLICADO, categoria);
        evento.asignarResponsables(Set.of(gestor));
        evento = eventoRepository.saveAndFlush(evento);
        Convocatoria convocatoria = convocatoriaRepository.saveAndFlush(new Convocatoria(evento,
                TipoPersonal.STAFF, "Apoyo", 3, LocalDate.now().plusDays(2)));
        return new DatosPrueba(gestor, evento, convocatoria, unico);
    }

    private Usuario crearUsuario(String sufijo) {
        return usuarioRepository.saveAndFlush(new Usuario("Candidato", sufijo,
                "candidato-" + UUID.randomUUID() + "@example.com", "hash", EstadoUsuario.ACTIVO));
    }

    private record DatosPrueba(Usuario gestor, Evento evento, Convocatoria convocatoria, String unico) {
    }
}
