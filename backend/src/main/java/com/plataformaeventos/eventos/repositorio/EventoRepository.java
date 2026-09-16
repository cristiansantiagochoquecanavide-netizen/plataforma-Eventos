package com.plataformaeventos.eventos.repositorio;

import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
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
              and e.estado <> com.plataformaeventos.eventos.modelo.EstadoEvento.CANCELADO
              and (e.fechaFin is null or e.fechaFin > :fechaInicio)
            """)
    boolean existeConflictoProgramacionCrearSinFin(
            @Param("idEspacio") Long idEspacio,
            @Param("fechaInicio") OffsetDateTime fechaInicio
    );

    @Query("""
            select count(e) > 0 from Evento e
            where e.espacio.id = :idEspacio
              and e.estado <> com.plataformaeventos.eventos.modelo.EstadoEvento.CANCELADO
              and e.fechaInicio < :fechaFin
              and (e.fechaFin is null or e.fechaFin > :fechaInicio)
            """)
    boolean existeConflictoProgramacionCrearConFin(
            @Param("idEspacio") Long idEspacio,
            @Param("fechaInicio") OffsetDateTime fechaInicio,
            @Param("fechaFin") OffsetDateTime fechaFin
    );

    @Query("""
            select count(e) > 0 from Evento e
            where e.espacio.id = :idEspacio
              and e.estado <> com.plataformaeventos.eventos.modelo.EstadoEvento.CANCELADO
              and (e.fechaFin is null or e.fechaFin > :fechaInicio)
              and e.id <> :idEvento
            """)
    boolean existeConflictoProgramacionEditarSinFin(
            @Param("idEspacio") Long idEspacio,
            @Param("fechaInicio") OffsetDateTime fechaInicio,
            @Param("idEvento") Long idEvento
    );

    @Query("""
            select count(e) > 0 from Evento e
            where e.espacio.id = :idEspacio
              and e.estado <> com.plataformaeventos.eventos.modelo.EstadoEvento.CANCELADO
              and e.fechaInicio < :fechaFin
              and (e.fechaFin is null or e.fechaFin > :fechaInicio)
              and e.id <> :idEvento
            """)
    boolean existeConflictoProgramacionEditarConFin(
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

    @Query("SELECT COUNT(e) > 0 FROM Evento e JOIN e.responsables responsable " +
            "WHERE e.id = :idEvento AND responsable.id = :idResponsable")
    boolean esResponsable(@Param("idEvento") Long idEvento, @Param("idResponsable") Long idResponsable);
}
