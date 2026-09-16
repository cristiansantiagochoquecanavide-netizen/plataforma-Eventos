package com.plataformaeventos.convocatorias.postulaciones;

import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.convocatorias.modelo.Convocatoria;
import com.plataformaeventos.convocatorias.modelo.EstadoConvocatoria;
import com.plataformaeventos.convocatorias.modelo.TipoPersonal;
import com.plataformaeventos.convocatorias.postulaciones.dto.PostulanteConvocatoriaRespuesta;
import com.plataformaeventos.convocatorias.repositorio.ConvocatoriaRepository;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestionPostulacionesServiceTest {

    private final PostulacionStaffRepository postulacionRepository = mock(PostulacionStaffRepository.class);
    private final AsignacionStaffRepository asignacionRepository = mock(AsignacionStaffRepository.class);
    private final ConvocatoriaRepository convocatoriaRepository = mock(ConvocatoriaRepository.class);
    private final EventoRepository eventoRepository = mock(EventoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final GestionPostulacionesService servicio = new GestionPostulacionesService(
            postulacionRepository,
            asignacionRepository,
            convocatoriaRepository,
            eventoRepository,
            usuarioRepository
    );

    private Convocatoria convocatoria;
    private PostulacionStaff postulacion;
    private Usuario gestor;

    @BeforeEach
    void preparar() {
        Evento evento = new Evento("Festival", "Evento de prueba", OffsetDateTime.now().plusDays(4),
                OffsetDateTime.now().plusDays(5), "Plaza", EstadoEvento.PUBLICADO, crearCategoria());
        ReflectionTestUtils.setField(evento, "id", 12L);
        convocatoria = new Convocatoria(evento, TipoPersonal.STAFF, "Control de acceso", 2,
                LocalDate.now().plusDays(1));
        ReflectionTestUtils.setField(convocatoria, "id", 8L);

        Usuario candidato = new Usuario("Ana", "Prueba", "ana@example.com", "hash", EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(candidato, "id", 20L);
        postulacion = new PostulacionStaff(candidato, convocatoria);
        ReflectionTestUtils.setField(postulacion, "id", 6L);

        gestor = mock(Usuario.class);
        when(gestor.getId()).thenReturn(30L);
        when(gestor.getEstado()).thenReturn(EstadoUsuario.ACTIVO);
        when(gestor.getRoles()).thenReturn(Set.of());
        when(usuarioRepository.findByCorreoConRoles("gestor@example.com")).thenReturn(Optional.of(gestor));
        when(eventoRepository.esResponsable(12L, 30L)).thenReturn(true);
        when(convocatoriaRepository.findById(8L)).thenReturn(Optional.of(convocatoria));
        when(convocatoriaRepository.buscarParaGestion(8L)).thenReturn(Optional.of(convocatoria));
        when(postulacionRepository.findById(6L)).thenReturn(Optional.of(postulacion));
        when(postulacionRepository.buscarParaGestion(6L)).thenReturn(Optional.of(postulacion));
        when(asignacionRepository.existsByPostulacionId(6L)).thenReturn(false);
        when(asignacionRepository.countByPostulacionConvocatoriaId(8L)).thenReturn(0L);
        when(postulacionRepository.saveAndFlush(any(PostulacionStaff.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(asignacionRepository.saveAndFlush(any(AsignacionStaff.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(postulacionRepository.findByConvocatoriaIdOrderByFechaPostulacionAscIdAsc(8L))
                .thenReturn(List.of(postulacion));
    }

    @Test
    void listarPostulantesDevuelveDatosDelCandidatoYLaConvocatoria() {
        List<PostulanteConvocatoriaRespuesta> resultado =
                servicio.listarPostulantes(8L, "gestor@example.com");

        assertThat(resultado).singleElement().satisfies(postulante -> {
            assertThat(postulante.nombres()).isEqualTo("Ana");
            assertThat(postulante.correo()).isEqualTo("ana@example.com");
            assertThat(postulante.nombreEvento()).isEqualTo("Festival");
            assertThat(postulante.tipoPersonal()).isEqualTo(TipoPersonal.STAFF);
            assertThat(postulante.estado()).isEqualTo(EstadoPostulacion.PENDIENTE);
        });
    }

    @Test
    void seleccionarCambiaEstadoYRegistraLaAsignacion() {
        PostulanteConvocatoriaRespuesta resultado = servicio.seleccionar(6L, "gestor@example.com");

        assertThat(resultado.estado()).isEqualTo(EstadoPostulacion.SELECCIONADA);
        verify(postulacionRepository).saveAndFlush(postulacion);
        verify(asignacionRepository).saveAndFlush(any(AsignacionStaff.class));
    }

    @Test
    void rechazarActualizaEstadoSinCrearAsignacion() {
        PostulanteConvocatoriaRespuesta resultado = servicio.rechazar(6L, "gestor@example.com");

        assertThat(resultado.estado()).isEqualTo(EstadoPostulacion.RECHAZADA);
        verify(postulacionRepository).saveAndFlush(postulacion);
        verify(asignacionRepository, never()).saveAndFlush(any(AsignacionStaff.class));
    }

    @Test
    void permiteRechazarPostulacionPendienteEnConvocatoriaCerrada() {
        convocatoria.cerrar();

        PostulanteConvocatoriaRespuesta resultado = servicio.rechazar(6L, "gestor@example.com");

        assertThat(resultado.estado()).isEqualTo(EstadoPostulacion.RECHAZADA);
        verify(postulacionRepository).saveAndFlush(postulacion);
        verify(asignacionRepository, never()).saveAndFlush(any(AsignacionStaff.class));
    }

    @Test
    void permiteRechazarPostulacionPendienteEnConvocatoriaCancelada() {
        convocatoria.cancelar();

        PostulanteConvocatoriaRespuesta resultado = servicio.rechazar(6L, "gestor@example.com");

        assertThat(resultado.estado()).isEqualTo(EstadoPostulacion.RECHAZADA);
        verify(postulacionRepository).saveAndFlush(postulacion);
        verify(asignacionRepository, never()).saveAndFlush(any(AsignacionStaff.class));
    }

    @Test
    void noPermiteSeleccionarLaMismaPostulacionDosVeces() {
        servicio.seleccionar(6L, "gestor@example.com");

        assertThatThrownBy(() -> servicio.seleccionar(6L, "gestor@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postulacion pendiente");
    }

    @Test
    void noPermiteSeleccionarEnConvocatoriaCerrada() {
        convocatoria.cerrar();

        assertThatThrownBy(() -> servicio.seleccionar(6L, "gestor@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("convocatorias abiertas");
        verify(asignacionRepository, never()).saveAndFlush(any(AsignacionStaff.class));
    }

    @Test
    void noPermiteSeleccionarSiLaConvocatoriaYaCompletoLasPlazas() {
        when(asignacionRepository.countByPostulacionConvocatoriaId(8L)).thenReturn(2L);

        assertThatThrownBy(() -> servicio.seleccionar(6L, "gestor@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cantidad requerida");
        verify(asignacionRepository, never()).saveAndFlush(any(AsignacionStaff.class));
    }

    @Test
    void noPermiteGestionarSiElUsuarioNoEsResponsableDelEvento() {
        when(eventoRepository.esResponsable(12L, 30L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.listarPostulantes(8L, "gestor@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void postulacionInexistenteNoSePuedeSeleccionar() {
        when(postulacionRepository.findById(6L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.seleccionar(6L, "gestor@example.com"))
                .isInstanceOf(ExcepcionNoEncontrado.class)
                .hasMessageContaining("Postulacion no encontrada");
    }

    private Categoria crearCategoria() {
        Categoria categoria = new Categoria("General", null);
        ReflectionTestUtils.setField(categoria, "id", 1L);
        return categoria;
    }
}
