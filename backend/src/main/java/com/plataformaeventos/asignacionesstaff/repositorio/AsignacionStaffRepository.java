package com.plataformaeventos.asignacionesstaff.repositorio;

import com.plataformaeventos.asignacionesstaff.modelo.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.*;

public interface AsignacionStaffRepository extends JpaRepository<AsignacionStaff, Long> {
    List<AsignacionStaff> findByOrganizacionIdOrderByTurnoInicioAsc(Long org);
    List<AsignacionStaff> findByOrganizacionIdAndPostulacionUsuarioIdOrderByTurnoInicioAsc(Long org, Long usuario);
    Optional<AsignacionStaff> findByIdAndOrganizacionId(Long id, Long org);
    boolean existsByPostulacionIdAndOrganizacionIdAndEstado(Long p, Long o, EstadoAsignacionStaff estado);
    @Query("select count(a) > 0 from AsignacionStaff a where a.organizacion.id = :org and a.postulacion.usuario.id = :usuario and a.estado = com.plataformaeventos.asignacionesstaff.modelo.EstadoAsignacionStaff.ASIGNADA and a.id <> coalesce(:id, -1) and (a.turnoInicio is null or a.turnoFin is null or :inicio is null or :fin is null or (a.turnoInicio < :fin and a.turnoFin > :inicio))")
    boolean existeConflicto(@Param("org") Long org, @Param("usuario") Long usuario, @Param("inicio") OffsetDateTime inicio, @Param("fin") OffsetDateTime fin, @Param("id") Long id);
}
