package com.plataformaeventos.convocatorias.repositorio;

import com.plataformaeventos.convocatorias.modelo.Convocatoria;
import com.plataformaeventos.convocatorias.modelo.EstadoConvocatoria;
import com.plataformaeventos.convocatorias.modelo.TipoPersonal;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;

import java.util.List;

public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Long> {

    @Query("SELECT c FROM Convocatoria c JOIN FETCH c.evento e WHERE c.organizacion.id = :org AND (:idEvento IS NULL OR e.id = :idEvento) AND (:tipoPersonal IS NULL OR c.tipoPersonal = :tipoPersonal) AND (:estado IS NULL OR c.estado = :estado) AND (:termino IS NULL OR :termino = '' OR LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :termino, '%')) OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')))")
    List<Convocatoria> buscarConFiltrosTenant(@Param("idEvento") Long idEvento, @Param("tipoPersonal") TipoPersonal tipoPersonal, @Param("estado") EstadoConvocatoria estado, @Param("termino") String termino, @Param("org") Long org, Sort sort);

    @Query("select c from Convocatoria c join fetch c.evento e where c.id = :id and c.organizacion.id = :org")
    java.util.Optional<Convocatoria> findByIdAndOrganizacionId(@Param("id") Long id, @Param("org") Long org);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Convocatoria c where c.id = :id and c.organizacion.id = :org")
    java.util.Optional<Convocatoria> findByIdAndOrganizacionIdForUpdate(@Param("id") Long id, @Param("org") Long org);

    @Query("""
            SELECT c FROM Convocatoria c
            JOIN FETCH c.evento e
            WHERE (:idEvento IS NULL OR e.id = :idEvento)
              AND (:tipoPersonal IS NULL OR c.tipoPersonal = :tipoPersonal)
              AND (:estado IS NULL OR c.estado = :estado)
              AND (:termino IS NULL OR :termino = '' OR
                   LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :termino, '%')) OR
                   LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')))
            """)
    List<Convocatoria> buscarConFiltros(
            @Param("idEvento") Long idEvento,
            @Param("tipoPersonal") TipoPersonal tipoPersonal,
            @Param("estado") EstadoConvocatoria estado,
            @Param("termino") String termino,
            Sort sort
    );
}
