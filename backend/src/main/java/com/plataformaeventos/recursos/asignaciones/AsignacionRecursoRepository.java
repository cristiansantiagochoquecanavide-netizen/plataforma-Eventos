package com.plataformaeventos.recursos.asignaciones;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AsignacionRecursoRepository extends JpaRepository<AsignacionRecurso, Long> {

    @Query("""
            SELECT a FROM AsignacionRecurso a
            JOIN FETCH a.evento e
            JOIN FETCH a.recurso r
            WHERE (:idEvento IS NULL OR e.id = :idEvento)
              AND (:idRecurso IS NULL OR r.id = :idRecurso)
              AND (:estado IS NULL OR a.estado = :estado)
            ORDER BY a.fechaAsignacion DESC
            """)
    List<AsignacionRecurso> buscarConFiltros(
            @Param("idEvento") Long idEvento,
            @Param("idRecurso") Long idRecurso,
            @Param("estado") EstadoAsignacion estado
    );
}
