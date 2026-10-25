package com.plataformaeventos.asistencias;

import com.plataformaeventos.asignacionesstaff.modelo.EstadoAsignacionStaff;
import com.plataformaeventos.asistencias.modelo.Asistencia;
import com.plataformaeventos.asistencias.dto.InscripcionElegibleAsistenciaRespuesta;
import com.plataformaeventos.asistencias.repositorio.AsignacionAsistenciaRepository;
import com.plataformaeventos.asistencias.repositorio.AsistenciaRepository;
import com.plataformaeventos.asistencias.servicio.AsistenciaDuplicadaException;
import com.plataformaeventos.asistencias.servicio.AsistenciaService;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsistenciaServiceTest {
    @Mock AsistenciaRepository asistencias;
    @Mock AsignacionAsistenciaRepository asignaciones;
    @Mock InscripcionRepository inscripciones;
    @Mock UsuarioRepository usuarios;

    private AsistenciaService servicio;
    private Organizacion organizacion;
    private Usuario staff;
    private Usuario participante;
    private Evento evento;
    private Inscripcion inscripcion;

    @BeforeEach
    void preparar() {
        servicio = new AsistenciaService(asistencias, asignaciones, inscripciones, usuarios);
        organizacion = mock(Organizacion.class);
        when(organizacion.getId()).thenReturn(4L);
        staff = usuario(2L, "staff@ejemplo.test", "Ana", "Staff");
        participante = usuario(3L, "persona@ejemplo.test", "Luis", "Perez");
        evento = mock(Evento.class);
        when(evento.getId()).thenReturn(8L);
        when(evento.getNombre()).thenReturn("Conferencia");
        when(evento.getEstado()).thenReturn(EstadoEvento.EN_CURSO);
        inscripcion = mock(Inscripcion.class);
        when(inscripcion.getId()).thenReturn(10L);
        when(inscripcion.getEstado()).thenReturn(EstadoInscripcion.CONFIRMADA);
        when(inscripcion.getEvento()).thenReturn(evento);
        when(inscripcion.getParticipante()).thenReturn(participante);
        when(usuarios.findByCorreo("staff@ejemplo.test")).thenReturn(Optional.of(staff));
        when(inscripciones.bloquearPorIdEOrganizacion(10L, 4L)).thenReturn(Optional.of(inscripcion));
        when(asignaciones.existsByOrganizacionIdAndPostulacionUsuarioIdAndPostulacionConvocatoriaEventoIdAndEstado(
                4L, 2L, 8L, EstadoAsignacionStaff.ASIGNADA)).thenReturn(true);
    }

    @Test
    void registraAsistenciaConInformacionDelParticipanteEventoYStaff() {
        when(asistencias.save(any(Asistencia.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = servicio.registrar(10L, "STAFF@EJEMPLO.TEST", organizacion);

        assertThat(respuesta.idInscripcion()).isEqualTo(10L);
        assertThat(respuesta.nombreEvento()).isEqualTo("Conferencia");
        assertThat(respuesta.participante()).isEqualTo("Luis Perez");
        assertThat(respuesta.nombreRegistradoPor()).isEqualTo("Ana Staff");
        verify(asistencias).flush();
    }

    @Test
    void devuelve404CuandoLaInscripcionNoExisteEnLaOrganizacion() {
        when(inscripciones.bloquearPorIdEOrganizacion(10L, 4L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.registrar(10L, staff.getCorreo(), organizacion))
                .isInstanceOf(ExcepcionNoEncontrado.class);
    }

    @Test
    void noPermiteRegistrarSiStaffNoEstaAsignadoAlEvento() {
        when(asignaciones.existsByOrganizacionIdAndPostulacionUsuarioIdAndPostulacionConvocatoriaEventoIdAndEstado(
                4L, 2L, 8L, EstadoAsignacionStaff.ASIGNADA)).thenReturn(false);
        assertThatThrownBy(() -> servicio.registrar(10L, staff.getCorreo(), organizacion))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rechazaInscripcionCanceladaOEnEspera() {
        when(inscripcion.getEstado()).thenReturn(EstadoInscripcion.CANCELADA);
        assertThatThrownBy(() -> servicio.registrar(10L, staff.getCorreo(), organizacion))
                .isInstanceOf(IllegalStateException.class);
        verify(asistencias, never()).save(any());
    }

    @Test
    void rechazaEventoCanceladoOFinalizado() {
        when(evento.getEstado()).thenReturn(EstadoEvento.CANCELADO);
        assertThatThrownBy(() -> servicio.registrar(10L, staff.getCorreo(), organizacion))
                .isInstanceOf(IllegalStateException.class);
        verify(asistencias, never()).save(any());
    }

    @Test
    void rechazaDuplicidadCon409() {
        when(asistencias.existsByInscripcionId(10L)).thenReturn(true);
        assertThatThrownBy(() -> servicio.registrar(10L, staff.getCorreo(), organizacion))
                .isInstanceOf(AsistenciaDuplicadaException.class);
    }

    @Test
    void rechazaIdentificadorDeInscripcionNulo() {
        assertThatThrownBy(() -> servicio.registrar(null, staff.getCorreo(), organizacion))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(usuarios);
    }

    @Test
    void rechazaUsuarioStaffInexistente() {
        when(usuarios.findByCorreo("ausente@ejemplo.test")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.registrar(10L, "ausente@ejemplo.test", organizacion))
                .isInstanceOf(ExcepcionNoEncontrado.class);
    }

    @Test
    void listaInscripcionesElegiblesParaElStaffAutenticadoYOrganizacion() {
        var elegible = new InscripcionElegibleAsistenciaRespuesta(10L, "Luis", "Perez",
                "persona@ejemplo.test", 8L, "Conferencia", EstadoInscripcion.CONFIRMADA, false, null);
        when(inscripciones.listarElegiblesParaAsistencia(4L, 2L)).thenReturn(java.util.List.of(elegible));

        var resultado = servicio.listarInscripcionesElegibles(" STAFF@EJEMPLO.TEST ", organizacion);

        assertThat(resultado).containsExactly(elegible);
        verify(inscripciones).listarElegiblesParaAsistencia(4L, 2L);
    }

    private Usuario usuario(Long id, String correo, String nombres, String apellidos) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(id);
        when(usuario.getCorreo()).thenReturn(correo);
        when(usuario.getNombres()).thenReturn(nombres);
        when(usuario.getApellidos()).thenReturn(apellidos);
        return usuario;
    }
}
