package com.plataformaeventos.recursos;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.recursos.dto.ActualizarRecursoSolicitud;
import com.plataformaeventos.recursos.dto.CambiarEstadoRecursoSolicitud;
import com.plataformaeventos.recursos.dto.CrearRecursoSolicitud;
import com.plataformaeventos.recursos.dto.RecursoRespuesta;
import com.plataformaeventos.recursos.modelo.EstadoRecurso;
import com.plataformaeventos.recursos.modelo.Recurso;
import com.plataformaeventos.recursos.modelo.TipoRecurso;
import com.plataformaeventos.recursos.repositorio.RecursoRepository;
import com.plataformaeventos.recursos.servicio.RecursoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecursoServiceTest {

    private final RecursoRepository recursoRepository = mock(RecursoRepository.class);
    private final RecursoService recursoService = new RecursoService(recursoRepository);

    @Test
    void crearRecursoValidaCodigoUnicoYGuarda() {
        CrearRecursoSolicitud solicitud = new CrearRecursoSolicitud(
                "proy-001",
                "Proyector 4K",
                TipoRecurso.AUDIOVISUAL,
                5,
                "Auditorio",
                "Proyector laser"
        );

        when(recursoRepository.existsByCodigo("PROY-001")).thenReturn(false);
        when(recursoRepository.save(any(Recurso.class))).thenAnswer(invocation -> {
            Recurso r = invocation.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 10L);
            return r;
        });

        RecursoRespuesta respuesta = recursoService.crearRecurso(solicitud);

        assertThat(respuesta.id()).isEqualTo(10L);
        assertThat(respuesta.codigo()).isEqualTo("PROY-001");
        assertThat(respuesta.nombre()).isEqualTo("Proyector 4K");
        assertThat(respuesta.tipo()).isEqualTo(TipoRecurso.AUDIOVISUAL);
        assertThat(respuesta.cantidadTotal()).isEqualTo(5);
        assertThat(respuesta.cantidadDisponible()).isEqualTo(5);
        assertThat(respuesta.estado()).isEqualTo(EstadoRecurso.DISPONIBLE);
    }

    @Test
    void crearRecursoFallaSiCodigoYaExiste() {
        CrearRecursoSolicitud solicitud = new CrearRecursoSolicitud(
                "PROY-001",
                "Proyector",
                TipoRecurso.AUDIOVISUAL,
                2,
                "Sala 1",
                null
        );

        when(recursoRepository.existsByCodigo("PROY-001")).thenReturn(true);

        assertThatThrownBy(() -> recursoService.crearRecurso(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un recurso con el codigo PROY-001");
    }

    @Test
    void actualizarRecursoFallaSiCantidadDisponibleMayorQueTotal() {
        Recurso recursoExistente = new Recurso("MIC-01", "Microfono", TipoRecurso.AUDIOVISUAL, 3, 3, EstadoRecurso.DISPONIBLE, "Cabina", null);
        ReflectionTestUtils.setField(recursoExistente, "id", 1L);

        when(recursoRepository.findById(1L)).thenReturn(Optional.of(recursoExistente));
        when(recursoRepository.existsByCodigoAndIdNot("MIC-01", 1L)).thenReturn(false);

        ActualizarRecursoSolicitud solicitud = new ActualizarRecursoSolicitud(
                "MIC-01",
                "Microfono Inalambrico",
                TipoRecurso.AUDIOVISUAL,
                2,
                5, // disponible > total
                EstadoRecurso.DISPONIBLE,
                "Cabina",
                null
        );

        assertThatThrownBy(() -> recursoService.actualizarRecurso(1L, solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La cantidad disponible no puede ser mayor");
    }

    @Test
    void cambiarEstadoActualizaYPersiste() {
        Recurso recurso = new Recurso("LAP-01", "Laptop Dell", TipoRecurso.EQUIPO_COMPUTO, 1, 1, EstadoRecurso.DISPONIBLE, "Oficina", null);
        ReflectionTestUtils.setField(recurso, "id", 5L);

        when(recursoRepository.findById(5L)).thenReturn(Optional.of(recurso));
        when(recursoRepository.save(any(Recurso.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecursoRespuesta respuesta = recursoService.cambiarEstado(5L, new CambiarEstadoRecursoSolicitud(EstadoRecurso.MANTENIMIENTO));

        assertThat(respuesta.estado()).isEqualTo(EstadoRecurso.MANTENIMIENTO);
        verify(recursoRepository).save(recurso);
    }

    @Test
    void listarRecursosLlamaRepositorioConOrdenYFiltros() {
        when(recursoRepository.buscarConFiltros(eq("proy"), eq(TipoRecurso.AUDIOVISUAL), isNull(), any(Sort.class)))
                .thenReturn(List.of());

        List<RecursoRespuesta> resultado = recursoService.listarRecursos("proy", TipoRecurso.AUDIOVISUAL, null);

        assertThat(resultado).isEmpty();
        verify(recursoRepository).buscarConFiltros(eq("proy"), eq(TipoRecurso.AUDIOVISUAL), isNull(), any(Sort.class));
    }
}
