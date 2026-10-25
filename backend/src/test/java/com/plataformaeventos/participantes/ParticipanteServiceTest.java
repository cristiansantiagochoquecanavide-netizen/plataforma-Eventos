package com.plataformaeventos.participantes;

import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.participantes.dto.CambiarEstadoParticipanteSolicitud;
import com.plataformaeventos.participantes.dto.PaginaParticipantesRespuesta;
import com.plataformaeventos.participantes.dto.ParticipanteDetalleRespuesta;
import com.plataformaeventos.participantes.dto.ParticipanteResumenRespuesta;
import com.plataformaeventos.participantes.repositorio.ParticipanteRepository;
import com.plataformaeventos.participantes.servicio.ParticipanteService;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParticipanteServiceTest {

    private final ParticipanteRepository participanteRepository = mock(ParticipanteRepository.class);
    private final InscripcionRepository inscripcionRepository = mock(InscripcionRepository.class);

    private ParticipanteService participanteService;
    private Organizacion organizacion;

    @BeforeEach
    void setUp() {
        participanteService = new ParticipanteService(participanteRepository, inscripcionRepository);
        organizacion = new Organizacion("Organización Demo", "demo");
        ReflectionTestUtils.setField(organizacion, "id", 1L);
    }

    @Test
    void listarParticipantesExitoso() {
        Usuario usuario = new Usuario("Juan", "Perez", "juan@test.com", "hash", EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", 10L);
        ReflectionTestUtils.setField(usuario, "fechaCreacion", OffsetDateTime.now().minusMonths(1));

        when(participanteRepository.buscarParticipantes(eq(1L), eq(null), eq(""), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(usuario)));
        when(inscripcionRepository.countByParticipanteIdAndOrganizacionId(10L, 1L)).thenReturn(2L);
        when(inscripcionRepository.countByParticipanteIdAndOrganizacionIdAndEstado(10L, 1L, EstadoInscripcion.CONFIRMADA)).thenReturn(1L);
        when(inscripcionRepository.countByParticipanteIdAndOrganizacionIdAndEstado(10L, 1L, EstadoInscripcion.CANCELADA)).thenReturn(1L);
        when(inscripcionRepository.findFirstByParticipanteIdAndOrganizacionIdOrderByFechaInscripcionDesc(10L, 1L)).thenReturn(Optional.empty());

        PaginaParticipantesRespuesta respuesta = participanteService.listarParticipantes(null, null, null, 0, 10, organizacion);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.elementos()).hasSize(1);
        ParticipanteResumenRespuesta item = respuesta.elementos().get(0);
        assertThat(item.idUsuario()).isEqualTo(10L);
        assertThat(item.nombres()).isEqualTo("Juan");
        assertThat(item.totalInscripciones()).isEqualTo(2L);
        assertThat(item.inscripcionesConfirmadas()).isEqualTo(1L);
        assertThat(item.inscripcionesCanceladas()).isEqualTo(1L);
    }

    @Test
    void obtenerDetalleParticipanteExitoso() {
        Usuario usuario = new Usuario("Maria", "Gomez", "maria@test.com", "hash", EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", 20L);

        Evento evento = new Evento("Congreso de Software", null, OffsetDateTime.now(), OffsetDateTime.now().plusDays(2), "Auditorio", EstadoEvento.PUBLICADO, mock(com.plataformaeventos.categorias.modelo.Categoria.class));
        ReflectionTestUtils.setField(evento, "id", 50L);

        Inscripcion inscripcion = new Inscripcion(evento, usuario, "QR-12345");
        ReflectionTestUtils.setField(inscripcion, "id", 100L);

        when(participanteRepository.buscarPorIdYOrganizacion(20L, 1L)).thenReturn(Optional.of(usuario));
        when(participanteRepository.buscarEstadoParticipante(20L, 1L)).thenReturn(Optional.of(EstadoUsuario.INACTIVO));
        when(inscripcionRepository.findByParticipanteIdAndOrganizacionIdConEvento(20L, 1L)).thenReturn(List.of(inscripcion));

        ParticipanteDetalleRespuesta detalle = participanteService.obtenerDetalleParticipante(20L, organizacion);

        assertThat(detalle).isNotNull();
        assertThat(detalle.idUsuario()).isEqualTo(20L);
        assertThat(detalle.nombres()).isEqualTo("Maria");
        assertThat(detalle.estado()).isEqualTo(EstadoUsuario.INACTIVO);
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(detalle.totalInscripciones()).isEqualTo(1L);
        assertThat(detalle.inscripciones()).hasSize(1);
        assertThat(detalle.inscripciones().get(0).tituloEvento()).isEqualTo("Congreso de Software");
    }

    @Test
    void obtenerDetalleParticipanteNoEncontrado() {
        when(participanteRepository.buscarPorIdYOrganizacion(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participanteService.obtenerDetalleParticipante(999L, organizacion))
                .isInstanceOf(ExcepcionNoEncontrado.class)
                .hasMessageContaining("Participante no encontrado");
    }

    @Test
    void cambiarEstadoActualizaSoloEstadoParticipanteDelTenantSinInactivarCuentaNiMembresia() {
        Usuario usuario = new Usuario("Carlos", "Roca", "carlos@test.com", "hash", EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", 30L);
        Organizacion otraOrganizacion = new Organizacion("Organización B", "org-b");
        ReflectionTestUtils.setField(otraOrganizacion, "id", 2L);

        when(participanteRepository.buscarPorIdYOrganizacion(30L, 1L)).thenReturn(Optional.of(usuario));
        when(participanteRepository.buscarParticipantes(eq(2L), eq(null), eq(""), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(usuario)));
        when(participanteRepository.cambiarEstadoParticipante(30L, 1L, EstadoUsuario.INACTIVO)).thenReturn(1);
        when(participanteRepository.buscarEstadoParticipante(30L, 1L)).thenReturn(Optional.of(EstadoUsuario.INACTIVO));
        when(participanteRepository.buscarEstadoParticipante(30L, 2L)).thenReturn(Optional.of(EstadoUsuario.ACTIVO));
        when(inscripcionRepository.countByParticipanteIdAndOrganizacionId(30L, 1L)).thenReturn(0L);
        when(inscripcionRepository.countByParticipanteIdAndOrganizacionId(30L, 2L)).thenReturn(0L);

        ParticipanteResumenRespuesta res = participanteService.cambiarEstado(
                30L,
                new CambiarEstadoParticipanteSolicitud(EstadoUsuario.INACTIVO),
                organizacion
        );
        ParticipanteResumenRespuesta enOtraOrganizacion = participanteService
                .listarParticipantes(null, null, null, 0, 10, otraOrganizacion)
                .elementos()
                .get(0);

        assertThat(res.estado()).isEqualTo(EstadoUsuario.INACTIVO);
        assertThat(enOtraOrganizacion.estado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        verify(participanteRepository).cambiarEstadoParticipante(30L, 1L, EstadoUsuario.INACTIVO);
        verify(participanteRepository, never()).save(usuario);
    }

    @Test
    void exportarCsvExitoso() {
        Usuario usuario = new Usuario("=1+1", "Vargas", "lucia@test.com", "hash", EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", 40L);

        when(participanteRepository.listarTodosParaExportacion(1L, null, "", null)).thenReturn(List.of(usuario));
        when(inscripcionRepository.countByParticipanteIdAndOrganizacionId(40L, 1L)).thenReturn(0L);

        byte[] csv = participanteService.exportarParticipantesCsv(null, null, null, organizacion);

        assertThat(csv).isNotEmpty();
        String contenido = new String(csv);
        assertThat(contenido).contains("Nombres,Apellidos,Correo,Estado");
        assertThat(contenido).contains("\"'=1+1\"");
        assertThat(contenido).contains("lucia@test.com");
    }
}
