package com.plataformaeventos.configuracion;

import com.plataformaeventos.asignacionesstaff.repositorio.AsignacionStaffRepository;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.convocatorias.repositorio.ConvocatoriaRepository;
import com.plataformaeventos.espacios.repositorio.EspacioRepository;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.postulacionesstaff.repositorio.PostulacionStaffRepository;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.usuarios.modelo.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DatasetEmpresasHistoricasTest {
    @Autowired private OrganizacionRepository organizaciones;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RolRepository roles;
    @Autowired private MembresiaOrganizacionRepository membresias;
    @Autowired private CategoriaRepository categorias;
    @Autowired private EspacioRepository espacios;
    @Autowired private EventoRepository eventos;
    @Autowired private ConvocatoriaRepository convocatorias;
    @Autowired private PostulacionStaffRepository postulaciones;
    @Autowired private AsignacionStaffRepository asignaciones;
    @Autowired private InscripcionRepository inscripciones;
    @PersistenceContext private EntityManager entityManager;

    @BeforeEach
    void crearRolesBase() {
        roles.saveAll(List.of(
                new Rol("ADMINISTRADOR", "Administra la organización", EstadoRol.ACTIVO),
                new Rol("ORGANIZADOR", "Organiza actividades", EstadoRol.ACTIVO),
                new Rol("STAFF", "Apoya actividades", EstadoRol.ACTIVO),
                new Rol("PARTICIPANTE", "Participa en actividades", EstadoRol.ACTIVO)
        ));
        roles.flush();
    }

    @Test
    void creaCincoEmpresasConSieteMesesDeActividadYEsIdempotente() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

        sembrar(encoder);
        entityManager.flush();
        entityManager.clear();

        assertThat(DatasetEmpresasHistoricas.empresas()).hasSize(5);
        assertThat(organizaciones.count()).isEqualTo(5);
        assertThat(usuarios.count()).isEqualTo(145);
        assertThat(categorias.count()).isEqualTo(10);
        assertThat(espacios.count()).isEqualTo(10);
        assertThat(eventos.count()).isEqualTo(70);
        assertThat(convocatorias.count()).isEqualTo(35);
        assertThat(postulaciones.count()).isEqualTo(105);
        assertThat(asignaciones.count()).isGreaterThan(20);
        assertThat(inscripciones.count()).isGreaterThan(800);

        List<Usuario> cuentasDataset = usuarios.findAll().stream()
                .filter(usuario -> usuario.getCorreo().endsWith("@sumaqamana.local")
                        || usuario.getCorreo().endsWith("@nexoandino.local")
                        || usuario.getCorreo().endsWith("@laestacion.local")
                        || usuario.getCorreo().endsWith("@camaravalle.local")
                        || usuario.getCorreo().endsWith("@clubillimani.local"))
                .toList();
        assertThat(cuentasDataset).hasSize(145)
                .allSatisfy(usuario -> assertThat(encoder.matches(
                        DemoDatosInicializador.CLAVE_DEMO, usuario.getClaveHash())).isTrue());

        YearMonth primerMes = YearMonth.now(ZoneOffset.ofHours(-4)).minusMonths(6);
        for (var empresa : DatasetEmpresasHistoricas.empresas()) {
            var organizacion = organizaciones.findBySlug(empresa.slug()).orElseThrow();
            List<Evento> eventosEmpresa = eventos.findAll().stream()
                    .filter(evento -> evento.getOrganizacion().getId().equals(organizacion.getId()))
                    .toList();
            assertThat(eventosEmpresa).hasSize(DatasetEmpresasHistoricas.EVENTOS_POR_EMPRESA);
            assertThat(eventosEmpresa.stream().map(evento -> YearMonth.from(evento.getFechaInicio())).min(YearMonth::compareTo))
                    .contains(primerMes);
            assertThat(eventosEmpresa.stream().map(Evento::getNombre))
                    .allSatisfy(nombre -> assertThat(nombre).doesNotContain("Demo", "Evento 1", "Empresa 1"));
        }

        List<Inscripcion> inscripcionesSeed = inscripciones.findAll();
        assertThat(inscripcionesSeed).allSatisfy(inscripcion -> {
            assertThat(inscripcion.getCodigoQr()).matches("[0-9a-f-]{36}");
            assertThat(inscripcion.getFechaInscripcion()).isBefore(inscripcion.getEvento().getFechaInicio());
        });
        OffsetDateTime primeraInscripcion = inscripcionesSeed.stream()
                .map(Inscripcion::getFechaInscripcion).min(OffsetDateTime::compareTo).orElseThrow();
        assertThat(primeraInscripcion.isBefore(OffsetDateTime.now(ZoneOffset.ofHours(-4)).minusMonths(6)))
                .isTrue();

        long empresasAntes = organizaciones.count();
        long usuariosAntes = usuarios.count();
        long eventosAntes = eventos.count();
        long convocatoriasAntes = convocatorias.count();
        long postulacionesAntes = postulaciones.count();
        long asignacionesAntes = asignaciones.count();
        long inscripcionesAntes = inscripciones.count();
        sembrar(encoder);
        entityManager.flush();

        assertThat(organizaciones.count()).isEqualTo(empresasAntes);
        assertThat(usuarios.count()).isEqualTo(usuariosAntes);
        assertThat(eventos.count()).isEqualTo(eventosAntes);
        assertThat(convocatorias.count()).isEqualTo(convocatoriasAntes);
        assertThat(postulaciones.count()).isEqualTo(postulacionesAntes);
        assertThat(asignaciones.count()).isEqualTo(asignacionesAntes);
        assertThat(inscripciones.count()).isEqualTo(inscripcionesAntes);
    }

    private void sembrar(BCryptPasswordEncoder encoder) {
        DatasetEmpresasHistoricas.sembrar(organizaciones, usuarios, roles, membresias, categorias, espacios,
                eventos, convocatorias, postulaciones, asignaciones, inscripciones, encoder, null);
    }
}
