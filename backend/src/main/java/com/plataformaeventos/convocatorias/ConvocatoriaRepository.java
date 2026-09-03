package com.plataformaeventos.convocatorias;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Long> {

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
