package com.plataformaeventos.recursos;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    Optional<Recurso> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    @Query("""
            SELECT r FROM Recurso r
            WHERE (:termino IS NULL OR :termino = '' OR
                   LOWER(r.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR
                   LOWER(r.codigo) LIKE LOWER(CONCAT('%', :termino, '%')) OR
                   LOWER(r.ubicacion) LIKE LOWER(CONCAT('%', :termino, '%')))
              AND (:tipo IS NULL OR r.tipo = :tipo)
              AND (:estado IS NULL OR r.estado = :estado)
            """)
    List<Recurso> buscarConFiltros(
            @Param("termino") String termino,
            @Param("tipo") TipoRecurso tipo,
            @Param("estado") EstadoRecurso estado,
            Sort sort
    );
}
