package com.plataformaeventos.eventos;

import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.espacios.modelo.Espacio;
import com.plataformaeventos.espacios.repositorio.EspacioRepository;
import com.plataformaeventos.eventos.dto.AsignarResponsablesSolicitud;
import com.plataformaeventos.eventos.dto.EventoRespuesta;
import com.plataformaeventos.eventos.dto.GuardarEventoSolicitud;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.eventos.servicio.EventoService;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventoServiceTest {

    private final EventoRepository eventoRepository = mock(EventoRepository.class);
    private final CategoriaRepository categoriaRepository = mock(CategoriaRepository.class);
    private final EspacioRepository espacioRepository = mock(EspacioRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final EventoService eventoService = new EventoService(
            eventoRepository,
            categoriaRepository,
            usuarioRepository,
            espacioRepository
    );

    @Test
    void crearEventoAsociaCategoriaYDevuelveCategoriaEnRespuesta() {
        Categoria categoria = categoriaConId(4L, "Academico");
        Espacio espacio = espacioConId(3L, 120);
        Usuario responsable = responsableConRol(8L, "ORGANIZADOR");
        GuardarEventoSolicitud solicitud = new GuardarEventoSolicitud(
                " Congreso de IA ",
                " Programa academico ",
                OffsetDateTime.now().plusDays(10),
                OffsetDateTime.now().plusDays(11),
                " Auditorio Central ",
                4L,
                3L,
                90,
                EstadoEvento.PUBLICADO,
                Set.of(8L)
        );
        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);

        when(categoriaRepository.findById(4L)).thenReturn(Optional.of(categoria));
        when(espacioRepository.findById(3L)).thenReturn(Optional.of(espacio));
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(responsable));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocacion -> {
            Evento evento = invocacion.getArgument(0);
            ReflectionTestUtils.setField(evento, "id", 33L);
            return evento;
        });

        EventoRespuesta respuesta = eventoService.crearEvento(solicitud);

        verify(eventoRepository).existeConflictoProgramacionCrearConFin(
                3L,
                solicitud.fechaInicio(),
                solicitud.fechaFin()
        );
        verify(eventoRepository).save(captor.capture());
        assertThat(captor.getValue().getCategoria()).isSameAs(categoria);
        assertThat(captor.getValue().getEspacio()).isSameAs(espacio);
        assertThat(captor.getValue().getAforoMaximo()).isEqualTo(90);
        assertThat(respuesta.id()).isEqualTo(33L);
        assertThat(respuesta.nombre()).isEqualTo("Congreso de IA");
        assertThat(respuesta.lugarPrincipal()).isEqualTo("Auditorio Central");
        assertThat(respuesta.categoria()).isNotNull();
        assertThat(respuesta.categoria().id()).isEqualTo(4L);
        assertThat(respuesta.categoria().nombre()).isEqualTo("Academico");
        assertThat(respuesta.categoriaNombre()).isEqualTo("Academico");
        assertThat(respuesta.espacio()).isNotNull();
        assertThat(respuesta.espacio().id()).isEqualTo(3L);
    }

    @Test
    void crearEventoPermiteResponsablesOmitidos() {
        Categoria categoria = categoriaConId(4L, "Academico");
        Espacio espacio = espacioConId(3L, 120);
        GuardarEventoSolicitud solicitud = new GuardarEventoSolicitud(
                "Congreso de IA",
                "Programa academico",
                OffsetDateTime.now().plusDays(10),
                null,
                "Auditorio Central",
                4L,
                3L,
                90,
                EstadoEvento.PUBLICADO,
                null
        );

        when(categoriaRepository.findById(4L)).thenReturn(Optional.of(categoria));
        when(espacioRepository.findById(3L)).thenReturn(Optional.of(espacio));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocacion -> {
            Evento evento = invocacion.getArgument(0);
            ReflectionTestUtils.setField(evento, "id", 34L);
            return evento;
        });

        EventoRespuesta respuesta = eventoService.crearEvento(solicitud);

        verify(eventoRepository).existeConflictoProgramacionCrearSinFin(
                3L,
                solicitud.fechaInicio()
        );
        assertThat(respuesta.id()).isEqualTo(34L);
        assertThat(respuesta.responsables()).isEmpty();
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void crearEventoFallaSiCategoriaNoExiste() {
        GuardarEventoSolicitud solicitud = new GuardarEventoSolicitud(
                "Evento",
                null,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(2),
                "Sala",
                99L,
                3L,
                50,
                EstadoEvento.PLANIFICADO,
                Set.of(1L)
        );
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventoService.crearEvento(solicitud))
                .isInstanceOf(ExcepcionNoEncontrado.class)
                .hasMessageContaining("Categoria no encontrada");
    }

    @Test
    void actualizarEventoPermiteCambiarCategoria() {
        Categoria anterior = categoriaConId(1L, "General");
        Categoria nueva = categoriaConId(2L, "Tecnologia");
        Espacio espacio = espacioConId(5L, 80);
        Evento evento = new Evento(
                "Evento inicial",
                null,
                OffsetDateTime.now().plusDays(5),
                OffsetDateTime.now().plusDays(6),
                "Salon",
                EstadoEvento.PLANIFICADO,
                anterior
        );
        ReflectionTestUtils.setField(evento, "id", 20L);

        GuardarEventoSolicitud solicitud = new GuardarEventoSolicitud(
                "Evento actualizado",
                null,
                OffsetDateTime.now().plusDays(7),
                OffsetDateTime.now().plusDays(8),
                "Salon 2",
                2L,
                5L,
                60,
                EstadoEvento.PUBLICADO,
                Set.of(9L)
        );

        when(eventoRepository.findById(20L)).thenReturn(Optional.of(evento));
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(nueva));
        when(espacioRepository.findById(5L)).thenReturn(Optional.of(espacio));

        EventoRespuesta respuesta = eventoService.actualizarEvento(20L, solicitud);

        verify(eventoRepository).existeConflictoProgramacionEditarConFin(
                5L,
                solicitud.fechaInicio(),
                solicitud.fechaFin(),
                20L
        );
        assertThat(evento.getCategoria()).isSameAs(nueva);
        assertThat(evento.getEspacio()).isSameAs(espacio);
        assertThat(evento.getAforoMaximo()).isEqualTo(60);
        assertThat(respuesta.categoria()).isNotNull();
        assertThat(respuesta.categoria().nombre()).isEqualTo("Tecnologia");
        assertThat(respuesta.espacio()).isNotNull();
        assertThat(respuesta.espacio().id()).isEqualTo(5L);
        assertThat(respuesta.estado()).isEqualTo("PUBLICADO");
    }

    @Test
    void actualizarEventoNoRequiereResponsablesNiModificaAsignaciones() {
        Categoria anterior = categoriaConId(1L, "General");
        Categoria nueva = categoriaConId(2L, "Tecnologia");
        Espacio espacio = espacioConId(5L, 80);
        Usuario responsable = responsableConRol(9L, "STAFF");
        Evento evento = new Evento(
                "Evento inicial",
                null,
                OffsetDateTime.now().plusDays(5),
                OffsetDateTime.now().plusDays(6),
                "Salon",
                EstadoEvento.PLANIFICADO,
                anterior
        );
        evento.asignarResponsables(Set.of(responsable));
        ReflectionTestUtils.setField(evento, "id", 20L);

        GuardarEventoSolicitud solicitud = new GuardarEventoSolicitud(
                "Evento actualizado",
                null,
                OffsetDateTime.now().plusDays(7),
                null,
                "Salon 2",
                2L,
                5L,
                60,
                EstadoEvento.PUBLICADO,
                null
        );

        when(eventoRepository.findById(20L)).thenReturn(Optional.of(evento));
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(nueva));
        when(espacioRepository.findById(5L)).thenReturn(Optional.of(espacio));

        EventoRespuesta respuesta = eventoService.actualizarEvento(20L, solicitud);

        verify(eventoRepository).existeConflictoProgramacionEditarSinFin(
                5L,
                solicitud.fechaInicio(),
                20L
        );
        assertThat(respuesta.responsables()).hasSize(1);
        assertThat(evento.getResponsables()).containsExactly(responsable);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void asignarResponsablesPermiteDejarEventoSinResponsables() {
        Categoria categoria = categoriaConId(1L, "General");
        Usuario responsable = responsableConRol(9L, "STAFF");
        Evento evento = new Evento(
                "Evento inicial",
                null,
                OffsetDateTime.now().plusDays(5),
                OffsetDateTime.now().plusDays(6),
                "Salon",
                EstadoEvento.PLANIFICADO,
                categoria
        );
        evento.asignarResponsables(Set.of(responsable));
        ReflectionTestUtils.setField(evento, "id", 20L);

        when(eventoRepository.findById(20L)).thenReturn(Optional.of(evento));

        EventoRespuesta respuesta = eventoService.asignarResponsables(
                20L,
                new AsignarResponsablesSolicitud(Set.of())
        );

        assertThat(respuesta.responsables()).isEmpty();
        assertThat(evento.getResponsables()).isEmpty();
    }

    private Categoria categoriaConId(Long id, String nombre) {
        Categoria categoria = new Categoria(nombre, null);
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
    }

    private Espacio espacioConId(Long id, Integer capacidad) {
        Espacio espacio = new Espacio("Auditorio " + id, "Campus", capacidad);
        ReflectionTestUtils.setField(espacio, "id", id);
        return espacio;
    }

    private Usuario responsableConRol(Long id, String rolNombre) {
        Usuario usuario = new Usuario(
                "Persona",
                "Responsable",
                "persona" + id + "@eventos.local",
                "hash",
                EstadoUsuario.ACTIVO
        );
        Rol rol = new Rol(rolNombre, null, EstadoRol.ACTIVO);
        usuario.asignarRoles(Set.of(rol));
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
