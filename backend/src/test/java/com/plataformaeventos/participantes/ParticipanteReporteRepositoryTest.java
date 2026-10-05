package com.plataformaeventos.participantes;

import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.participantes.dto.ParticipanteResumenRespuesta;
import com.plataformaeventos.participantes.repositorio.ParticipanteRepository;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ParticipanteReporteRepositoryTest {

    @Autowired private ParticipanteRepository participantes;
    @Autowired private OrganizacionRepository organizaciones;
    @Autowired private CategoriaRepository categorias;
    @Autowired private EventoRepository eventos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RolRepository roles;
    @Autowired private MembresiaOrganizacionRepository membresias;
    @Autowired private InscripcionRepository inscripciones;

    @Test
    void resumenAgrupaInscripcionesYNoIncluyeParticipantesDeOtroTenant() {
        Organizacion tenantA = organizaciones.saveAndFlush(new Organizacion("Tenant A", "tenant-reporte-a"));
        Organizacion tenantB = organizaciones.saveAndFlush(new Organizacion("Tenant B", "tenant-reporte-b"));
        Rol rolParticipante = roles.saveAndFlush(new Rol("PARTICIPANTE", "Participante", EstadoRol.ACTIVO));
        Usuario usuario = usuarios.saveAndFlush(new Usuario(
                "Ana", "Prueba", "ana-reporte@example.test", "hash", EstadoUsuario.ACTIVO));
        MembresiaOrganizacion membresia = new MembresiaOrganizacion(usuario, tenantA);
        membresia.asignarRoles(Set.of(rolParticipante));
        membresias.saveAndFlush(membresia);

        Categoria categoria = categorias.saveAndFlush(new Categoria("Charlas", null, tenantA));
        Evento evento = new Evento("Charla", null, OffsetDateTime.now().plusDays(1), null,
                "Auditorio", EstadoEvento.PLANIFICADO, categoria);
        evento.asignarOrganizacion(tenantA);
        evento = eventos.saveAndFlush(evento);
        Inscripcion inscripcion = new Inscripcion(evento, usuario, "qr-reporte");
        inscripcion.asignarOrganizacion(tenantA);
        inscripciones.saveAndFlush(inscripcion);

        var reporteTenantA = participantes.buscarResumenParaReporte(
                tenantA.getId(), evento.getId(), null, EstadoUsuario.ACTIVO, PageRequest.of(0, 20));
        var reporteTenantB = participantes.buscarResumenParaReporte(
                tenantB.getId(), null, null, null, PageRequest.of(0, 20));

        assertThat(reporteTenantA.getContent()).singleElement().satisfies((ParticipanteResumenRespuesta fila) -> {
            assertThat(fila.idUsuario()).isEqualTo(usuario.getId());
            assertThat(fila.estado()).isEqualTo(EstadoUsuario.ACTIVO);
            assertThat(fila.totalInscripciones()).isEqualTo(1);
            assertThat(fila.inscripcionesConfirmadas()).isEqualTo(1);
            assertThat(fila.inscripcionesCanceladas()).isZero();
            assertThat(fila.ultimaInscripcion()).isNotNull();
        });
        assertThat(reporteTenantB.getContent()).isEmpty();
    }
}
