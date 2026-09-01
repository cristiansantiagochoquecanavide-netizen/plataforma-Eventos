package com.plataformaeventos.eventos;

import com.plataformaeventos.categorias.Categoria;
import com.plataformaeventos.categorias.CategoriaRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.roles.EstadoRol;
import com.plataformaeventos.roles.Rol;
import com.plataformaeventos.usuarios.EstadoUsuario;
import com.plataformaeventos.usuarios.Usuario;
import com.plataformaeventos.usuarios.UsuarioRepository;
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
import static org.mockito.Mockito.when;

class EventoServiceTest {

    private final EventoRepository eventoRepository = mock(EventoRepository.class);
    private final CategoriaRepository categoriaRepository = mock(CategoriaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final EventoService eventoService = new EventoService(
            eventoRepository,
            categoriaRepository,
            usuarioRepository
    );

    @Test
    void crearEventoAsociaCategoriaYDevuelveCategoriaEnRespuesta() {
        Categoria categoria = categoriaConId(4L, "Academico");
        Usuario responsable = responsableConRol(8L, "ORGANIZADOR");
        GuardarEventoSolicitud solicitud = new GuardarEventoSolicitud(
                " Congreso de IA ",
                " Programa academico ",
                OffsetDateTime.now().plusDays(10),
                OffsetDateTime.now().plusDays(11),
                " Auditorio Central ",
                4L,
                EstadoEvento.PUBLICADO,
                Set.of(8L)
        );
        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);

        when(categoriaRepository.findById(4L)).thenReturn(Optional.of(categoria));
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(responsable));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocacion -> {
            Evento evento = invocacion.getArgument(0);
            ReflectionTestUtils.setField(evento, "id", 33L);
            return evento;
        });

        EventoRespuesta respuesta = eventoService.crearEvento(solicitud);

        verify(eventoRepository).save(captor.capture());
        assertThat(captor.getValue().getCategoria()).isSameAs(categoria);
        assertThat(respuesta.id()).isEqualTo(33L);
        assertThat(respuesta.nombre()).isEqualTo("Congreso de IA");
        assertThat(respuesta.lugarPrincipal()).isEqualTo("Auditorio Central");
        assertThat(respuesta.categoria()).isNotNull();
        assertThat(respuesta.categoria().id()).isEqualTo(4L);
        assertThat(respuesta.categoria().nombre()).isEqualTo("Academico");
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
        ReflectionTestUtils.setField(evento, "id", 20L);

        GuardarEventoSolicitud solicitud = new GuardarEventoSolicitud(
                "Evento actualizado",
                null,
                OffsetDateTime.now().plusDays(7),
                OffsetDateTime.now().plusDays(8),
                "Salon 2",
                2L,
                EstadoEvento.PUBLICADO,
                Set.of(9L)
        );

        when(eventoRepository.findById(20L)).thenReturn(Optional.of(evento));
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(nueva));
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(responsable));

        EventoRespuesta respuesta = eventoService.actualizarEvento(20L, solicitud);

        assertThat(evento.getCategoria()).isSameAs(nueva);
        assertThat(respuesta.categoria()).isNotNull();
        assertThat(respuesta.categoria().nombre()).isEqualTo("Tecnologia");
        assertThat(respuesta.estado()).isEqualTo("PUBLICADO");
    }

    private Categoria categoriaConId(Long id, String nombre) {
        Categoria categoria = new Categoria(nombre, null);
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
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
