package com.plataformaeventos.recursos.asignaciones;

import com.plataformaeventos.categorias.Categoria;
import com.plataformaeventos.eventos.EstadoEvento;
import com.plataformaeventos.eventos.Evento;
import com.plataformaeventos.eventos.EventoRepository;
import com.plataformaeventos.recursos.EstadoRecurso;
import com.plataformaeventos.recursos.Recurso;
import com.plataformaeventos.recursos.RecursoRepository;
import com.plataformaeventos.recursos.TipoRecurso;
import com.plataformaeventos.recursos.asignaciones.dto.AsignacionRecursoRespuesta;
import com.plataformaeventos.recursos.asignaciones.dto.CrearAsignacionSolicitud;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsignacionRecursoServiceTest {

    private final AsignacionRecursoRepository asignacionRepository = mock(AsignacionRecursoRepository.class);
    private final EventoRepository eventoRepository = mock(EventoRepository.class);
    private final RecursoRepository recursoRepository = mock(RecursoRepository.class);

    private final AsignacionRecursoService service = new AsignacionRecursoService(
            asignacionRepository,
            eventoRepository,
            recursoRepository
    );

    @Test
    void crearAsignacionDescuentaStockDisponibleYGuarda() {
        Evento evento = crearEvento();

        Recurso recurso = new Recurso("PROY-01", "Proyector 4K", TipoRecurso.AUDIOVISUAL, 5, 5, EstadoRecurso.DISPONIBLE, "Depósito", null);
        ReflectionTestUtils.setField(recurso, "id", 10L);

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(recursoRepository.findById(10L)).thenReturn(Optional.of(recurso));
        when(asignacionRepository.save(any(AsignacionRecurso.class))).thenAnswer(inv -> {
            AsignacionRecurso a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 100L);
            return a;
        });

        CrearAsignacionSolicitud solicitud = new CrearAsignacionSolicitud(1L, 10L, "Sala A1", 3, "Para conferencias");
        AsignacionRecursoRespuesta respuesta = service.crearAsignacion(solicitud);

        assertThat(respuesta.id()).isEqualTo(100L);
        assertThat(respuesta.nombreEvento()).isEqualTo("Congreso Tech");
        assertThat(respuesta.codigoRecurso()).isEqualTo("PROY-01");
        assertThat(respuesta.cantidadAsignada()).isEqualTo(3);
        assertThat(recurso.getCantidadDisponible()).isEqualTo(2);
        assertThat(recurso.getEstado()).isEqualTo(EstadoRecurso.DISPONIBLE);
        verify(recursoRepository).save(recurso);
    }

    @Test
    void crearAsignacionCambiaEstadoAEnUsoSiDisponibilidadLlegaACero() {
        Evento evento = crearEvento();

        Recurso recurso = new Recurso("PROY-01", "Proyector 4K", TipoRecurso.AUDIOVISUAL, 2, 2, EstadoRecurso.DISPONIBLE, "Depósito", null);
        ReflectionTestUtils.setField(recurso, "id", 10L);

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(recursoRepository.findById(10L)).thenReturn(Optional.of(recurso));
        when(asignacionRepository.save(any(AsignacionRecurso.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearAsignacionSolicitud solicitud = new CrearAsignacionSolicitud(1L, 10L, "Auditorio", 2, null);
        service.crearAsignacion(solicitud);

        assertThat(recurso.getCantidadDisponible()).isEqualTo(0);
        assertThat(recurso.getEstado()).isEqualTo(EstadoRecurso.EN_USO);
    }

    @Test
    void crearAsignacionFallaSiCantidadMayorQueDisponible() {
        Evento evento = crearEvento();

        Recurso recurso = new Recurso("PROY-01", "Proyector 4K", TipoRecurso.AUDIOVISUAL, 5, 2, EstadoRecurso.DISPONIBLE, "Depósito", null);
        ReflectionTestUtils.setField(recurso, "id", 10L);

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(recursoRepository.findById(10L)).thenReturn(Optional.of(recurso));

        CrearAsignacionSolicitud solicitud = new CrearAsignacionSolicitud(1L, 10L, "Sala A1", 4, null);

        assertThatThrownBy(() -> service.crearAsignacion(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se puede asignar una cantidad superior a la disponibilidad existente");
    }

    @Test
    void liberarAsignacionRestauraStockDisponibleYActualizaEstado() {
        Evento evento = crearEvento();
        Recurso recurso = new Recurso("LAP-01", "Laptop", TipoRecurso.EQUIPO_COMPUTO, 2, 0, EstadoRecurso.EN_USO, "Oficina", null);
        AsignacionRecurso asignacion = new AsignacionRecurso(evento, recurso, "Mesa de Registro", 2, EstadoAsignacion.ACTIVA, null);
        ReflectionTestUtils.setField(asignacion, "id", 50L);

        when(asignacionRepository.findById(50L)).thenReturn(Optional.of(asignacion));
        when(asignacionRepository.save(any(AsignacionRecurso.class))).thenAnswer(inv -> inv.getArgument(0));

        AsignacionRecursoRespuesta respuesta = service.liberarAsignacion(50L);

        assertThat(respuesta.estado()).isEqualTo(EstadoAsignacion.DEVUELTA);
        assertThat(recurso.getCantidadDisponible()).isEqualTo(2);
        assertThat(recurso.getEstado()).isEqualTo(EstadoRecurso.DISPONIBLE);
        verify(recursoRepository).save(recurso);
    }

    private Evento crearEvento() {
        Categoria categoria = new Categoria("General", null);
        ReflectionTestUtils.setField(categoria, "id", 1L);
        Evento evento = new Evento(
                "Congreso Tech",
                "Desc",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(2),
                "Centro",
                EstadoEvento.PUBLICADO,
                categoria
        );
        ReflectionTestUtils.setField(evento, "id", 1L);
        return evento;
    }
}
