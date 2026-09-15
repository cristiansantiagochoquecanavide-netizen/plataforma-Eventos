package com.plataformaeventos.inscripciones.repositorio;

import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.evento e LEFT JOIN FETCH e.categoria LEFT JOIN FETCH e.espacio WHERE i.participante.id = :idParticipante ORDER BY i.fechaInscripcion DESC")
    List<Inscripcion> findByParticipanteIdConEvento(@Param("idParticipante") Long idParticipante);

    Optional<Inscripcion> findByEventoIdAndParticipanteId(Long idEvento, Long idParticipante);

    boolean existsByEventoIdAndParticipanteIdAndEstado(Long idEvento, Long idParticipante, EstadoInscripcion estado);

    long countByEventoIdAndEstado(Long idEvento, EstadoInscripcion estado);

    Optional<Inscripcion> findByIdAndParticipanteId(Long idInscripcion, Long idParticipante);
}
