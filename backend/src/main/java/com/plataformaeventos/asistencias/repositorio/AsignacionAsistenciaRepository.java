package com.plataformaeventos.asistencias.repositorio;

import com.plataformaeventos.asignacionesstaff.modelo.AsignacionStaff;
import com.plataformaeventos.asignacionesstaff.modelo.EstadoAsignacionStaff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignacionAsistenciaRepository extends JpaRepository<AsignacionStaff, Long> {
    boolean existsByOrganizacionIdAndPostulacionUsuarioIdAndPostulacionConvocatoriaEventoIdAndEstado(
            Long idOrganizacion, Long idUsuario, Long idEvento, EstadoAsignacionStaff estado);
}
