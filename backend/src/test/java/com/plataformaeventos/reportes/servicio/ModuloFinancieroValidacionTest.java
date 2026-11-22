package com.plataformaeventos.reportes.servicio;

import com.plataformaeventos.asistencias.repositorio.AsistenciaRepository;
import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.espacios.modelo.Espacio;
import com.plataformaeventos.espacios.repositorio.EspacioRepository;
import com.plataformaeventos.eventos.dto.GuardarEventoSolicitud;
import com.plataformaeventos.eventos.dto.TipoEntradaDTO;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.modelo.TipoEntrada;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.eventos.repositorio.TipoEntradaRepository;
import com.plataformaeventos.eventos.servicio.EventoService;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.inscripciones.servicio.InscripcionService;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.recursos.modelo.AsignacionRecurso;
import com.plataformaeventos.recursos.modelo.EstadoAsignacion;
import com.plataformaeventos.recursos.modelo.EstadoRecurso;
import com.plataformaeventos.recursos.modelo.Recurso;
import com.plataformaeventos.recursos.modelo.TipoRecurso;
import com.plataformaeventos.recursos.repositorio.AsignacionRecursoRepository;
import com.plataformaeventos.reportes.dto.ResumenFinancieroDTO;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuloFinancieroValidacionTest {

    @Mock private EventoRepository eventoRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private AsignacionRecursoRepository asignacionRecursoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EspacioRepository espacioRepository;
    @Mock private TipoEntradaRepository tipoEntradaRepository;
    @Mock private AsistenciaRepository asistenciaRepository;

    @Test
    void balanceFinancieroExcluyeAsignacionesCanceladasYCalculaRoi() {
        BalanceFinancieroService service = new BalanceFinancieroService(
                eventoRepository, inscripcionRepository, asignacionRecursoRepository
        );

        Organizacion org = mock(Organizacion.class);
        when(org.getId()).thenReturn(1L);

        Espacio espacio = new Espacio("Auditorio", "Piso 1", 200);
        espacio.setCostoAlquiler(new BigDecimal("1000.00"));

        Evento evento = new Evento("Tech Summit", "Desc", OffsetDateTime.now().plusDays(2),
                OffsetDateTime.now().plusDays(3), "Centro", EstadoEvento.PUBLICADO, null);
        ReflectionTestUtils.setField(evento, "id", 10L);
        evento.configurarProgramacion(espacio, 150);

        Recurso sonido = new Recurso("SND-1", "Sonido", TipoRecurso.AUDIOVISUAL, 2, 1,
                EstadoRecurso.DISPONIBLE, "Deposito", null);
        sonido.setCostoUnitario(new BigDecimal("250.00"));

        AsignacionRecurso activa = new AsignacionRecurso(evento, sonido, "Sala A", 2, EstadoAsignacion.ACTIVA, null);

        AsignacionRecurso cancelada = new AsignacionRecurso(evento, sonido, "Sala B", 1, EstadoAsignacion.CANCELADA, null);

        when(eventoRepository.findByOrganizacionIdAndEstadoNotOrderByFechaInicioAsc(1L, EstadoEvento.CANCELADO))
                .thenReturn(List.of(evento));
        when(inscripcionRepository.sumMontoPagadoByEventoId(10L)).thenReturn(new BigDecimal("2250.00"));
        when(inscripcionRepository.countByEventoIdAndOrganizacionIdAndEstado(10L, 1L, EstadoInscripcion.CONFIRMADA))
                .thenReturn(15L);
        when(asignacionRecursoRepository.buscarConFiltrosTenant(10L, null, null, 1L))
                .thenReturn(List.of(activa, cancelada));

        ResumenFinancieroDTO resumen = service.calcularResumenFinanciero(org);

        // Costo esperado = 1000 (espacio) + 2*250 (activa) = 1500. La cancelada NO se suma.
        assertEquals(new BigDecimal("2250.00"), resumen.ingresosTotales());
        assertEquals(new BigDecimal("1500.00"), resumen.costosTotales());
        assertEquals(new BigDecimal("750.00"), resumen.balanceNetoGlobal());
        assertEquals(50.0, resumen.roiGlobal(), 0.01);
        assertEquals(1L, resumen.eventosConGanancia());
        assertEquals("SUPERAVIT", resumen.detalleEventos().get(0).situacion());
    }

    @Test
    void eventoServiceRechazaEntradasInvalidasODuplicadasOSuperandoAforo() {
        EventoService eventoService = new EventoService(
                eventoRepository, categoriaRepository, usuarioRepository, espacioRepository,
                null, tipoEntradaRepository, inscripcionRepository, null
        );

        Organizacion org = mock(Organizacion.class);
        when(org.getId()).thenReturn(1L);

        Categoria cat = mock(Categoria.class);
        Espacio esp = new Espacio("Salon", "Ubicacion", 100);
        ReflectionTestUtils.setField(esp, "id", 5L);

        when(categoriaRepository.findByIdAndOrganizacionId(1L, 1L)).thenReturn(Optional.of(cat));
        when(espacioRepository.findByIdAndOrganizacionId(5L, 1L)).thenReturn(Optional.of(esp));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(inv -> {
            Evento e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 99L);
            return e;
        });

        OffsetDateTime ini = OffsetDateTime.now().plusDays(5);
        OffsetDateTime fin = ini.plusHours(4);

        // 1. Precio negativo
        GuardarEventoSolicitud precioNegativo = new GuardarEventoSolicitud(
                "Evento A", null, ini, fin, null, 1L, 5L, 50, EstadoEvento.PUBLICADO, Set.of(),
                List.of(new TipoEntradaDTO(null, "General", new BigDecimal("-10.00"), 20, null))
        );
        assertThrows(IllegalArgumentException.class, () -> eventoService.crearEvento(precioNegativo, org));

        // 2. Categorias duplicadas (case-insensitive)
        GuardarEventoSolicitud duplicadas = new GuardarEventoSolicitud(
                "Evento B", null, ini, fin, null, 1L, 5L, 50, EstadoEvento.PUBLICADO, Set.of(),
                List.of(
                        new TipoEntradaDTO(null, "VIP", new BigDecimal("100.00"), 10, null),
                        new TipoEntradaDTO(null, "vip", new BigDecimal("150.00"), 10, null)
                )
        );
        assertThrows(IllegalArgumentException.class, () -> eventoService.crearEvento(duplicadas, org));

        // 3. Suma de cupos supera aforo maximo (40 + 20 = 60 > 50)
        GuardarEventoSolicitud superaAforo = new GuardarEventoSolicitud(
                "Evento C", null, ini, fin, null, 1L, 5L, 50, EstadoEvento.PUBLICADO, Set.of(),
                List.of(
                        new TipoEntradaDTO(null, "General", new BigDecimal("50.00"), 40, null),
                        new TipoEntradaDTO(null, "VIP", new BigDecimal("150.00"), 20, null)
                )
        );
        assertThrows(IllegalArgumentException.class, () -> eventoService.crearEvento(superaAforo, org));
    }

    @Test
    void inscripcionServiceRechazaCategoriaInactivaOAgotada() {
        InscripcionService inscripcionService = new InscripcionService(
                inscripcionRepository, eventoRepository, usuarioRepository, asistenciaRepository,
                null, tipoEntradaRepository
        );

        Organizacion org = mock(Organizacion.class);
        when(org.getId()).thenReturn(1L);

        Usuario usuario = mock(Usuario.class);
        when(usuarioRepository.findByCorreo("test@demo.com")).thenReturn(Optional.of(usuario));

        Evento evento = new Evento("Evento", "Desc", OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(2), "Lugar", EstadoEvento.PUBLICADO, null);
        ReflectionTestUtils.setField(evento, "id", 10L);
        evento.asignarOrganizacion(org);
        when(inscripcionRepository.bloquearEvento(10L, 1L)).thenReturn(Optional.of(evento));

        // Caso 1: Categoria inactiva
        TipoEntrada inactiva = new TipoEntrada(org, evento, "Antigua", new BigDecimal("50"), 10, null);
        ReflectionTestUtils.setField(inactiva, "id", 77L);
        inactiva.setActivo(false);
        when(tipoEntradaRepository.findByIdAndEventoId(77L, 10L)).thenReturn(Optional.of(inactiva));

        assertThrows(IllegalStateException.class,
                () -> inscripcionService.inscribir(10L, "test@demo.com", 77L, org));

        // Caso 2: Sin idTipoEntrada pero todas agotadas
        TipoEntrada agotada = new TipoEntrada(org, evento, "General", new BigDecimal("50"), 5, null);
        ReflectionTestUtils.setField(agotada, "id", 88L);
        when(tipoEntradaRepository.findByEventoIdAndActivoTrueOrderByPrecioAsc(10L))
                .thenReturn(List.of(agotada));
        when(inscripcionRepository.countByTipoEntradaIdAndEstado(88L, EstadoInscripcion.CONFIRMADA))
                .thenReturn(5L);

        assertThrows(IllegalStateException.class,
                () -> inscripcionService.inscribir(10L, "test@demo.com", null, org));
    }
}
