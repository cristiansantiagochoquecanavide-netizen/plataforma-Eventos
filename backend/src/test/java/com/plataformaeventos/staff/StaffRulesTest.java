package com.plataformaeventos.staff;

import com.plataformaeventos.asignacionesstaff.dto.CrearAsignacionStaffSolicitud;
import com.plataformaeventos.asignacionesstaff.modelo.AsignacionStaff;
import com.plataformaeventos.asignacionesstaff.repositorio.AsignacionStaffRepository;
import com.plataformaeventos.asignacionesstaff.servicio.AsignacionStaffService;
import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.convocatorias.modelo.Convocatoria;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.postulacionesstaff.modelo.EstadoPostulacionStaff;
import com.plataformaeventos.postulacionesstaff.modelo.PostulacionStaff;
import com.plataformaeventos.postulacionesstaff.repositorio.PostulacionStaffRepository;
import com.plataformaeventos.usuarios.modelo.Usuario;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StaffRulesTest {
    @Test
    void postulacionResueltaNoPuedeCambiarDeEstado() {
        Usuario usuario = new Usuario("Ana", "Perez", "ana@test.local", "hash", com.plataformaeventos.usuarios.modelo.EstadoUsuario.ACTIVO);
        PostulacionStaff real = new PostulacionStaff(mock(Convocatoria.class), usuario, mock(Organizacion.class), "motivo");
        real.aceptar();
        assertThatThrownBy(real::rechazar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void asignacionRechazaConflictoDeTurno() {
        AsignacionStaffRepository asignaciones = mock(AsignacionStaffRepository.class);
        PostulacionStaffRepository postulaciones = mock(PostulacionStaffRepository.class);
        AsignacionStaffService service = new AsignacionStaffService(asignaciones, postulaciones, mock(BitacoraService.class));
        PostulacionStaff postulacion = mock(PostulacionStaff.class);
        Usuario usuario = new Usuario("Ana", "Perez", "ana@test.local", "hash", com.plataformaeventos.usuarios.modelo.EstadoUsuario.ACTIVO);
        when(postulaciones.findByIdAndOrganizacionId(4L, 2L)).thenReturn(Optional.of(postulacion));
        when(postulacion.getEstado()).thenReturn(EstadoPostulacionStaff.ACEPTADA);
        when(postulacion.getId()).thenReturn(4L);
        when(postulacion.getUsuario()).thenReturn(usuario);
        when(asignaciones.existeConflicto(eq(2L), any(), any(), any(), isNull())).thenReturn(true);
        Organizacion organizacion = mock(Organizacion.class);
        when(organizacion.getId()).thenReturn(2L);
        CrearAsignacionStaffSolicitud solicitud = new CrearAsignacionStaffSolicitud(4L, "Apoyo", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), null);

        assertThatThrownBy(() -> service.crear(solicitud, organizacion, usuario))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("solapado");
        verify(asignaciones, never()).save(any(AsignacionStaff.class));
    }

    @Test
    void asignacionCanceladaNoPuedeCancelarseDosVeces() {
        Usuario usuario = new Usuario("Ana", "Perez", "ana@test.local", "hash", com.plataformaeventos.usuarios.modelo.EstadoUsuario.ACTIVO);
        PostulacionStaff postulacion = new PostulacionStaff(mock(Convocatoria.class), usuario, mock(Organizacion.class), "motivo");
        AsignacionStaff asignacion = new AsignacionStaff(postulacion, mock(Organizacion.class), "Apoyo", null, null, null);
        asignacion.cancelar();
        assertThatThrownBy(asignacion::cancelar).isInstanceOf(IllegalStateException.class);
    }
}
