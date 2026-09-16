package com.plataformaeventos.convocatorias.repositorio;

import com.plataformaeventos.convocatorias.modelo.Convocatoria;
import com.plataformaeventos.convocatorias.modelo.EstadoConvocatoria;
import com.plataformaeventos.convocatorias.modelo.TipoPersonal;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Long> {

    // Impide que un cierre concurrente se confirme entre la validacion y el alta.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT c FROM Convocatoria c WHERE c.id = :id")
    Optional<Convocatoria> buscarParaPostulacion(@Param("id") Long id);

    // Serializa la gestion con el cierre y protege el limite de cupos al seleccionar.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Convocatoria c WHERE c.id = :id")
    Optional<Convocatoria> buscarParaGestion(@Param("id") Long id);

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
