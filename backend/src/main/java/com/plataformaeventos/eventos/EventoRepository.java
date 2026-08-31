package com.plataformaeventos.eventos;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    @EntityGraph(attributePaths = {"responsables", "responsables.roles"})
    List<Evento> findByEstadoNotOrderByFechaInicioAsc(EstadoEvento estado);

    @EntityGraph(attributePaths = {"responsables", "responsables.roles"})
    List<Evento> findAllByOrderByFechaInicioAsc();

    @EntityGraph(attributePaths = {"responsables", "responsables.roles"})
    Optional<Evento> findById(Long id);
}
