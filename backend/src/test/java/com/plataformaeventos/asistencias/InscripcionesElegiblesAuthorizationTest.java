package com.plataformaeventos.asistencias;

import com.plataformaeventos.asistencias.controlador.AsistenciaController;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class InscripcionesElegiblesAuthorizationTest {

    @Test
    void listadoDeAsistenciaUsaPermisoEspecificoDeCu23() throws NoSuchMethodException {
        Method metodo = Arrays.stream(AsistenciaController.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(GetMapping.class))
                .filter(m -> Arrays.asList(m.getAnnotation(GetMapping.class).value())
                        .contains("/inscripciones-elegibles"))
                .findFirst().orElseThrow();

        assertThat(metodo.getAnnotation(PreAuthorize.class).value())
                .contains("'ASISTENCIA_REGISTRAR'")
                .doesNotContain("PARTICIPANTE_VER");
    }

    @Test
    void consultaLimitaInscripcionesAStaffAsignadoYOrganizacionActiva() throws NoSuchMethodException {
        Method metodo = InscripcionRepository.class.getDeclaredMethod(
                "listarElegiblesParaAsistencia", Long.class, Long.class);
        String consulta = metodo.getAnnotation(Query.class).value();

        assertThat(consulta).contains(
                "i.organizacion.id = :organizacionId",
                "e.organizacion.id = :organizacionId",
                "asignacion.organizacion.id = :organizacionId",
                "asignacion.usuario.id = :staffId",
                "EstadoAsignacionStaff.ASIGNADA",
                "EstadoInscripcion.CONFIRMADA",
                "EstadoEvento.PUBLICADO",
                "EstadoEvento.EN_CURSO",
                "left join Asistencia a on a.inscripcion = i");
    }
}
