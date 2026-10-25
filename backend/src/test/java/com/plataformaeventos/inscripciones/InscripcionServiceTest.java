package com.plataformaeventos.inscripciones;

import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.inscripciones.dto.DisponibilidadEventoRespuesta;
import com.plataformaeventos.inscripciones.dto.InscripcionRespuesta;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.inscripciones.servicio.InscripcionService;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InscripcionServiceTest {

    private final InscripcionRepository inscripcionRepository = mock(InscripcionRepository.class);
    private final EventoRepository eventoRepository = mock(EventoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

    private final InscripcionService inscripcionService = new InscripcionService(
            inscripcionRepository,
            eventoRepository,
            usuarioRepository
    );

    @Test
    void inscribirExitosoConAforoDisponible() {
        Usuario usuario = usuarioConId(1L, "participante@eventos.com");
        Evento evento = eventoConId(10L, EstadoEvento.PUBLICADO, 50, OffsetDateTime.now().plusDays(5));

        when(usuarioRepository.findByCorreo("participante@eventos.com")).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(inscripcionRepository.findByEventoIdAndParticipanteId(10L, 1L)).thenReturn(Optional.empty());
        when(inscripcionRepository.countByEventoIdAndEstado(10L, EstadoInscripcion.CONFIRMADA)).thenReturn(10L);
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(invocation -> {
            Inscripcion ins = invocation.getArgument(0);
            ReflectionTestUtils.setField(ins, "id", 100L);
            return ins;
        });

        InscripcionRespuesta respuesta = inscripcionService.inscribir(10L, "participante@eventos.com");

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.idInscripcion()).isEqualTo(100L);
        assertThat(respuesta.idEvento()).isEqualTo(10L);
        assertThat(respuesta.estado()).isEqualTo(EstadoInscripcion.CONFIRMADA);
        assertThat(respuesta.codigoQr()).startsWith("INS-10-1-");
    }

    @Test
    void inscribirLanzaExcepcionSiEventoNoEstaPublicado() {
        Usuario usuario = usuarioConId(1L, "participante@eventos.com");
        Evento evento = eventoConId(10L, EstadoEvento.BORRADOR, 50, OffsetDateTime.now().plusDays(5));

        when(usuarioRepository.findByCorreo("participante@eventos.com")).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> inscripcionService.inscribir(10L, "participante@eventos.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo es posible inscribirse a eventos en estado PUBLICADO");
    }

    @Test
    void inscribirLanzaExcepcionSiAforoCompleto() {
        Usuario usuario = usuarioConId(1L, "participante@eventos.com");
        Evento evento = eventoConId(10L, EstadoEvento.PUBLICADO, 20, OffsetDateTime.now().plusDays(5));

        when(usuarioRepository.findByCorreo("participante@eventos.com")).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(inscripcionRepository.findByEventoIdAndParticipanteId(10L, 1L)).thenReturn(Optional.empty());
        when(inscripcionRepository.countByEventoIdAndEstado(10L, EstadoInscripcion.CONFIRMADA)).thenReturn(20L);

        assertThatThrownBy(() -> inscripcionService.inscribir(10L, "participante@eventos.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aforo completo");
    }

    @Test
    void inscribirLanzaExcepcionSiYaEstaInscrito() {
        Usuario usuario = usuarioConId(1L, "participante@eventos.com");
        Evento evento = eventoConId(10L, EstadoEvento.PUBLICADO, 50, OffsetDateTime.now().plusDays(5));
        Inscripcion activa = new Inscripcion(evento, usuario, "QR-123");

        when(usuarioRepository.findByCorreo("participante@eventos.com")).thenReturn(Optional.of(usuario));
        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(inscripcionRepository.findByEventoIdAndParticipanteId(10L, 1L)).thenReturn(Optional.of(activa));

        assertThatThrownBy(() -> inscripcionService.inscribir(10L, "participante@eventos.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya cuentas con una inscripción confirmada");
    }

    @Test
    void cancelarInscripcionExitosa() {
        Usuario usuario = usuarioConId(1L, "participante@eventos.com");
        Evento evento = eventoConId(10L, EstadoEvento.PUBLICADO, 50, OffsetDateTime.now().plusDays(5));
        Inscripcion inscripcion = new Inscripcion(evento, usuario, "QR-123");
        ReflectionTestUtils.setField(inscripcion, "id", 100L);

        when(usuarioRepository.findByCorreo("participante@eventos.com")).thenReturn(Optional.of(usuario));
        when(inscripcionRepository.findByIdAndParticipanteId(100L, 1L)).thenReturn(Optional.of(inscripcion));
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InscripcionRespuesta respuesta = inscripcionService.cancelarInscripcion(100L, "participante@eventos.com");

        assertThat(respuesta.estado()).isEqualTo(EstadoInscripcion.CANCELADA);
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
    }

    @Test
    void cancelarLanzaExcepcionSiEventoYaFinalizo() {
        Usuario usuario = usuarioConId(1L, "participante@eventos.com");
        Evento evento = eventoConId(10L, EstadoEvento.FINALIZADO, 50, OffsetDateTime.now().minusDays(2));
        Inscripcion inscripcion = new Inscripcion(evento, usuario, "QR-123");
        ReflectionTestUtils.setField(inscripcion, "id", 100L);

        when(usuarioRepository.findByCorreo("participante@eventos.com")).thenReturn(Optional.of(usuario));
        when(inscripcionRepository.findByIdAndParticipanteId(100L, 1L)).thenReturn(Optional.of(inscripcion));

        assertThatThrownBy(() -> inscripcionService.cancelarInscripcion(100L, "participante@eventos.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No es posible cancelar la inscripción a un evento que ya ha finalizado");
    }

    @Test
    void consultarDisponibilidadCalculaCuposCorrectamente() {
        Usuario usuario = usuarioConId(1L, "participante@eventos.com");
        Evento evento = eventoConId(10L, EstadoEvento.PUBLICADO, 100, OffsetDateTime.now().plusDays(5));

        when(eventoRepository.findById(10L)).thenReturn(Optional.of(evento));
        when(inscripcionRepository.countByEventoIdAndEstado(10L, EstadoInscripcion.CONFIRMADA)).thenReturn(25L);
        when(usuarioRepository.findByCorreo("participante@eventos.com")).thenReturn(Optional.of(usuario));
        when(inscripcionRepository.findByEventoIdAndParticipanteId(10L, 1L)).thenReturn(Optional.empty());

        DisponibilidadEventoRespuesta respuesta = inscripcionService.consultarDisponibilidad(10L, "participante@eventos.com");

        assertThat(respuesta.idEvento()).isEqualTo(10L);
        assertThat(respuesta.aforoMaximo()).isEqualTo(100);
        assertThat(respuesta.cuposOcupados()).isEqualTo(25L);
        assertThat(respuesta.cuposDisponibles()).isEqualTo(75);
        assertThat(respuesta.tieneCupo()).isTrue();
        assertThat(respuesta.estaInscrito()).isFalse();
    }

    private Usuario usuarioConId(Long id, String correo) {
        Usuario usuario = new Usuario("Juan", "Perez", correo, "Clave123", EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private Evento eventoConId(Long id, EstadoEvento estado, Integer aforo, OffsetDateTime fechaInicio) {
        Categoria categoria = new Categoria("General", "Descripcion");
        Evento evento = new Evento("Conferencia Tech", "Detalle", fechaInicio, fechaInicio.plusHours(4), "Aula Magna", estado, categoria);
        ReflectionTestUtils.setField(evento, "id", id);
        ReflectionTestUtils.setField(evento, "aforoMaximo", aforo);
        return evento;
    }
}
