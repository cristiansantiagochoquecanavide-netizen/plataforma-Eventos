package com.plataformaeventos.eventos;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    @Query("""
            select count(e) > 0 from Evento e
            where e.espacio.id = :idEspacio
              and e.estado <> com.plataformaeventos.eventos.EstadoEvento.CANCELADO
              and (:fechaFin is null or e.fechaInicio < :fechaFin)
              and (e.fechaFin is null or e.fechaFin > :fechaInicio)
              and (:idEvento is null or e.id <> :idEvento)
            """)
    boolean existeConflictoProgramacion(
            @Param("idEspacio") Long idEspacio,
            @Param("fechaInicio") OffsetDateTime fechaInicio,
            @Param("fechaFin") OffsetDateTime fechaFin,
            @Param("idEvento") Long idEvento
    );

    @EntityGraph(attributePaths = {"categoria", "espacio", "responsables", "responsables.roles"})
    List<Evento> findByEstadoNotOrderByFechaInicioAsc(EstadoEvento estado);

    @EntityGraph(attributePaths = {"categoria", "espacio", "responsables", "responsables.roles"})
    List<Evento> findAllByOrderByFechaInicioAsc();

    @EntityGraph(attributePaths = {"categoria", "espacio", "responsables", "responsables.roles"})
    Optional<Evento> findById(Long id);

    boolean existsByCategoriaId(Long idCategoria);
}
