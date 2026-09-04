package com.plataformaeventos.convocatorias;

import com.plataformaeventos.categorias.Categoria;
import com.plataformaeventos.convocatorias.dto.ConvocatoriaRespuesta;
import com.plataformaeventos.convocatorias.dto.CrearConvocatoriaSolicitud;
import com.plataformaeventos.eventos.EstadoEvento;
import com.plataformaeventos.eventos.Evento;
import com.plataformaeventos.eventos.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConvocatoriaServiceTest {

    private final ConvocatoriaRepository convocatoriaRepository = mock(ConvocatoriaRepository.class);
    private final EventoRepository eventoRepository = mock(EventoRepository.class);
    private final ConvocatoriaService convocatoriaService = new ConvocatoriaService(
            convocatoriaRepository,
            eventoRepository
    );

    @Test
    void crearAsociaEventoEIniciaAbierta() {
        Evento evento = crearEvento(8L, EstadoEvento.PUBLICADO, 10);
        CrearConvocatoriaSolicitud solicitud = new CrearConvocatoriaSolicitud(
                8L,
                TipoPersonal.VOLUNTARIO,
                " Apoyo en acreditacion y orientacion de asistentes ",
                12,
                LocalDate.now().plusDays(5)
        );

        when(eventoRepository.findById(8L)).thenReturn(Optional.of(evento));
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(invocacion -> {
            Convocatoria convocatoria = invocacion.getArgument(0);
            ReflectionTestUtils.setField(convocatoria, "id", 25L);
            return convocatoria;
        });

        ConvocatoriaRespuesta respuesta = convocatoriaService.crear(solicitud);

        assertThat(respuesta.id()).isEqualTo(25L);
        assertThat(respuesta.idEvento()).isEqualTo(8L);
        assertThat(respuesta.tipoPersonal()).isEqualTo(TipoPersonal.VOLUNTARIO);
        assertThat(respuesta.descripcion()).isEqualTo(
                "Apoyo en acreditacion y orientacion de asistentes"
        );
        assertThat(respuesta.cantidadRequerida()).isEqualTo(12);
        assertThat(respuesta.estado()).isEqualTo(EstadoConvocatoria.ABIERTA);
    }

    @Test
    void crearFallaCuandoEventoNoExiste() {
        CrearConvocatoriaSolicitud solicitud = new CrearConvocatoriaSolicitud(
                99L,
                TipoPersonal.STAFF,
                "Control de acceso",
                2,
                LocalDate.now().plusDays(1)
        );
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> convocatoriaService.crear(solicitud))
                .isInstanceOf(ExcepcionNoEncontrado.class)
                .hasMessageContaining("Evento no encontrado");
    }

    @Test
    void crearFallaSiFechaLimiteSuperaInicioDelEvento() {
        Evento evento = crearEvento(3L, EstadoEvento.PUBLICADO, 3);
        CrearConvocatoriaSolicitud solicitud = new CrearConvocatoriaSolicitud(
                3L,
                TipoPersonal.STAFF,
                "Soporte tecnico",
                3,
                LocalDate.now().plusDays(4)
        );
        when(eventoRepository.findById(3L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> convocatoriaService.crear(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior al inicio del evento");
    }

    @Test
    void cerrarCambiaEstadoYNoPermiteSegundaTransicion() {
        Convocatoria convocatoria = crearConvocatoria(4L);
        when(convocatoriaRepository.findById(4L)).thenReturn(Optional.of(convocatoria));
        when(convocatoriaRepository.save(convocatoria)).thenReturn(convocatoria);

        ConvocatoriaRespuesta respuesta = convocatoriaService.cerrar(4L);

        assertThat(respuesta.estado()).isEqualTo(EstadoConvocatoria.CERRADA);
        verify(convocatoriaRepository).save(convocatoria);
        assertThatThrownBy(() -> convocatoriaService.cancelar(4L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("convocatoria abierta");
    }

    @Test
    void cancelarCambiaEstado() {
        Convocatoria convocatoria = crearConvocatoria(7L);
        when(convocatoriaRepository.findById(7L)).thenReturn(Optional.of(convocatoria));
        when(convocatoriaRepository.save(convocatoria)).thenReturn(convocatoria);

        ConvocatoriaRespuesta respuesta = convocatoriaService.cancelar(7L);

        assertThat(respuesta.estado()).isEqualTo(EstadoConvocatoria.CANCELADA);
    }

    private Evento crearEvento(Long id, EstadoEvento estado, int diasHastaInicio) {
        Categoria categoria = new Categoria("General", null);
        ReflectionTestUtils.setField(categoria, "id", 1L);
        Evento evento = new Evento(
                "Congreso de prueba",
                "Evento para pruebas",
                OffsetDateTime.now().plusDays(diasHastaInicio),
                OffsetDateTime.now().plusDays(diasHastaInicio + 1),
                "Auditorio",
                estado,
                categoria
        );
        ReflectionTestUtils.setField(evento, "id", id);
        return evento;
    }

    private Convocatoria crearConvocatoria(Long id) {
        Evento evento = crearEvento(1L, EstadoEvento.PUBLICADO, 5);
        Convocatoria convocatoria = new Convocatoria(
                evento,
                TipoPersonal.STAFF,
                "Apoyo logistico",
                4,
                LocalDate.now().plusDays(2)
        );
        ReflectionTestUtils.setField(convocatoria, "id", id);
        return convocatoria;
    }
}
