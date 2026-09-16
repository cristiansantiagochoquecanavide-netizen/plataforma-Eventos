package com.plataformaeventos.convocatorias.postulaciones;

import com.plataformaeventos.convocatorias.modelo.Convocatoria;
import com.plataformaeventos.convocatorias.repositorio.ConvocatoriaRepository;
import com.plataformaeventos.convocatorias.modelo.TipoPersonal;
import com.plataformaeventos.excepciones.ExcepcionNoAutorizado;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostulacionStaffServiceTest {

    private final PostulacionStaffRepository postulaciones = mock(PostulacionStaffRepository.class);
    private final ConvocatoriaRepository convocatorias = mock(ConvocatoriaRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PostulacionStaffService servicio = new PostulacionStaffService(postulaciones, convocatorias, usuarios);
    private final Usuario usuario = new Usuario("Ana", "Perez", "ana@example.com", "hash", EstadoUsuario.ACTIVO);
    private final Convocatoria convocatoria = new Convocatoria(null, TipoPersonal.STAFF, "Apoyo", 2, LocalDate.now());

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(usuario, "id", 1L);
        ReflectionTestUtils.setField(convocatoria, "id", 2L);
        when(usuarios.findByCorreo(usuario.getCorreo())).thenReturn(Optional.of(usuario));
        when(convocatorias.findById(2L)).thenReturn(Optional.of(convocatoria));
        when(convocatorias.buscarParaPostulacion(2L)).thenReturn(Optional.of(convocatoria));
        when(postulaciones.saveAndFlush(any(PostulacionStaff.class))).thenAnswer(invocacion -> {
            PostulacionStaff postulacion = invocacion.getArgument(0);
            if (postulacion.getId() == null) {
                ReflectionTestUtils.setField(postulacion, "id", 3L);
                postulacion.prePersist();
            } else {
                postulacion.preUpdate();
            }
            return postulacion;
        });
    }

    @Test
    void postularseCorrectamenteInclusoEnFechaLimite() {
        var respuesta = servicio.postularse(2L, usuario.getCorreo());
        assertThat(respuesta.id()).isEqualTo(3L);
        assertThat(respuesta.idUsuario()).isEqualTo(1L);
        assertThat(respuesta.idConvocatoria()).isEqualTo(2L);
        assertThat(respuesta.estado()).isEqualTo(EstadoPostulacion.PENDIENTE);
        assertThat(respuesta.fechaPostulacion()).isNotNull();
        assertThat(respuesta.fechaActualizacion()).isEqualTo(respuesta.fechaPostulacion());
    }

    @Test
    void rechazarUsuarioYaPostulado() {
        when(postulaciones.existsByConvocatoriaIdAndUsuarioId(2L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> servicio.postularse(2L, usuario.getCorreo()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ya se ha postulado");
        verify(postulaciones, never()).saveAndFlush(any());
    }

    @Test
    void traducirDuplicadoConcurrenteAErrorDeNegocio() {
        var causa = new ConstraintViolationException("duplicado", new SQLException("duplicado", "23505"),
                "uk_postulacion_convocatoria_usuario");
        when(postulaciones.saveAndFlush(any(PostulacionStaff.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado", causa));
        assertThatThrownBy(() -> servicio.postularse(2L, usuario.getCorreo()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ya se ha postulado");
    }

    @Test
    void noOcultarOtrosErroresDeIntegridad() {
        when(postulaciones.saveAndFlush(any(PostulacionStaff.class)))
                .thenThrow(new DataIntegrityViolationException("otra restriccion"));
        assertThatThrownBy(() -> servicio.postularse(2L, usuario.getCorreo()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazarConvocatoriaCerrada() {
        convocatoria.cerrar();
        assertThatThrownBy(() -> servicio.postularse(2L, usuario.getCorreo()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("abierta");
        verify(postulaciones, never()).saveAndFlush(any());
    }

    @Test
    void rechazarConvocatoriaInexistente() {
        assertThatThrownBy(() -> servicio.postularse(99L, usuario.getCorreo()))
                .isInstanceOf(ExcepcionNoEncontrado.class).hasMessageContaining("Convocatoria no encontrada");
        verify(postulaciones, never()).saveAndFlush(any());
    }

    @Test
    void rechazarFechaLimiteVencida() {
        ReflectionTestUtils.setField(convocatoria, "fechaLimite", LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> servicio.postularse(2L, usuario.getCorreo()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("vencido");
        verify(postulaciones, never()).saveAndFlush(any());
    }

    @Test
    void rechazarUsuarioInactivo() {
        usuario.actualizarEstado(EstadoUsuario.INACTIVO);
        assertThatThrownBy(() -> servicio.postularse(2L, usuario.getCorreo()))
                .isInstanceOf(AccessDeniedException.class);
        verify(postulaciones, never()).saveAndFlush(any());
    }

    @Test
    void rechazarUsuarioSinAutenticar() {
        assertThatThrownBy(() -> servicio.postularse(2L, null)).isInstanceOf(ExcepcionNoAutorizado.class);
    }

    @Test
    void consultarSinPostulacionDevuelveVacio() {
        assertThat(servicio.consultar(2L, usuario.getCorreo())).isEmpty();
    }

    @Test
    void consultarYListarSoloPostulacionesPropias() {
        PostulacionStaff postulacion = new PostulacionStaff(usuario, convocatoria);
        when(postulaciones.findByConvocatoriaIdAndUsuarioId(2L, 1L)).thenReturn(Optional.of(postulacion));
        when(postulaciones.findByUsuarioIdOrderByFechaPostulacionDescIdDesc(1L)).thenReturn(List.of(postulacion));
        assertThat(servicio.consultar(2L, usuario.getCorreo())).get()
                .extracting("idUsuario").isEqualTo(1L);
        assertThat(servicio.listarMias(usuario.getCorreo())).hasSize(1);
        verify(postulaciones).findByUsuarioIdOrderByFechaPostulacionDescIdDesc(1L);
    }

    @Test
    void retirarConservaHistorialYNoPermiteSegundoRetiro() {
        PostulacionStaff postulacion = new PostulacionStaff(usuario, convocatoria);
        postulacion.prePersist();
        ReflectionTestUtils.setField(postulacion, "id", 3L);
        var fechaOriginal = postulacion.getFechaPostulacion();
        when(postulaciones.findByIdAndUsuarioId(3L, 1L)).thenReturn(Optional.of(postulacion));
        var respuesta = servicio.retirar(3L, usuario.getCorreo());
        assertThat(respuesta.estado()).isEqualTo(EstadoPostulacion.RETIRADA);
        assertThat(respuesta.fechaPostulacion()).isEqualTo(fechaOriginal);
        assertThat(respuesta.fechaActualizacion()).isAfterOrEqualTo(fechaOriginal);
        assertThatThrownBy(() -> servicio.retirar(3L, usuario.getCorreo()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noPermitirRetirarPostulacionAjena() {
        assertThatThrownBy(() -> servicio.retirar(9L, usuario.getCorreo()))
                .isInstanceOf(ExcepcionNoEncontrado.class);
        verify(postulaciones, never()).saveAndFlush(any());
    }
}
